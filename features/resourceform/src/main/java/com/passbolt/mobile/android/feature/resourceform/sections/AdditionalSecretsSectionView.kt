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
import com.passbolt.mobile.android.feature.resourceform.databinding.ViewAdditionalSecretsSectionBinding
import com.passbolt.mobile.android.ui.ResourceFormUiModel
import com.passbolt.mobile.android.ui.ResourceFormUiModel.Secret.CUSTOM_FIELDS
import com.passbolt.mobile.android.ui.ResourceFormUiModel.Secret.NOTE
import com.passbolt.mobile.android.ui.ResourceFormUiModel.Secret.PASSWORD
import com.passbolt.mobile.android.ui.ResourceFormUiModel.Secret.TOTP

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

class AdditionalSecretsSectionView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0,
    ) : LinearLayout(context, attrs, defStyle) {
        var additionalPasswordClick: (() -> Unit)? = null
            set(value) {
                field = value
                value?.let {
                    passwordAdditionalSecretSection.setDebouncingOnClick(action = value)
                }
            }

        var additionalTotpClick: (() -> Unit)? = null
            set(value) {
                field = value
                value?.let {
                    totpAdditionalSecretSection.setDebouncingOnClick(action = value)
                }
            }

        var additionalNoteClick: (() -> Unit)? = null
            set(value) {
                field = value
                value?.let {
                    noteAdditionalSecretSection.setDebouncingOnClick(action = value)
                }
            }

        var customFieldsClick: (() -> Unit)? = null
            set(value) {
                field = value
                value?.let {
                    customFieldsAdditionalSecretSection.setDebouncingOnClick(action = value)
                }
            }

        private val binding = ViewAdditionalSecretsSectionBinding.inflate(LayoutInflater.from(context), this)

        private val passwordAdditionalSecretSection: OpenableSettingView
            get() = binding.additionalSecretsSectionView.findViewById(R.id.additionalPassword)

        private val totpAdditionalSecretSection: OpenableSettingView
            get() = binding.additionalSecretsSectionView.findViewById(R.id.additionalTotp)

        private val noteAdditionalSecretSection: OpenableSettingView
            get() = binding.additionalSecretsSectionView.findViewById(R.id.additionalNote)

        private val customFieldsAdditionalSecretSection: OpenableSettingView
            get() = binding.additionalSecretsSectionView.findViewById(R.id.customFields)

        init {
            orientation = VERTICAL
            LayoutInflater.from(context).inflate(
                R.layout.view_additional_secrets_fields,
                binding.additionalSecretsSectionView.backgroundContainer,
                true,
            )
        }

        fun setUp(fields: List<ResourceFormUiModel.Secret>) {
            if (fields.isEmpty()) {
                gone()
            } else {
                passwordAdditionalSecretSection.isVisible = fields.contains(PASSWORD)
                totpAdditionalSecretSection.isVisible = fields.contains(TOTP)
                noteAdditionalSecretSection.isVisible = fields.contains(NOTE)
                customFieldsAdditionalSecretSection.isVisible = fields.contains(CUSTOM_FIELDS)
            }
        }
    }
