package com.yueti.scanner.core

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.hypot
import kotlin.math.max

data class DocumentPoint(val x: Float, val y: Float)

enum class DocumentFilter { Original, ColorEnhanced, Grayscale, BlackWhite, WhitePaper }

/**
 * Android-native extraction of the MIT OSS Document Scanner processing chain.
 * The detector follows the pinned upstream DocumentDetector contour/quad pipeline;
 * the app shell, OCR and QR dependencies are intentionally not included.
 */
object DocumentScannerCore {
    val defaultCorners = listOf(
        DocumentPoint(.04f, .04f), DocumentPoint(.96f, .04f),
        DocumentPoint(.96f, .96f), DocumentPoint(.04f, .96f),
    )

    fun detect(bitmap: Bitmap): List<DocumentPoint> {
        val rgba = Mat()
        val gray = Mat()
        val edges = Mat()
        Utils.bitmapToMat(bitmap, rgba)
        Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
        Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 0.0)
        Imgproc.Canny(gray, edges, 60.0, 180.0)
        Imgproc.morphologyEx(edges, edges, Imgproc.MORPH_CLOSE, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(7.0, 7.0)))
        val contours = mutableListOf<MatOfPoint>()
        Imgproc.findContours(edges, contours, Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)
        val imageArea = bitmap.width.toDouble() * bitmap.height
        val quad = contours.asSequence().sortedByDescending(Imgproc::contourArea).mapNotNull { contour ->
            val curve = MatOfPoint2f(*contour.toArray())
            val approx = MatOfPoint2f()
            Imgproc.approxPolyDP(curve, approx, Imgproc.arcLength(curve, true) * .02, true)
            approx.toArray().takeIf { it.size == 4 && Imgproc.contourArea(MatOfPoint(*it)) > imageArea * .12 }
        }.firstOrNull()
        rgba.release(); gray.release(); edges.release(); contours.forEach(Mat::release)
        return quad?.let(::order)?.map { DocumentPoint((it.x / bitmap.width).toFloat().coerceIn(0f, 1f), (it.y / bitmap.height).toFloat().coerceIn(0f, 1f)) } ?: defaultCorners
    }

    fun process(bitmap: Bitmap, normalizedCorners: List<DocumentPoint>, filter: DocumentFilter): Bitmap {
        val points = normalizedCorners.takeIf { it.size == 4 } ?: defaultCorners
        val srcPoints = order(points.map { Point((it.x * bitmap.width).toDouble(), (it.y * bitmap.height).toDouble()) }.toTypedArray())
        val width = max(distance(srcPoints[0], srcPoints[1]), distance(srcPoints[3], srcPoints[2])).toInt().coerceAtLeast(64)
        val height = max(distance(srcPoints[0], srcPoints[3]), distance(srcPoints[1], srcPoints[2])).toInt().coerceAtLeast(64)
        val source = Mat()
        Utils.bitmapToMat(bitmap, source)
        val transform = Imgproc.getPerspectiveTransform(
            MatOfPoint2f(*srcPoints),
            MatOfPoint2f(Point(0.0, 0.0), Point(width - 1.0, 0.0), Point(width - 1.0, height - 1.0), Point(0.0, height - 1.0)),
        )
        val warped = Mat(height, width, source.type())
        Imgproc.warpPerspective(source, warped, transform, Size(width.toDouble(), height.toDouble()), Imgproc.INTER_CUBIC, Core.BORDER_REPLICATE)
        val output = applyFilter(warped, filter)
        val bitmapOut = Bitmap.createBitmap(output.cols(), output.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(output, bitmapOut)
        source.release(); transform.release(); warped.release(); if (output !== warped) output.release()
        return bitmapOut
    }

    private fun applyFilter(source: Mat, filter: DocumentFilter): Mat = when (filter) {
        DocumentFilter.Original -> source
        DocumentFilter.ColorEnhanced -> source.clone().also { Core.normalize(source, it, 0.0, 255.0, Core.NORM_MINMAX) }
        DocumentFilter.Grayscale -> Mat().also { Imgproc.cvtColor(source, it, Imgproc.COLOR_RGBA2GRAY); Imgproc.cvtColor(it, it, Imgproc.COLOR_GRAY2RGBA) }
        DocumentFilter.BlackWhite -> {
            val gray = Mat(); Imgproc.cvtColor(source, gray, Imgproc.COLOR_RGBA2GRAY)
            Imgproc.adaptiveThreshold(gray, gray, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 31, 13.0)
            Mat().also { Imgproc.cvtColor(gray, it, Imgproc.COLOR_GRAY2RGBA); gray.release() }
        }
        DocumentFilter.WhitePaper -> {
            val lab = Mat(); Imgproc.cvtColor(source, lab, Imgproc.COLOR_RGBA2RGB)
            val background = Mat(); Imgproc.GaussianBlur(lab, background, Size(0.0, 0.0), 18.0)
            Mat().also { Core.divide(lab, background, it, 245.0); Core.normalize(it, it, 0.0, 255.0, Core.NORM_MINMAX); Imgproc.cvtColor(it, it, Imgproc.COLOR_RGB2RGBA); lab.release(); background.release() }
        }
    }

    private fun distance(a: Point, b: Point) = hypot(a.x - b.x, a.y - b.y)

    private fun order(points: Array<Point>): Array<Point> {
        val topLeft = points.minBy { it.x + it.y }
        val bottomRight = points.maxBy { it.x + it.y }
        val topRight = points.minBy { it.y - it.x }
        val bottomLeft = points.maxBy { it.y - it.x }
        return arrayOf(topLeft, topRight, bottomRight, bottomLeft)
    }
}
