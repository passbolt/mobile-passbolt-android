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

package com.passbolt.mobile.android.feature.otp.createotpmanually

import com.passbolt.mobile.android.core.mvp.authentication.BaseAuthenticatedContract
import com.passbolt.mobile.android.ui.OtpAdvancedSettingsModel
import com.passbolt.mobile.android.ui.OtpResourceModel

interface CreateOtpContract {

    interface View : BaseAuthenticatedContract.View {
        fun navigateToCreateOtpAdvancedSettings(advancedSettingsModel: OtpAdvancedSettingsModel)
        fun showLabelValidationError(maxLength: Int)
        fun showSecretValidationError(maxLength: Int)
        fun showIssuerValidationError(maxLength: Int)
        fun showProgress()
        fun showGenericError()
        fun showEncryptionError(message: String)
        fun navigateToOtpListInCreateFlow(otpCreated: Boolean)
        fun hideProgress()
        fun setValues(label: String, issuer: String, secret: String)
        fun setFormValues(label: String, issuer: String, secret: String)
        fun showError(message: String)
        fun navigateToOtpListInUpdateFlow(otpUpdated: Boolean)
        fun setupEditUi()
        fun setupCreateUi()
        fun navigateToResourcePicker(suggestion: String)
    }

    interface Presenter : BaseAuthenticatedContract.Presenter<View> {
        fun advancedSettingsClick()
        fun mainButtonClick()
        fun otpSettingsModified(algorithm: String, period: Long, digits: Int)
        fun totpLabelChanged(label: String)
        fun totpSecretChanged(secret: String)
        fun totpIssuerChanged(issuer: String)
        fun argsRetrieved(editedOtpData: OtpResourceModel?)
        fun linkToResourceClick()
    }
}
