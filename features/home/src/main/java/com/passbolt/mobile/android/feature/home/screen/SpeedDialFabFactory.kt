package com.passbolt.mobile.android.feature.home.screen

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
import com.leinardi.android.speeddial.SpeedDialView.OnChangeListener
import com.passbolt.mobile.android.feature.home.R
import com.passbolt.mobile.android.feature.home.screen.model.HomeDisplayViewModel

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
class SpeedDialFabFactory(
    val context: Context
) {

    var addPasswordClick: (() -> Unit)? = null
    var addFolderClick: (() -> Unit)? = null

    private val backgroundColor = ResourcesCompat.getColor(context.resources, R.color.background, context.theme)
    private val tintColor = ResourcesCompat.getColor(context.resources, R.color.icon_tint, context.theme)
    private val primaryColor = ResourcesCompat.getColor(context.resources, R.color.primary, context.theme)
    private val textColor = ResourcesCompat.getColor(context.resources, R.color.text_primary, context.theme)

    fun getSpeedDialFab(context: Context, overlay: SpeedDialOverlayLayout, homeDisplay: HomeDisplayViewModel) =
        when (homeDisplay) {
            is HomeDisplayViewModel.Folders -> foldersSpeedDial(context, overlay)
            else -> mainFab(context, overlay)
        }

    /**
     * Single FAB without speed dial. Upon click add a new resource.
     */
    private fun mainFab(context: Context, overlay: SpeedDialOverlayLayout) =
        SpeedDialView(context).apply {
            val margin = context.resources.getDimension(R.dimen.dp_16).toInt()
            id = R.id.speedDialViewId
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
                ContextCompat.getDrawable(context, R.drawable.ic_plus)
                    ?.mutate()
                    ?.apply {
                        colorFilter = PorterDuffColorFilter(tintColor, PorterDuff.Mode.SRC_IN)
                    }
            )

            // disable ripple as there is a shadow bug on some APIs in material design lib
            mainFab.rippleColor = Color.TRANSPARENT

            setOnChangeListener(object : OnChangeListener {
                override fun onMainActionSelected(): Boolean {
                    addPasswordClick?.invoke()
                    return false
                }

                override fun onToggleChanged(isOpen: Boolean) {
                    // not interested
                }
            })
        }

    /**
     * FAB with two action items: add resource & add folders.
     */
    private fun foldersSpeedDial(context: Context, overlay: SpeedDialOverlayLayout) =
        mainFab(context, overlay).apply {
            addSpeedDialItem(R.id.speedDialViewAddPasswordId, R.drawable.ic_key, R.string.home_speed_dial_add_password)
            addSpeedDialItem(R.id.speedDialViewAddFolderId, R.drawable.ic_folder, R.string.home_speed_dial_add_folder)

            setOnChangeListener(null)
            setOnActionSelectedListener(SpeedDialView.OnActionSelectedListener { actionItem ->
                return@OnActionSelectedListener when (actionItem.id) {
                    R.id.speedDialViewAddPasswordId -> {
                        addPasswordClick?.invoke()
                        close()
                        true
                    }
                    R.id.speedDialViewAddFolderId -> {
                        addFolderClick?.invoke()
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
                .setFabImageTintColor(ResourcesCompat.getColor(context.resources, R.color.icon_tint, context.theme))
                .setLabel(context.getString(labelId))
                .create()
        )
    }
}
