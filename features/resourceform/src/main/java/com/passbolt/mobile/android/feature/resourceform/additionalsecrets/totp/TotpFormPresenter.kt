package com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp

import com.passbolt.mobile.android.common.validation.StringIsBase32
import com.passbolt.mobile.android.common.validation.StringNotBlank
import com.passbolt.mobile.android.common.validation.validation
import com.passbolt.mobile.android.ui.OtpParseResult
import com.passbolt.mobile.android.ui.ResourceFormMode
import com.passbolt.mobile.android.ui.TotpUiModel

/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021 Passbolt SA
 *
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 *
 * The name "Passbolt" is a registered trademark of Passbolt SA, and Passbolt SA hereby declines to grant a trademark
 * license to "Passbolt" pursuant to the GNU Affero General Public License version 3 Section 7(e), without a separate
 * agreement with Passbolt SA.
 *
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Passbolt SA (https://www.passbolt.com)
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://www.passbolt.com Passbolt (tm)
 * @since v1.0
 */

class TotpFormPresenter : TotpFormContract.Presenter {
    override var view: TotpFormContract.View? = null

    private lateinit var totpUiModel: TotpUiModel

    override fun argsRetrieved(
        mode: ResourceFormMode,
        totpUiModel: TotpUiModel,
    ) {
        this.totpUiModel = totpUiModel

        when (mode) {
            is ResourceFormMode.Create -> view?.showCreateTitle()
            is ResourceFormMode.Edit -> view?.showEditTitle(mode.resourceName)
        }

        view?.showSecret(totpUiModel.secret)
        view?.showUrl(totpUiModel.issuer)
    }

    override fun totpSecretChanged(secret: String) {
        totpUiModel = totpUiModel.copy(secret = secret)
    }

    override fun totpUrlChanged(url: String) {
        totpUiModel = totpUiModel.copy(issuer = url)
    }

    override fun moreSettingsClick() {
        view?.navigateToTotpAdvancedSettingsForm(totpUiModel)
    }

    override fun totpAdvancedSettingsChanged(totpModel: TotpUiModel?) {
        totpModel?.let {
            totpUiModel =
                it.copy(
                    expiry = totpModel.expiry,
                    length = totpModel.length,
                    algorithm = totpModel.algorithm,
                )
        }
    }

    override fun totpScanned(
        isManualCreationChosen: Boolean,
        totpQr: OtpParseResult.OtpQr.TotpQr?,
    ) {
        // just stay on totp screen and allow manual input
        if (isManualCreationChosen) return

        totpQr?.let {
            totpUiModel =
                TotpUiModel(
                    issuer = it.issuer.orEmpty(),
                    secret = it.secret,
                    algorithm = it.algorithm.name,
                    length = it.digits.toString(),
                    expiry = it.period.toString(),
                )
            view?.showSecret(it.secret)
            view?.showUrl(it.issuer.orEmpty())
        }
    }

    override fun removeTotpClick() {
        view?.goBackWithResult(null)
    }

    override fun applyClick() {
        validation {
            of(totpUiModel.secret) {
                withRules(StringNotBlank) {
                    onInvalid {
                        view?.showSecretMustNotBeEmpty()
                    }
                }
                withRules(StringIsBase32) {
                    onInvalid {
                        view?.showSecretMustBeBase32()
                    }
                }
            }
            onValid { view?.goBackWithResult(totpUiModel) }
        }
    }
}
