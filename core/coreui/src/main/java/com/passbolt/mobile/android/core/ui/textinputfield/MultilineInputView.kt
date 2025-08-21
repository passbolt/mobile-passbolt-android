package com.passbolt.mobile.android.core.ui.textinputfield

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.view.Gravity
import com.passbolt.mobile.android.core.extension.px
import com.passbolt.mobile.android.core.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.extension.visible
import com.passbolt.mobile.android.core.ui.R
import com.skydoves.balloon.ArrowOrientation
import com.skydoves.balloon.createBalloon
import com.skydoves.balloon.showAlignLeft
import com.passbolt.mobile.android.core.localization.R as LocalizationR

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
class MultilineInputView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0,
    ) : TextInputView(context, attrs, defStyle) {
        init {
            with(binding.textLayout) {
                minLines = MIN_LINES
                editText?.apply {
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    gravity = Gravity.TOP
                }
            }
        }

        // TODO confirm if lock will be entirely removed
        @Suppress("Unused")
        fun addLockIcon() {
            with(binding.icon) {
                setImageResource(R.drawable.ic_lock)
                setDebouncingOnClick { showIconTooltip() }
                visible()
            }
        }

        fun updateLockIconVisibility(isSecret: Boolean) {
            if (isSecret) {
                binding.icon.setImageResource(R.drawable.ic_lock)
            } else {
                binding.icon.setImageResource(R.drawable.ic_lock_open)
            }
        }

        private fun showIconTooltip() {
            binding.icon.showAlignLeft(
                createBalloon(context) {
                    setArrowOrientation(ArrowOrientation.END)
                    setTextResource(LocalizationR.string.password_description_encrypted)
                    paddingBottom = TOOLTIP_PADDING
                    paddingLeft = TOOLTIP_PADDING
                    paddingTop = TOOLTIP_PADDING
                    paddingRight = TOOLTIP_PADDING
                },
                TOOLTIP_ALIGN_PADDING,
            )
        }

        companion object {
            private const val MIN_LINES = 3
            private val TOOLTIP_PADDING = 16.px
            private val TOOLTIP_ALIGN_PADDING = (-8).px
        }
    }
