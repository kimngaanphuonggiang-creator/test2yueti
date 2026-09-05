package com.yueti.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.hypot

internal enum class ScratchTool { Pen, Eraser }
internal enum class EraserMode { Stroke, Pixel }

internal data class ScratchStroke(
    val points: SnapshotStateList<Offset>,
    val widthPx: Float,
)

internal class ScratchPageState {
    val strokes = mutableStateListOf<ScratchStroke>()
    private val undoStates = mutableStateListOf<List<ScratchStroke>>()
    private val redoStates = mutableStateListOf<List<ScratchStroke>>()
    var tool by mutableStateOf(ScratchTool.Pen)
    var eraserMode by mutableStateOf(EraserMode.Stroke)
    var penSize by mutableFloatStateOf(4f)
    var eraserSize by mutableFloatStateOf(24f)

    val canUndo: Boolean get() = undoStates.isNotEmpty()
    val canRedo: Boolean get() = redoStates.isNotEmpty()

    private fun snapshot(): List<ScratchStroke> = strokes.map { stroke ->
        ScratchStroke(stroke.points.toMutableStateList(), stroke.widthPx)
    }

    private fun restore(snapshot: List<ScratchStroke>) {
        strokes.clear()
        strokes.addAll(snapshot.map { stroke -> ScratchStroke(stroke.points.toMutableStateList(), stroke.widthPx) })
    }

    private fun checkpoint() {
        undoStates.add(snapshot())
        if (undoStates.size > 48) undoStates.removeAt(0)
        redoStates.clear()
    }

    fun undo() {
        val previous = undoStates.removeLastOrNull() ?: return
        redoStates.add(snapshot())
        restore(previous)
    }

    fun redo() {
        val next = redoStates.removeLastOrNull() ?: return
        undoStates.add(snapshot())
        restore(next)
    }

    fun clear() {
        if (strokes.isEmpty()) return
        checkpoint()
        strokes.clear()
    }

    fun beginStroke(point: Offset, widthPx: Float): ScratchStroke =
        ScratchStroke(mutableStateListOf(point), widthPx).also {
            checkpoint()
            strokes.add(it)
        }

    fun appendPoint(stroke: ScratchStroke, point: Offset) {
        stroke.points.add(point)
    }

    /** Records one undo unit before the many samples produced by a single erase gesture. */
    fun beginEraseGesture() = checkpoint()

    fun eraseAt(point: Offset, radius: Float) {
        if (eraserMode == EraserMode.Stroke) {
            strokes.removeAll { stroke -> stroke.points.any { it.distanceTo(point) <= radius } }
            return
        }
        val rebuilt = buildList {
            strokes.forEach { stroke ->
                var segment = mutableStateListOf<Offset>()
                fun flush() {
                    if (segment.isNotEmpty()) add(ScratchStroke(segment, stroke.widthPx))
                    segment = mutableStateListOf()
                }
                stroke.points.forEach { sample ->
                    if (sample.distanceTo(point) <= radius) flush() else segment.add(sample)
                }
                flush()
            }
        }
        strokes.clear()
        strokes.addAll(rebuilt)
    }
}

@Composable
internal fun ScratchQuestionLayer(
    state: ScratchPageState,
    expanded: Boolean,
    animationsEnabled: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    question: @Composable () -> Unit,
) {
    var heightPx by remember { mutableFloatStateOf(1f) }
    var localExpanded by remember { mutableStateOf(expanded) }
    var dragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(0f) }
    androidx.compose.runtime.LaunchedEffect(expanded) { localExpanded = expanded }
    val expandedOffset = heightPx * .86f
    val restingOffset = if (localExpanded) expandedOffset else 0f
    val sheetOffset by animateFloatAsState(
        targetValue = if (dragging) dragPosition else restingOffset,
        animationSpec = if (dragging || !animationsEnabled) snap() else spring(dampingRatio = .76f, stiffness = 360f),
        label = "scratch sheet offset",
    )
    val actualOffset = sheetOffset.coerceIn(0f, heightPx * .9f)
    val revealProgress = (actualOffset / expandedOffset.coerceAtLeast(1f)).coerceIn(0f, 1f)
    val dragState = rememberDraggableState { delta ->
        dragPosition = (dragPosition + delta).coerceIn(0f, heightPx * .9f)
    }

    Box(
        Modifier.fillMaxSize().background(Color(0xFFF8F5EC)).onSizeChanged { heightPx = it.height.toFloat().coerceAtLeast(1f) },
    ) {
        ScratchCanvas(state, Modifier.fillMaxSize().padding(bottom = 138.dp))
        ScratchToolbar(
            state = state,
            onClose = { localExpanded = false; onExpandedChange(false) },
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp).graphicsLayer {
                alpha = ((revealProgress - .08f) / .32f).coerceIn(0f, 1f)
                translationY = (1f - revealProgress) * -24.dp.toPx()
            },
        )

        Box(
            Modifier.fillMaxSize().graphicsLayer { translationY = actualOffset }
                .draggable(
                    state = dragState,
                    orientation = Orientation.Vertical,
                    onDragStarted = { dragging = true; dragPosition = actualOffset },
                    onDragStopped = { velocity ->
                        val projected = dragPosition + velocity * .09f
                        val next = when {
                            velocity > 780f -> true
                            velocity < -780f -> false
                            else -> projected > heightPx * .34f
                        }
                        localExpanded = next
                        dragging = false
                        onExpandedChange(next)
                    },
                ),
        ) {
            question()
            Surface(
                onClick = { localExpanded = !localExpanded; onExpandedChange(localExpanded) },
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = CircleShape,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp).height(34.dp)
                    .semantics { contentDescription = if (expanded) "收起草稿纸" else "下拉打开本题草稿纸" },
            ) {
                Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(28.dp, 4.dp).background(MaterialTheme.colorScheme.outline, CircleShape))
                }
            }
        }
    }
}

@Composable
private fun ScratchCanvas(state: ScratchPageState, modifier: Modifier = Modifier) {
    var active by remember { mutableStateOf<ScratchStroke?>(null) }
    Canvas(
        modifier.pointerInput(state.tool, state.penSize, state.eraserSize) {
            detectDragGestures(
                onDragStart = { start ->
                    if (state.tool == ScratchTool.Pen) {
                        active = state.beginStroke(start, state.penSize * density)
                    } else {
                        state.beginEraseGesture()
                        state.eraseAt(start, state.eraserSize * density)
                    }
                },
                onDragEnd = { active = null },
                onDragCancel = { active = null },
            ) { change, _ ->
                change.consume()
                if (state.tool == ScratchTool.Pen) {
                    active?.let { state.appendPoint(it, change.position) }
                } else {
                    state.eraseAt(change.position, state.eraserSize * density)
                }
            }
        },
    ) {
        val grid = 28.dp.toPx()
        var x = 0f
        while (x < size.width) { drawLine(Color(0x1A5C4A35), Offset(x, 0f), Offset(x, size.height), 1f); x += grid }
        var y = 0f
        while (y < size.height) { drawLine(Color(0x1A5C4A35), Offset(0f, y), Offset(size.width, y), 1f); y += grid }
        state.strokes.forEach { stroke ->
            if (stroke.points.size == 1) drawCircle(Color(0xFF171318), stroke.widthPx / 2f, stroke.points.first())
            else stroke.points.zipWithNext().forEach { (a, b) -> drawLine(Color(0xFF171318), a, b, stroke.widthPx, StrokeCap.Round) }
        }
    }
}

@Composable
private fun ScratchToolbar(state: ScratchPageState, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(26.dp), color = Color(0xF2211E22), contentColor = Color.White, shadowElevation = 8.dp) {
        Column(Modifier.fillMaxWidth(.94f).padding(horizontal = 8.dp, vertical = 5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                IconButton(onClick = { state.tool = ScratchTool.Pen }) { Icon(Icons.Rounded.Brush, "画笔", tint = if (state.tool == ScratchTool.Pen) Color(0xFFCCFF62) else Color.White) }
                IconButton(onClick = { state.tool = ScratchTool.Eraser }) { Icon(Icons.Rounded.CleaningServices, "橡皮擦", tint = if (state.tool == ScratchTool.Eraser) Color(0xFFCCFF62) else Color.White) }
                Slider(
                    value = if (state.tool == ScratchTool.Pen) state.penSize else state.eraserSize,
                    onValueChange = { if (state.tool == ScratchTool.Pen) state.penSize = it else state.eraserSize = it },
                    valueRange = if (state.tool == ScratchTool.Pen) 1f..16f else 8f..48f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFD7FF72),
                        activeTrackColor = Color(0xFFB46CFF),
                        inactiveTrackColor = Color.White.copy(alpha = .28f),
                    ),
                    modifier = Modifier.weight(1f).semantics { contentDescription = if (state.tool == ScratchTool.Pen) "画笔粗细" else "橡皮擦大小" },
                )
                IconButton(onClick = state::undo, enabled = state.canUndo) { Icon(Icons.AutoMirrored.Rounded.Undo, "撤销") }
                IconButton(onClick = state::redo, enabled = state.canRedo) { Icon(Icons.AutoMirrored.Rounded.Redo, "重做") }
                IconButton(onClick = state::clear, enabled = state.strokes.isNotEmpty()) { Icon(Icons.Rounded.Clear, "清空") }
                IconButton(onClick = onClose) { Icon(Icons.Rounded.ExpandLess, "收起草稿纸") }
            }
            if (state.tool == ScratchTool.Eraser) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    FilterChip(
                        selected = state.eraserMode == EraserMode.Stroke,
                        onClick = { state.eraserMode = EraserMode.Stroke },
                        label = { androidx.compose.material3.Text("整笔擦除") },
                        colors = scratchModeChipColors(),
                    )
                    androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
                    FilterChip(
                        selected = state.eraserMode == EraserMode.Pixel,
                        onClick = { state.eraserMode = EraserMode.Pixel },
                        label = { androidx.compose.material3.Text("像素擦除") },
                        colors = scratchModeChipColors(),
                    )
                }
            }
        }
    }
}

private fun Offset.distanceTo(other: Offset): Float = hypot(x - other.x, y - other.y)

@Composable
private fun scratchModeChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = Color.Transparent,
    labelColor = Color.White,
    selectedContainerColor = Color(0xFF5A147E),
    selectedLabelColor = Color.White,
)
