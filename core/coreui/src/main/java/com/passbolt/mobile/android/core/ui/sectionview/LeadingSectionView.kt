package com.passbolt.mobile.android.core.ui.sectionview

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.res.use
import com.passbolt.mobile.android.core.ui.R
import com.passbolt.mobile.android.core.ui.databinding.ViewLeadingSectionBinding

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

class LeadingSectionView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0,
    ) : LinearLayout(context, attrs, defStyle) {
        var title: String = ""
            set(value) {
                field = value
                binding.title.text = value
            }

        private val binding = ViewLeadingSectionBinding.inflate(LayoutInflater.from(context), this)

        val backgroundContainer: ViewGroup
            get() = binding.backgroundContainer

        init {
            orientation = VERTICAL
            parseAttributes(attrs)
        }

        private fun parseAttributes(attrs: AttributeSet?) {
            attrs?.let {
                context.obtainStyledAttributes(attrs, R.styleable.LeadingSectionView, 0, 0).use {
                    title = it.getString(R.styleable.LeadingSectionView_sectionTitle).orEmpty()
                }
            }
        }
    }
