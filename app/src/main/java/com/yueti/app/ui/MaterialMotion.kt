package com.yueti.app.ui

import android.view.animation.AnimationUtils
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.android.material.motion.MotionUtils

/**
 * Bridges the official Material Components Android motion tokens into Compose.
 * The source implementation lives in material-components/material-components-android.
 */
internal data class MaterialMotionTokens(
    val quick: Int,
    val standard: Int,
    val expressive: Int,
    val emphasizedEasing: Easing,
)

@Composable
internal fun rememberMaterialMotionTokens(): MaterialMotionTokens {
    val context = LocalContext.current
    return remember(context) {
        val emphasized = MotionUtils.resolveThemeInterpolator(
            context,
            com.google.android.material.R.attr.motionEasingEmphasizedInterpolator,
            AnimationUtils.loadInterpolator(context, android.R.interpolator.fast_out_slow_in),
        )
        MaterialMotionTokens(
            quick = MotionUtils.resolveThemeDuration(
                context,
                com.google.android.material.R.attr.motionDurationShort3,
                150,
            ),
            standard = MotionUtils.resolveThemeDuration(
                context,
                com.google.android.material.R.attr.motionDurationMedium2,
                300,
            ),
            expressive = MotionUtils.resolveThemeDuration(
                context,
                com.google.android.material.R.attr.motionDurationLong2,
                500,
            ),
            emphasizedEasing = Easing { fraction -> emphasized.getInterpolation(fraction) },
        )
    }
}
