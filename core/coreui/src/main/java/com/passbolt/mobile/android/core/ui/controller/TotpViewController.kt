package com.passbolt.mobile.android.core.ui.controller

import android.animation.ObjectAnimator
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.passbolt.mobile.android.common.OtpFormatter
import com.passbolt.mobile.android.core.ui.R
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TotpViewController : KoinComponent {

    @Suppress("MagicNumber")
    private val rotateAnimation = RotateAnimation(
        0f, 180f,
        Animation.RELATIVE_TO_SELF, 0.5f,
        Animation.RELATIVE_TO_SELF, 0.5f
    ).apply {
        interpolator = FastOutSlowInInterpolator()
        duration = 500
        repeatCount = Animation.INFINITE
    }

    private val otpFormatter: OtpFormatter by inject()

    fun updateView(
        viewParameters: ViewParameters,
        stateParameters: StateParameters,
        timeParameters: TimeParameters
    ) {
        with(viewParameters.progressIndicator) {
            min = 0
            max = timeParameters.otpExpirySeconds?.let { it.toInt() * ANIMATION_MULTIPLIER } ?: 0
            progress = timeParameters.remainingSecondsCounter?.let { (it.toInt() + 1) * ANIMATION_MULTIPLIER } ?: 0
        }

        viewParameters.otpText.text = otpFormatter.format(
            stateParameters.otpValue ?: viewParameters.otpText.context.getString(R.string.otp_hide_otp)
        )
        viewParameters.progressIndicator.isVisible = stateParameters.isOtpVisible

        updateIsRefreshing(
            viewParameters.generationInProgressImage,
            stateParameters.isOtpRefreshing
        )
        updateProgress(
            viewParameters.progressIndicator,
            timeParameters.remainingSecondsCounter
        )
        updateColors(
            viewParameters.otpText,
            viewParameters.progressIndicator,
            timeParameters.otpExpirySeconds,
            timeParameters.remainingSecondsCounter
        )
    }

    private fun updateIsRefreshing(generationInProgressImage: ImageView, isOtpRefreshing: Boolean) {
        generationInProgressImage.let { inProgressView ->
            inProgressView.isVisible = isOtpRefreshing
            if (isOtpRefreshing) {
                inProgressView.startAnimation(rotateAnimation)
            } else {
                inProgressView.clearAnimation()
            }
        }
    }

    private fun updateProgress(progressIndicator: CircularProgressIndicator, remainingSecondsCounter: Long?) {
        val progressAnimator = ObjectAnimator.ofInt(
            progressIndicator,
            PROPERTY_PROGRESS,
            remainingSecondsCounter?.let { it.toInt() * ANIMATION_MULTIPLIER } ?: 0
        ).apply {
            duration = PROGRESS_ANIMATION_DURATION_MILLIS
            interpolator = LinearInterpolator()
        }
        progressAnimator.start()
    }

    /**
     * If progress <= 25% highlight otp value and progress in red
     */
    private fun updateColors(
        otpText: TextView,
        progressIndicator: CircularProgressIndicator,
        otpExpirySeconds: Long?,
        remainingSecondsCounter: Long?
    ) {
        val progressPercentage =
            if (remainingSecondsCounter != null && otpExpirySeconds != null) {
                remainingSecondsCounter.toFloat() / otpExpirySeconds
            } else {
                MAX_PERCENTAGE
            }
        if (progressPercentage <= RED_HIGHLIGHT_PROGRESS_PERCENTAGE) {
            otpText.setTextColor(otpText.context.getColor(R.color.red))
            progressIndicator.setIndicatorColor(progressIndicator.context.getColor(R.color.red))
        } else {
            otpText.setTextColor(otpText.context.getColor(R.color.text_primary))
            progressIndicator.setIndicatorColor(progressIndicator.context.getColor(R.color.icon_tint))
        }
    }

    data class ViewParameters(
        val progressIndicator: CircularProgressIndicator,
        val otpText: TextView,
        val generationInProgressImage: ImageView
    )

    data class StateParameters(
        val isOtpRefreshing: Boolean,
        val isOtpVisible: Boolean,
        val otpValue: String?
    )

    data class TimeParameters(
        val otpExpirySeconds: Long?,
        val remainingSecondsCounter: Long?
    )

    private companion object {
        private const val ANIMATION_MULTIPLIER = 1_000
        private const val RED_HIGHLIGHT_PROGRESS_PERCENTAGE = 0.25
        private const val MAX_PERCENTAGE = 1f
        private const val PROGRESS_ANIMATION_DURATION_MILLIS = 1_000L
        private const val PROPERTY_PROGRESS = "progress"
    }
}
