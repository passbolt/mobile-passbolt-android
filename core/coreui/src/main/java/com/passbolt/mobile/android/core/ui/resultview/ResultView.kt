package com.passbolt.mobile.android.core.ui.resultview

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.use
import com.passbolt.mobile.android.common.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.ui.R
import com.passbolt.mobile.android.core.ui.databinding.ViewResultBinding

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
class ResultView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle) {

    private val binding = ViewResultBinding.inflate(LayoutInflater.from(context), this)

    init {
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.ResultView)
        fillAttributes(attributes)
    }

    fun setDescription(text: String) {
        binding.descriptionLabel.text = text
    }

    fun setTitle(text: String) {
        binding.titleLabel.text = text
    }

    fun setButtonLabel(text: String) {
        binding.button.text = text
    }

    fun setButtonAction(action: () -> Unit) {
        binding.button.setDebouncingOnClick { action.invoke() }
    }

    fun setIcon(@DrawableRes icon: Int) {
        binding.icon.setImageResource(icon)
    }

    private fun fillAttributes(attributes: TypedArray) {
        with(binding) {
            attributes.use {
                titleLabel.text = it.getString(R.styleable.ResultView_resultTitle)
                descriptionLabel.text = it.getString(R.styleable.ResultView_resultDescription)
                icon.setImageDrawable(it.getDrawable(R.styleable.ResultView_resultIcon))
            }
        }
    }
}
