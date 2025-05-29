package com.passbolt.mobile.android.core.ui.menu

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.content.res.use
import androidx.core.view.children
import com.passbolt.mobile.android.core.ui.R
import com.passbolt.mobile.android.core.ui.databinding.ViewSettingBinding

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
open class SettingView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0,
    ) : LinearLayout(context, attrs, defStyle) {
        protected val binding = ViewSettingBinding.inflate(LayoutInflater.from(context), this, true)

        var icon: Drawable? = null
            set(value) {
                field = value
                with(binding.iconImage) {
                    setImageDrawable(value)
                    imageTintList = ColorStateList.valueOf(context.getColor(R.color.icon_tint))
                }
            }

        var name: String = ""
            set(value) {
                field = value
                binding.nameLabel.text = value
            }

        init {
            parseAttributes(attrs)
        }

        private fun parseAttributes(attrs: AttributeSet?) {
            attrs?.let {
                context.obtainStyledAttributes(attrs, R.styleable.SettingView, 0, 0).use {
                    icon = it.getDrawable(R.styleable.SettingView_icon)
                    name = it.getString(R.styleable.SettingView_name).orEmpty()
                }
            }
        }

        override fun setEnabled(enabled: Boolean) {
            super.setEnabled(enabled)
            binding.root.children.forEach {
                it.alpha = if (enabled) ALPHA_FULLY_VISIBLE else ALPHA_GREYED_OUT
                it.isEnabled = enabled
            }
        }

        protected companion object {
            const val ALPHA_GREYED_OUT = 0.5f
            const val ALPHA_FULLY_VISIBLE = 1f
        }
    }
