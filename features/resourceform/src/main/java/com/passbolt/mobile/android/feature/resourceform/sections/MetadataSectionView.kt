package com.passbolt.mobile.android.feature.resourceform.sections

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.passbolt.mobile.android.core.extension.gone
import com.passbolt.mobile.android.core.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.ui.menu.OpenableSettingView
import com.passbolt.mobile.android.feature.resourceform.R
import com.passbolt.mobile.android.feature.resourceform.databinding.ViewMetadataSectionBinding
import com.passbolt.mobile.android.ui.ResourceFormUiModel
import com.passbolt.mobile.android.ui.ResourceFormUiModel.Metadata.DESCRIPTION

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

class MetadataSectionView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0,
    ) : LinearLayout(context, attrs, defStyle) {
        var descriptionClick: (() -> Unit)? = null
            set(value) {
                field = value
                value?.let {
                    descriptionSection.setDebouncingOnClick(action = value)
                }
            }

        private val binding = ViewMetadataSectionBinding.inflate(LayoutInflater.from(context), this)

        private val descriptionSection: OpenableSettingView
            get() = binding.metadataSectionView.backgroundContainer.findViewById(R.id.description)

        init {
            orientation = VERTICAL
            LayoutInflater.from(context).inflate(
                R.layout.view_metadata_fields,
                binding.metadataSectionView.backgroundContainer,
                true,
            )
        }

        fun setUp(fields: List<ResourceFormUiModel.Metadata>) {
            if (fields.isEmpty()) {
                gone()
            } else {
                descriptionSection.isVisible = fields.contains(DESCRIPTION)
            }
        }
    }
