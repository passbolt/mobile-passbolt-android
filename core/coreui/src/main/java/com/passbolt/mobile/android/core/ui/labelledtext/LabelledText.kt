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

package com.passbolt.mobile.android.core.ui.labelledtext

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.res.use
import com.passbolt.mobile.android.core.extension.selectableBackgroundBorderlessResourceId
import com.passbolt.mobile.android.core.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.ui.R
import com.passbolt.mobile.android.core.ui.databinding.ViewLabelledTextBinding

class LabelledText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle) {

    var label = ""
        set(value) {
            field = value
            binding.label.text = value
        }
    var text = ""
        set(value) {
            field = value
            binding.text.text = value
        }
    var endActionButton: LabelledTextEndAction? = null
        set(value) {
            field = value
            setEndAction(value)
        }

    private val binding = ViewLabelledTextBinding.inflate(LayoutInflater.from(context), this)

    init {
        parseAttributes(attrs)
    }

    private fun parseAttributes(attrs: AttributeSet?) {
        attrs?.let {
            context.obtainStyledAttributes(attrs, R.styleable.LabelledText, 0, 0).use {
                label = it.getString(R.styleable.LabelledText_labelledText_label).orEmpty()
                text = it.getString(R.styleable.LabelledText_labelledText_text).orEmpty()
                if (it.getBoolean(R.styleable.LabelledText_labelledText_use_monospace_font, false)) {
                    val fontFamily = ResourcesCompat.getFont(binding.root.context, R.font.inconsolata)
                    binding.text.typeface = Typeface.create(fontFamily, MONOSPACED_FONT_WEIGHT, false)
                    binding.text.textSize = MONOSPACED_FONT_TEXT_SIZE_SP
                }
            }
        }
    }

    private fun setEndAction(model: LabelledTextEndAction?) {
        model?.let {
            ImageView(context)
                .apply {
                    setImageResource(model.icon)
                    setBackgroundResource(context.selectableBackgroundBorderlessResourceId())
                    setDebouncingOnClick { model.action.invoke() }
                    binding.actionsContainer.addView(this)
                }
        } ?: kotlin.run {
            binding.actionsContainer.removeAllViews()
        }
    }

    private companion object {
        private const val MONOSPACED_FONT_WEIGHT = 500
        private const val MONOSPACED_FONT_TEXT_SIZE_SP = 16f
    }
}
