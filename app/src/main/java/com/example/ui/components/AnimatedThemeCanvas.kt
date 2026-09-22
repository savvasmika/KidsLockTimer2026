package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.model.KidTheme
import com.example.model.ParticleType
import kotlin.random.Random

private data class Particle(
    val xRatio: Float,
    val yRatio: Float,
    val size: Float,
    val speed: Float,
    val color: Color,
    val alpha: Float,
    val rotationOffset: Float
)

@Composable
fun AnimatedThemeCanvas(
    theme: KidTheme,
    modifier: Modifier = Modifier,
    animationsEnabled: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "theme_anim")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "anim_progress"
    )

    val particles = remember(theme.id) {
        val random = Random(theme.id.hashCode())
        List(25) {
            Particle(
                xRatio = random.nextFloat(),
                yRatio = random.nextFloat(),
                size = random.nextFloat() * 14f + 6f,
                speed = random.nextFloat() * 0.5f + 0.5f,
                color = when (random.nextInt(3)) {
                    0 -> theme.accentColor
                    1 -> theme.primaryColor
                    else -> theme.secondaryColor
                },
                alpha = random.nextFloat() * 0.5f + 0.3f,
                rotationOffset = random.nextFloat() * 360f
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Subtle gradient background
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    theme.backgroundColor,
                    theme.surfaceColor
                )
            )
        )

        // Draw animated particles
        particles.forEach { p ->
            val animatedProgress = if (animationsEnabled) (progress * p.speed) % 1f else 0f
            val currentY = (p.yRatio * height + animatedProgress * height) % height
            val currentX = (p.xRatio * width + (if (theme.particleType == ParticleType.SPEED_LINES) animatedProgress * 150f else 0f)) % width
            val animatedAlpha = if (animationsEnabled) p.alpha * (0.6f + 0.4f * kotlin.math.sin(animatedProgress * 6.28f).toFloat()) else p.alpha

            when (theme.particleType) {
                ParticleType.STARS, ParticleType.MAGIC_SPARKLES -> {
                    // Glowing diamond star
                    rotate(degrees = if (animationsEnabled) progress * 360f + p.rotationOffset else p.rotationOffset, pivot = Offset(currentX, currentY)) {
                        drawCircle(
                            color = p.color.copy(alpha = animatedAlpha),
                            radius = p.size,
                            center = Offset(currentX, currentY)
                        )
                        drawRect(
                            color = Color.White.copy(alpha = animatedAlpha * 0.8f),
                            topLeft = Offset(currentX - p.size / 3, currentY - p.size / 3),
                            size = Size(p.size * 0.66f, p.size * 0.66f)
                        )
                    }
                }
                ParticleType.BUBBLES, ParticleType.RAINBOW_CIRCLES -> {
                    drawCircle(
                        color = p.color.copy(alpha = animatedAlpha * 0.6f),
                        radius = p.size * 1.5f,
                        center = Offset(currentX, currentY)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = animatedAlpha * 0.7f),
                        radius = p.size * 0.4f,
                        center = Offset(currentX - p.size * 0.4f, currentY - p.size * 0.4f)
                    )
                }
                ParticleType.GAMING_BLOCKS -> {
                    drawRect(
                        color = p.color.copy(alpha = animatedAlpha),
                        topLeft = Offset(currentX, currentY),
                        size = Size(p.size * 1.5f, p.size * 1.5f)
                    )
                }
                ParticleType.SPEED_LINES -> {
                    drawLine(
                        color = p.color.copy(alpha = animatedAlpha),
                        start = Offset(currentX - p.size * 3, currentY),
                        end = Offset(currentX + p.size * 3, currentY),
                        strokeWidth = p.size * 0.4f
                    )
                }
                ParticleType.ROBOT_GEARS -> {
                    rotate(degrees = if (animationsEnabled) progress * 360f else 0f, pivot = Offset(currentX, currentY)) {
                        drawCircle(
                            color = p.color.copy(alpha = animatedAlpha),
                            radius = p.size,
                            center = Offset(currentX, currentY)
                        )
                        drawCircle(
                            color = theme.backgroundColor,
                            radius = p.size * 0.4f,
                            center = Offset(currentX, currentY)
                        )
                    }
                }
                else -> {
                    // Default soft glowing circle
                    drawCircle(
                        color = p.color.copy(alpha = animatedAlpha),
                        radius = p.size,
                        center = Offset(currentX, currentY)
                    )
                }
            }
        }
    }
}
