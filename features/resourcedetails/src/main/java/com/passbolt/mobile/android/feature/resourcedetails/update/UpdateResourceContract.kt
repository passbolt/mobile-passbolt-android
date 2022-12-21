package com.passbolt.mobile.android.feature.resourcedetails.update

import com.passbolt.mobile.android.core.fulldatarefresh.base.DataRefreshViewReactiveContract
import com.passbolt.mobile.android.core.ui.textinputfield.PasswordGenerateInputView
import com.passbolt.mobile.android.feature.resourcedetails.ResourceMode
import com.passbolt.mobile.android.ui.ResourceModel

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
interface UpdateResourceContract {

    interface View : DataRefreshViewReactiveContract.View {
        fun addTextInput(
            name: String,
            isSecret: Boolean,
            uiTag: String,
            isRequired: Boolean,
            initialValue: String? = null
        )

        fun addPasswordInput(
            name: String,
            uiTag: String,
            isRequired: Boolean,
            initialPassword: String? = null,
            initialPasswordStrength: PasswordGenerateInputView.PasswordStrength
        )

        fun addDescriptionInput(
            name: String,
            isSecret: Boolean,
            uiTag: String,
            isRequired: Boolean,
            initialValue: String? = null
        )

        fun showEmptyValueError(tag: String)
        fun showTooLongError(tag: String)
        fun showPassword(tag: String, password: String?, passwordStrength: PasswordGenerateInputView.PasswordStrength)
        fun showPasswordStrength(tag: String, strength: PasswordGenerateInputView.PasswordStrength)
        fun showError()
        fun showProgress()
        fun hideProgress()
        fun showCreateButton()
        fun showEditButton()
        fun showCreateTitle()
        fun showEditTitle()
        fun closeWithCreateSuccessResult(name: String, id: String)
        fun closeWithEditSuccessResult(name: String)
        fun showEncryptionError(message: String)
        fun showShareSimulationFailure()
        fun showShareFailure()
        fun showSecretFetchFailure()
        fun showSecretEncryptFailure()
        fun showSecretDecryptFailure()
        fun showDataRefreshError()
        fun showContentNotAvailable()
        fun navigateHome()
        fun clearInputFields()
    }

    interface Presenter : DataRefreshViewReactiveContract.Presenter<View> {
        fun updateClick()
        fun passwordGenerateClick(tag: String)
        fun passwordTextChanged(tag: String, password: String)
        fun textChanged(tag: String, value: String)
        fun argsRetrieved(mode: ResourceMode, resource: ResourceModel? = null, resourceParentFolderId: String?)
    }
}
