package com.yueti.app.music

import kotlin.math.exp

enum class MusicChromePhase {
    Collapsed,
    Mini,
    ExpandingToPage,
    FullPage,
    FanQueue,
}

internal enum class MusicFanPhase { Entering, Browsing, Snapping, Merging, Closing }

enum class MusicGestureAxis { Undecided, Horizontal, Vertical }

data class MusicNavRevealState(
    val progress: Float = 0f,
    val settledExpanded: Boolean = false,
    val axis: MusicGestureAxis = MusicGestureAxis.Undecided,
)

data class MusicChromeTransform(
    val heightDp: Float,
    val cornerDp: Float,
    val iconTranslationYDp: Float,
    val iconScale: Float,
    val iconAlpha: Float,
    val playerReveal: Float,
    val playerRotationX: Float,
)

data class MusicFanPose(
    val relativeIndex: Int,
    val xDp: Float,
    val yDp: Float,
    val rotationDegrees: Float,
    val scale: Float,
    val alpha: Float,
    val elevationDp: Float,
)

data class MusicRevealRelease(
    val target: Float,
    val initialVelocity: Float,
)

fun musicChromeTransform(progress: Float): MusicChromeTransform {
    val p = progress.coerceIn(0f, 1f)
    val reveal = ((p - .05f) / .75f).coerceIn(0f, 1f)
    val iconFade = (1f - ((p - .35f) / .4f).coerceIn(0f, 1f))
    return MusicChromeTransform(
        // Navigation and mini player are two contents inside one immutable surface.
        heightDp = 64f,
        cornerDp = 20f,
        iconTranslationYDp = 18f * p,
        iconScale = 1f - .06f * p,
        iconAlpha = iconFade,
        playerReveal = reveal,
        playerRotationX = -12f * (1f - reveal),
    )
}

fun resolveMusicRevealTarget(progress: Float, velocityDpPerSecond: Float): Float = when {
    velocityDpPerSecond >= 900f -> 1f
    velocityDpPerSecond <= -900f -> 0f
    progress >= .45f -> 1f
    else -> 0f
}

internal fun resolveMusicRevealRelease(progress: Float, velocityDpPerSecond: Float): MusicRevealRelease =
    MusicRevealRelease(
        target = resolveMusicRevealTarget(progress, velocityDpPerSecond),
        // Animatable uses progress units/second. Keep the release energy visible without
        // allowing a fast edge swipe to overshoot the entire navigation surface.
        initialVelocity = (velocityDpPerSecond / 48f * .16f).coerceIn(-4f, 4f),
    )

/** The bottom edge only leaves about 48dp of finger travel; retain bounded rubber-band travel. */
fun musicRevealProgressForDrag(startProgress: Float, dragDp: Float): Float {
    val raw = startProgress.coerceIn(0f, 1f) + dragDp / 48f
    return when {
        raw < 0f -> -.16f * (1f - exp(raw * 2.4f))
        raw > 1f -> 1f + .16f * (1f - exp(-(raw - 1f) * 2.4f))
        else -> raw
    }
}

fun musicFanPose(relativeIndex: Int, expansion: Float, visualSlot: Int? = null): MusicFanPose {
    require(relativeIndex in -2..2) { "Dock fan only supports the current track and two neighbours per side" }
    val p = expansion.coerceIn(0f, 1f)
    val defaultStackOrder = when (relativeIndex) {
        0 -> 0f
        -1 -> 1f
        1 -> 2f
        -2 -> 3f
        else -> 4f
    }
    val stackOrder = visualSlot?.coerceAtLeast(0)?.toFloat() ?: defaultStackOrder
    return MusicFanPose(
        relativeIndex = relativeIndex,
        // The artwork column is the shared Dock pivot. Curvature comes from rotating
        // the card body around that pivot instead of shifting the artwork sideways.
        xDp = 0f,
        yDp = -stackOrder * 64f * p,
        rotationDegrees = if (relativeIndex == 0) 0f else -(0.35f + stackOrder * .42f) * p,
        scale = 1f - stackOrder * .02f * p,
        alpha = 1f - stackOrder * .055f * p,
        elevationDp = (18f - stackOrder * 2.5f) * p,
    )
}
