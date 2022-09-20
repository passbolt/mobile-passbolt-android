package com.passbolt.mobile.android.core.ui.toolbar.progresstoolbar

import android.animation.LayoutTransition
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toolbar
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.view.updatePadding
import com.passbolt.mobile.android.common.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.extension.selectableBackgroundBorderlessResourceId
import com.passbolt.mobile.android.core.ui.R

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

class ProgressToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : Toolbar(context, attrs, defStyle) {

    private val rootLayout: LinearLayout
    private val progressBar: ProgressBar
    private val progressBarHorizontalPadding by lazy { resources.getDimension(R.dimen.dp_16).toInt() }

    init {
        setBackgroundColor(ContextCompat.getColor(context, R.color.background))
        setNavigationIcon(R.drawable.ic_back)
        layoutTransition = LayoutTransition()
        rootLayout = createRootLayout()
        progressBar = createProgressBar()
        rootLayout.addView(progressBar)
        addView(rootLayout)
    }

    private fun createRootLayout() = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
    }

    private fun createProgressBar() =
        ProgressBar(context, null, android.R.style.Widget_ProgressBar_Horizontal).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                resources.getDimension(R.dimen.dp_8).toInt(),
                1f
            )
            updatePadding(left = progressBarHorizontalPadding, right = progressBarHorizontalPadding)
            isIndeterminate = false
            progressDrawable = ContextCompat.getDrawable(context, R.drawable.progress_toolbar_progress_drawable)
        }

    fun initializeProgressBar(minProgress: Int, maxProgress: Int) {
        progressBar.min = minProgress * ANIMATION_VALUE_MULTIPLIER
        progressBar.max = maxProgress * ANIMATION_VALUE_MULTIPLIER
    }

    fun setCurrentProgress(progress: Int) {
        ObjectAnimator.ofInt(
            progressBar,
            PROGRESS_PROPERTY,
            progressBar.progress,
            progress * ANIMATION_VALUE_MULTIPLIER
        ).apply {
            setAutoCancel(true)
            duration = PROGRESS_ANIMATION_DURATION_MILLIS
            interpolator = DecelerateInterpolator()
        }
            .start()
    }

    fun addIconEnd(@DrawableRes iconRes: Int, clickListener: () -> Unit) {
        ImageView(context).apply {
            setImageResource(iconRes)
            setBackgroundResource(context.selectableBackgroundBorderlessResourceId())
            setDebouncingOnClick { clickListener.invoke() }
            contentDescription = context.getString(R.string.help_button_description)
        }.let {
            rootLayout.addView(
                it,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }
    }

    private companion object {
        private const val PROGRESS_ANIMATION_DURATION_MILLIS = 250L
        private const val ANIMATION_VALUE_MULTIPLIER = 1_000
        private const val PROGRESS_PROPERTY = "progress"
    }
}
