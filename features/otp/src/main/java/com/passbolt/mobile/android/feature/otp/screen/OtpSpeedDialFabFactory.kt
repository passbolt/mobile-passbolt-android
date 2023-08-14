package com.passbolt.mobile.android.feature.otp.screen

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.Gravity
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.updateMargins
import com.leinardi.android.speeddial.SpeedDialActionItem
import com.leinardi.android.speeddial.SpeedDialOverlayLayout
import com.leinardi.android.speeddial.SpeedDialView
import com.passbolt.mobile.android.feature.home.R
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

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
class OtpSpeedDialFabFactory(
    val context: Context
) {

    var scanQrCodeClick: (() -> Unit)? = null
    var createManuallyClick: (() -> Unit)? = null

    private val backgroundColor = ResourcesCompat.getColor(context.resources, CoreUiR.color.background, context.theme)
    private val tintColor = ResourcesCompat.getColor(context.resources, CoreUiR.color.icon_tint, context.theme)
    private val primaryColor = ResourcesCompat.getColor(context.resources, CoreUiR.color.primary, context.theme)
    private val textColor = ResourcesCompat.getColor(context.resources, CoreUiR.color.text_primary, context.theme)

    fun getSpeedDialFab(context: Context, overlay: SpeedDialOverlayLayout) =
        otpSpeedDial(context, overlay)

    /**
     * Single FAB without speed dial. Upon click add a new resource.
     */
    private fun mainFab(context: Context, overlay: SpeedDialOverlayLayout) =
        SpeedDialView(context).apply {
            val margin = context.resources.getDimension(CoreUiR.dimen.dp_16).toInt()
            id = R.id.otpSpeedDialViewId
            overlayLayout = overlay

            layoutParams = CoordinatorLayout.LayoutParams(
                CoordinatorLayout.LayoutParams.WRAP_CONTENT,
                CoordinatorLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                updateMargins(margin, margin, margin, margin)
                gravity = Gravity.BOTTOM or Gravity.END
                behavior = SpeedDialView.ScrollingViewSnackbarBehavior()
            }

            mainFabClosedBackgroundColor = primaryColor
            mainFabOpenedBackgroundColor = backgroundColor

            setMainFabClosedDrawable(
                ContextCompat.getDrawable(context, CoreUiR.drawable.ic_plus)
                    ?.mutate()
                    ?.apply {
                        colorFilter = PorterDuffColorFilter(tintColor, PorterDuff.Mode.SRC_IN)
                    }
            )

            // disable ripple as there is a shadow bug on some APIs in material design lib
            mainFab.rippleColor = Color.TRANSPARENT
        }

    /**
     * FAB with two action items: add resource & add folders.
     */
    private fun otpSpeedDial(context: Context, overlay: SpeedDialOverlayLayout) =
        mainFab(context, overlay).apply {
            addSpeedDialItem(
                R.id.otpSpeedDialViewCreateManually,
                CoreUiR.drawable.ic_write,
                LocalizationR.string.otp_speed_dial_create_manually
            )
            addSpeedDialItem(
                R.id.otpSpeedDialViewScanQr,
                CoreUiR.drawable.ic_camera,
                LocalizationR.string.otp_speed_dial_scan_qr
            )

            setOnChangeListener(null)
            setOnActionSelectedListener(SpeedDialView.OnActionSelectedListener { actionItem ->
                return@OnActionSelectedListener when (actionItem.id) {
                    R.id.otpSpeedDialViewScanQr -> {
                        scanQrCodeClick?.invoke()
                        close()
                        true
                    }
                    R.id.otpSpeedDialViewCreateManually -> {
                        createManuallyClick?.invoke()
                        close()
                        true
                    }
                    else -> false
                }
            })
        }

    private fun SpeedDialView.addSpeedDialItem(
        @IdRes viewId: Int,
        @DrawableRes drawableId: Int,
        @StringRes labelId: Int
    ) {
        addActionItem(
            SpeedDialActionItem.Builder(viewId, drawableId)
                .setFabBackgroundColor(backgroundColor)
                .setLabelColor(textColor)
                .setLabelBackgroundColor(backgroundColor)
                .setFabImageTintColor(
                    ResourcesCompat.getColor(context.resources, CoreUiR.color.icon_tint, context.theme)
                )
                .setLabel(context.getString(labelId))
                .create()
        )
    }
}
