package com.passbolt.mobile.android.feature.otp.screen.recycler

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import androidx.core.view.isVisible
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import com.mikepenz.fastadapter.listeners.ClickEventHook
import com.passbolt.mobile.android.common.OtpFormatter
import com.passbolt.mobile.android.common.extension.asBinding
import com.passbolt.mobile.android.core.ui.initialsicon.InitialsIconGenerator
import com.passbolt.mobile.android.feature.otp.R
import com.passbolt.mobile.android.feature.otp.databinding.ItemOtpBinding
import com.passbolt.mobile.android.ui.OtpListItemWrapper
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021 Passbolt SA
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * The name "Passbolt" is a registered trademark of Passbolt SA, and Passbolt SA hereby declines to grant a trademark
 * license to "Passbolt" pursuant to the GNU Affero General Public License version 3 Section 7(e), without a separate
 * agreement with Passbolt SA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Passbolt SA (https://www.passbolt.com)
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://www.passbolt.com Passbolt (tm)
 * @since v1.0
 */
class OtpItem(
    val otpModel: OtpListItemWrapper,
    private val initialsIconGenerator: InitialsIconGenerator
) : AbstractBindingItem<ItemOtpBinding>(), KoinComponent {

    override val type: Int
        get() = R.id.itemOtp

    private val otpFormatter: OtpFormatter by inject()

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

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemOtpBinding =
        ItemOtpBinding.inflate(inflater, parent, false)

    override fun bindView(binding: ItemOtpBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)
        with(binding) {
            with(progress) {
                min = 0
                max = otpModel.otpExpirySeconds?.let { it.toInt() * ANIMATION_MULTIPLIER } ?: 0
                progress = otpModel.remainingSecondsCounter?.let { (it.toInt() + 1) * ANIMATION_MULTIPLIER } ?: 0
            }
            name.text = otpModel.otp.name
            otp.text = otpFormatter.format(otpModel.otpValue ?: otp.context.getString(R.string.otp_hide_otp))
            icon.setImageDrawable(initialsIconGenerator.generate(otpModel.otp.name, otpModel.otp.initials))
            eye.isVisible = !otpModel.isVisible && !otpModel.isRefreshing
            progress.isVisible = otpModel.isVisible
            updateIsRefreshing(binding)
            updateProgress(binding)
            updateColors(binding)
        }
    }

    private fun updateIsRefreshing(binding: ItemOtpBinding) {
        binding.generationInProgress.let { inProgressView ->
            inProgressView.isVisible = otpModel.isRefreshing
            if (otpModel.isRefreshing) {
                inProgressView.startAnimation(rotateAnimation)
            } else {
                inProgressView.clearAnimation()
            }
        }
    }

    private fun updateProgress(binding: ItemOtpBinding) {
        val progressAnimator = ObjectAnimator.ofInt(
            binding.progress,
            PROPERTY_PROGRESS,
            otpModel.remainingSecondsCounter?.let { it.toInt() * ANIMATION_MULTIPLIER } ?: 0
        ).apply {
            duration = PROGRESS_ANIMATION_DURATION_MILLIS
            interpolator = LinearInterpolator()
        }
        progressAnimator.start()
    }

    /**
     * If progress <= 25% highlight otp value and progress in red
     */
    private fun updateColors(binding: ItemOtpBinding) {
        val progressPercentage =
            if (otpModel.remainingSecondsCounter != null && otpModel.otpExpirySeconds != null) {
                otpModel.remainingSecondsCounter!!.toFloat() / otpModel.otpExpirySeconds!!
            } else {
                MAX_PERCENTAGE
            }
        with(binding) {
            if (progressPercentage <= RED_HIGHLIGHT_PROGRESS_PERCENTAGE) {
                otp.setTextColor(progress.context.getColor(R.color.red))
                progress.setIndicatorColor(progress.context.getColor(R.color.red))
            } else {
                otp.setTextColor(progress.context.getColor(R.color.text_primary))
                progress.setIndicatorColor(progress.context.getColor(R.color.icon_tint))
            }
        }
    }

    class ItemClick(
        private val clickListener: (OtpListItemWrapper) -> Unit
    ) : ClickEventHook<OtpItem>() {

        override fun onBind(viewHolder: RecyclerView.ViewHolder): View? {
            return viewHolder.asBinding<ItemOtpBinding> {
                it.itemOtp
            }
        }

        override fun onClick(
            v: View,
            position: Int,
            fastAdapter: FastAdapter<OtpItem>,
            item: OtpItem
        ) {
            clickListener.invoke(item.otpModel)
        }
    }

    class ItemMoreClick(
        private val clickListener: (OtpListItemWrapper) -> Unit
    ) : ClickEventHook<OtpItem>() {

        override fun onBind(viewHolder: RecyclerView.ViewHolder): View? {
            return viewHolder.asBinding<ItemOtpBinding> {
                it.more
            }
        }

        override fun onClick(
            v: View,
            position: Int,
            fastAdapter: FastAdapter<OtpItem>,
            item: OtpItem
        ) {
            clickListener.invoke(item.otpModel)
        }
    }

    private companion object {
        private const val ANIMATION_MULTIPLIER = 1_000
        private const val RED_HIGHLIGHT_PROGRESS_PERCENTAGE = 0.25
        private const val MAX_PERCENTAGE = 1f
        private const val PROGRESS_ANIMATION_DURATION_MILLIS = 1_000L
        private const val PROPERTY_PROGRESS = "progress"
    }
}
