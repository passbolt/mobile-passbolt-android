package com.passbolt.mobile.android.core.ui.controller

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.passbolt.mobile.android.core.ui.R

class TotpComposeController {
    data class TotpColors(
        val otpTextColor: Color,
        val progressIndicatorColor: Color,
    )

    data class TotpAnimations(
        val animatedProgress: Float,
        val rotationAngle: Float,
    )

    @Composable
    fun calculateTotpColors(
        remainingSeconds: Long?,
        expirySeconds: Long?,
        isOtpVisible: Boolean,
    ): TotpColors {
        val progressPercentage = calculateProgressPercentage(remainingSeconds, expirySeconds)
        val shouldHighlightRed =
            progressPercentage <= RED_HIGHLIGHT_PROGRESS_PERCENTAGE && isOtpVisible

        val otpTextColor: Color =
            if (shouldHighlightRed) {
                colorResource(R.color.red).copy(alpha = 1f)
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 1f)
            }

        val progressIndicatorColor: Color =
            if (shouldHighlightRed) {
                colorResource(R.color.red).copy(alpha = 1f)
            } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 1f)
            }

        return TotpColors(otpTextColor, progressIndicatorColor)
    }

    @Composable
    fun calculateTotpAnimations(
        remainingSeconds: Long?,
        expirySeconds: Long?,
    ): TotpAnimations {
        val progressPercentage = calculateProgressPercentage(remainingSeconds, expirySeconds)

        val animatedProgress = progressPercentage

        val infiniteTransition = rememberInfiniteTransition(label = "refresh_rotation")
        val rotationAngle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = REFRESH_ANIMATION_DURATION_MILLIS, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "rotation",
        )

        return TotpAnimations(animatedProgress, rotationAngle)
    }

    private fun calculateProgressPercentage(
        remainingSeconds: Long?,
        expirySeconds: Long?,
    ): Float =
        if (remainingSeconds != null && expirySeconds != null) {
            remainingSeconds.toFloat() / expirySeconds.toFloat()
        } else {
            MAX_PERCENTAGE
        }

    private companion object {
        private const val RED_HIGHLIGHT_PROGRESS_PERCENTAGE = 0.25f
        private const val MAX_PERCENTAGE = 1f
        private const val REFRESH_ANIMATION_DURATION_MILLIS = 1_000
    }
}
