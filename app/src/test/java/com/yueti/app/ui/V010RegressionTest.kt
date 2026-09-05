package com.yueti.app.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V010RegressionTest {
    @Test
    fun emotionEnvelopeAcceptsProviderVariantsAndNeverLeaksTheMarker() {
        listOf(
            "[[emotion:14]]\n和你一起探索数学。",
            "[emotion:14] 和你一起探索数学。",
            "[emotion]:14和你一起探索数学。",
        ).forEach { raw ->
            val parsed = parseEmotionEnvelope(raw)
            assertEquals("14", parsed.emotionId)
            assertEquals("和你一起探索数学。", parsed.visibleText)
            assertFalse(parsed.visibleText.contains("emotion", ignoreCase = true))
        }
    }

    @Test
    fun everyUpstreamEmotionIdIsAccepted() {
        assertEquals(32, EmotionBallIds.all.size)
        assertTrue(listOf("00", "07", "10", "21", "30", "41").all(EmotionBallIds.all::contains))
    }

    @Test
    fun vocabularyProgressStaysContinuousAcrossCurrentPageBoundary() {
        val before = vocabularyPagerProgress(currentPage = 0, offsetFraction = .49f, pageCount = 20)
        val after = vocabularyPagerProgress(currentPage = 1, offsetFraction = -.49f, pageCount = 20)

        assertTrue(after > before)
        assertTrue(after - before < .002f)
    }

    @Test
    fun mathKeyboardReplacesSelectionAndBackspacesAtCursor() {
        val selected = TextFieldValue("sin(x)", selection = TextRange(4, 5))
        val replaced = applyMathKey(selected, "theta")
        assertEquals("sin(theta)", replaced.text)
        assertEquals(9, replaced.selection.start)

        val erased = applyMathKey(replaced, "BACK")
        assertEquals("sin(thet)", erased.text)
        assertEquals(8, erased.selection.start)
    }

    @Test
    fun questionQuantityIsContinuousWithinTheRealBankCapacity() {
        assertEquals(1..25, questionQuantityRange(BankType.Addition))
        assertEquals(1..25, questionQuantityRange(BankType.Subtraction))
        assertEquals(1..50, questionQuantityRange(BankType.Mixed))
    }

    @Test
    fun firstScratchStrokeMutatesObservablePointStorageImmediately() {
        val state = ScratchPageState()
        val stroke = state.beginStroke(Offset(1f, 2f), 4f)
        state.appendPoint(stroke, Offset(3f, 4f))
        assertEquals(2, stroke.points.size)
        assertEquals(1, state.strokes.size)
    }

    @Test
    fun scratchEraserSupportsWholeStrokeAndPixelModes() {
        val state = ScratchPageState()
        val whole = state.beginStroke(Offset(0f, 0f), 4f)
        state.appendPoint(whole, Offset(10f, 0f))
        state.eraserMode = EraserMode.Stroke
        state.eraseAt(Offset(5f, 0f), 6f)
        assertTrue(state.strokes.isEmpty())

        val pixels = state.beginStroke(Offset(0f, 0f), 4f)
        state.appendPoint(pixels, Offset(10f, 0f))
        state.appendPoint(pixels, Offset(20f, 0f))
        state.eraserMode = EraserMode.Pixel
        state.eraseAt(Offset(10f, 0f), 2f)
        assertEquals(2, state.strokes.size)
        assertTrue(state.strokes.none { stroke -> Offset(10f, 0f) in stroke.points })
    }

    @Test
    fun eraseGestureIsOneUndoableOperation() {
        val state = ScratchPageState()
        val stroke = state.beginStroke(Offset(0f, 0f), 4f)
        state.appendPoint(stroke, Offset(10f, 0f))
        state.appendPoint(stroke, Offset(20f, 0f))

        state.eraserMode = EraserMode.Stroke
        state.beginEraseGesture()
        state.eraseAt(Offset(10f, 0f), 3f)
        assertTrue(state.strokes.isEmpty())

        state.undo()
        assertEquals(1, state.strokes.size)
        assertEquals(3, state.strokes.single().points.size)
        state.redo()
        assertTrue(state.strokes.isEmpty())
    }
}
