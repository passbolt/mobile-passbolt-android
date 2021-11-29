package com.passbolt.mobile.android.feature.resources.update

import android.content.Context
import android.view.ViewGroup
import android.widget.LinearLayout
import com.passbolt.mobile.android.common.px
import com.passbolt.mobile.android.core.ui.textinputfield.MultilineInputView
import com.passbolt.mobile.android.core.ui.textinputfield.PasswordGenerateInputView
import com.passbolt.mobile.android.core.ui.textinputfield.TextInputView
import com.passbolt.mobile.android.feature.resources.R

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
class ViewProvider {

    fun getTextInput(name: String, context: Context, isSecret: Boolean): Pair<TextInputView, ViewGroup.LayoutParams> =
        Pair(TextInputView(context).apply {
            if (isSecret) enableSecretInput()
            this.title = name
            setDefaultHint(this.title)
        }, getDefaultParams())

    fun getPasswordWithGeneratorInput(
        context: Context
    ): Pair<PasswordGenerateInputView, ViewGroup.LayoutParams> =
        Pair(PasswordGenerateInputView(context).apply {
            this.title = context.resources.getString(R.string.resource_update_password)
            this.hint = context.resources.getString(R.string.resource_update_password_hint)
        }, getDefaultParams())

    fun getDescriptionInput(
        context: Context,
        isSecret: Boolean
    ): Pair<MultilineInputView, ViewGroup.LayoutParams> =
        Pair(MultilineInputView(context).apply {
            updateLockIconVisibility(isSecret)
            this.title = context.resources.getString(R.string.resource_update_description)
            setDefaultHint(this.title)
        }, getDefaultParams())

    private fun getDefaultParams() =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setMargins(16.px, 16.px, 16.px, 0)
        }
}
