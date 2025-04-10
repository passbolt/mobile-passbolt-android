package com.passbolt.mobile.android.feature.resourceform.main

import com.passbolt.mobile.android.core.mvp.authentication.BaseAuthenticatedContract
import com.passbolt.mobile.android.core.passwordgenerator.codepoints.Codepoint
import com.passbolt.mobile.android.feature.otp.scanotp.ScanOtpMode
import com.passbolt.mobile.android.ui.OtpParseResult
import com.passbolt.mobile.android.ui.PasswordStrength
import com.passbolt.mobile.android.ui.PasswordStrength.Empty
import com.passbolt.mobile.android.ui.PasswordUiModel
import com.passbolt.mobile.android.ui.ResourceFormMode
import com.passbolt.mobile.android.ui.ResourceFormUiModel
import com.passbolt.mobile.android.ui.TotpUiModel

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
interface ResourceFormContract {

    interface View : BaseAuthenticatedContract.View {
        fun addTotpLeadingForm(totpUiModel: TotpUiModel)
        fun addPasswordLeadingForm(
            password: String = "",
            passwordStrength: PasswordStrength = Empty,
            passwordEntropyBits: Double = 0.0
        )

        fun showCreatePasswordTitle()
        fun showCreateTotpTitle()
        fun showCreateButton()
        fun showSaveButton()
        fun showUnableToGeneratePassword(minimumEntropyBits: Int)
        fun showPassword(password: List<Codepoint>, entropy: Double, passwordStrength: PasswordStrength)
        fun showPasswordStrength(strength: PasswordStrength, entropyBits: Double)
        fun setupAdditionalSecrets(supportedAdditionalSecrets: List<ResourceFormUiModel.Secret>)
        fun setupMetadata(supportedMetadata: List<ResourceFormUiModel.Metadata>)
        fun hideAdvancedSettings()
        fun navigateToSecureNote(secureNote: String)
        fun navigateToTotp(totpUiModel: TotpUiModel)
        fun navigateToMetadataDescription(metadataDescription: String)
        fun showName(name: String)
        fun showPasswordUsername(username: String)
        fun showPasswordMainUri(mainUri: String)
        fun showTotpSecret(secret: String)
        fun showTotpIssuer(issuer: String)
        fun navigateToPassword(passwordUiModel: PasswordUiModel)
        fun showProgress()
        fun hideProgress()
        fun showGenericError()
        fun showEncryptionError(error: String)
        fun showJsonResourceSchemaValidationError()
        fun showJsonSecretSchemaValidationError()
        fun navigateBackWithCreateSuccess(name: String)
        fun navigateToScanTotp(scanMode: ScanOtpMode)
        fun showInitializationProgress()
        fun hideInitializationProgress()
        fun showEditResourceInitializationError()
        fun navigateBack()
        fun navigateBackWithEditSuccess(name: String)
    }

    interface Presenter : BaseAuthenticatedContract.Presenter<View> {
        fun argsRetrieved(mode: ResourceFormMode)
        fun createResourceClick()
        fun updateResourceClick()
        fun passwordGenerateClick()
        fun passwordTextChanged(password: String)
        fun advancedSettingsClick()
        fun nameTextChanged(name: String)
        fun passwordMainUriTextChanged(mainUri: String)
        fun passwordUsernameTextChanged(username: String)
        fun additionalSecureNoteClick()
        fun additionalTotpClick()
        fun metadataDescriptionClick()
        fun secureNoteChanged(secureNote: String?)
        fun metadataDescriptionChanged(metadataDescription: String?)
        fun totpChanged(totpUiModel: TotpUiModel?)
        fun totpSecretChanged(totpSecret: String)
        fun totpUrlChanged(url: String)
        fun totpAdvancedSettingsChanged(totpAdvancedSettings: TotpUiModel?)
        fun additionalPasswordClick()
        fun passwordChanged(passwordUiModel: PasswordUiModel?)
        fun totpScanned(isManualCreationChosen: Boolean, scannedTotp: OtpParseResult.OtpQr.TotpQr?)
    }
}
