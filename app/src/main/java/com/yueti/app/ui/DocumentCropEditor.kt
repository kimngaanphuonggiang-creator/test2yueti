package com.yueti.app.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.yueti.scanner.core.DocumentFilter
import com.yueti.scanner.core.DocumentPoint
import kotlin.math.hypot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DocumentCropEditor(
    bitmap: Bitmap,
    initialCorners: List<DocumentPoint>,
    onDismiss: () -> Unit,
    onApply: (List<DocumentPoint>, DocumentFilter) -> Unit,
) {
    var corners by remember(bitmap) { mutableStateOf(initialCorners) }
    var filter by remember { mutableStateOf(DocumentFilter.WhitePaper) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 26.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("调整纸张边缘", style = MaterialTheme.typography.headlineSmall)
            Text("拖动四个圆点，放大边缘区域后再生成扫描件。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            var canvasSize by remember { mutableStateOf(IntSize.Zero) }
            Box(
                Modifier.fillMaxWidth().height(430.dp).background(Color(0xFF171318), RoundedCornerShape(24.dp))
                    .onSizeChanged { canvasSize = it }
                    .pointerInput(canvasSize, corners) {
                        var active = -1
                        detectDragGestures(
                            onDragStart = { p ->
                                active = corners.indices.minByOrNull { i -> hypot(p.x - corners[i].x * size.width, p.y - corners[i].y * size.height) } ?: -1
                            },
                            onDragEnd = { active = -1 },
                            onDragCancel = { active = -1 },
                        ) { change, _ ->
                            if (active >= 0) {
                                change.consume()
                                corners = corners.toMutableList().also {
                                    it[active] = DocumentPoint((change.position.x / size.width).coerceIn(.01f, .99f), (change.position.y / size.height).coerceIn(.01f, .99f))
                                }
                            }
                        }
                    },
            ) {
                Image(bitmap.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                Canvas(Modifier.fillMaxSize()) {
                    if (corners.size == 4) {
                        val pts = corners.map { Offset(it.x * size.width, it.y * size.height) }
                        val path = Path().apply { moveTo(pts[0].x, pts[0].y); pts.drop(1).forEach { lineTo(it.x, it.y) }; close() }
                        drawPath(path, Color(0xFFCCFF62), style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
                        pts.forEach { drawCircle(Color.White, 14.dp.toPx(), it); drawCircle(Color(0xFF6D28D9), 9.dp.toPx(), it) }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(
                    DocumentFilter.Original to "原图", DocumentFilter.ColorEnhanced to "彩色",
                    DocumentFilter.Grayscale to "灰度", DocumentFilter.BlackWhite to "黑白", DocumentFilter.WhitePaper to "白纸",
                ).forEach { (value, label) -> FilterChip(selected = filter == value, onClick = { filter = value }, label = { Text(label) }) }
            }
            Button(onClick = { onApply(corners, filter) }, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("透视矫正并添加页面") }
        }
    }
}
