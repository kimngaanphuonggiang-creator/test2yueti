package com.yueti.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

internal val ToolScreenBackground = Color(0xFF121013)
internal val ToolChromeColor = Color(0xF21A171B)

@Composable
internal fun ToolScreenHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val largeFont = LocalDensity.current.fontScale > 1.1f
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val entrance by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = spring(dampingRatio = .92f, stiffness = 620f),
        label = "tool header entrance",
    )
    Row(
        modifier = modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 4.dp)
            .graphicsLayer { alpha = entrance; translationY = (1f - entrance) * -10f },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedBackSurface(onBack)
        Spacer(Modifier.width(9.dp))
        Surface(
            // A fixed chrome height is intentional. `heightIn(min = …)` together with a
            // fillMaxSize child can consume the parent's entire remaining height when this
            // header is placed above weighted content.
            modifier = Modifier.weight(1f).height(
                when {
                    subtitle == null -> 56.dp
                    largeFont -> 72.dp
                    else -> 64.dp
                },
            ),
            shape = RoundedCornerShape(19.dp),
            color = ToolChromeColor,
            contentColor = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .11f)),
        ) {
            Row(
                Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF8B3DFF).copy(alpha = .12f), Color.Transparent, Color(0xFFD7FF72).copy(alpha = .04f)),
                    ),
                ).padding(start = 17.dp, end = 7.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    subtitle?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = .65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                actions()
            }
        }
    }
}

@Composable
internal fun RollingNumber(
    value: Int,
    animationsEnabled: Boolean,
    modifier: Modifier = Modifier,
    suffix: String = "",
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineSmall,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val previous by rememberUpdatedState(value)
    AnimatedContent(
        targetState = value,
        modifier = modifier,
        transitionSpec = {
            if (!animationsEnabled) {
                fadeIn(tween(0)) togetherWith fadeOut(tween(0))
            } else if (targetState >= initialState) {
                (slideInVertically(tween(200)) { it } + fadeIn(tween(140))) togetherWith
                    (slideOutVertically(tween(200)) { -it } + fadeOut(tween(120)))
            } else {
                (slideInVertically(tween(200)) { -it } + fadeIn(tween(140))) togetherWith
                    (slideOutVertically(tween(200)) { it } + fadeOut(tween(120)))
            }
        },
        label = "rolling number $previous",
    ) { current ->
        Text(
            "$current$suffix",
            style = style,
            color = color,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
    }
}

@Composable
private fun AnimatedBackSurface(onBack: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) .88f else 1f,
        animationSpec = spring(dampingRatio = .7f, stiffness = 760f),
        label = "back surface press",
    )
    Surface(
        modifier = Modifier.size(56.dp).graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        shape = RoundedCornerShape(19.dp),
        color = ToolChromeColor,
        contentColor = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .13f)),
    ) {
        Box(
            Modifier.fillMaxSize().clip(RoundedCornerShape(19.dp)).clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onBack,
            ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                "返回",
                modifier = Modifier.size(25.dp).graphicsLayer { translationX = if (pressed) -3f else 0f },
            )
        }
    }
}
