package com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.advanced

import com.passbolt.mobile.android.common.validation.StringIsAPositiveIntegerNumber
import com.passbolt.mobile.android.common.validation.validation
import com.passbolt.mobile.android.ui.Mode
import com.passbolt.mobile.android.ui.Mode.CREATE
import com.passbolt.mobile.android.ui.Mode.UPDATE
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

class TotpAdvancedSettingsFormPresenter : TotpAdvancedSettingsFormContract.Presenter {

    override var view: TotpAdvancedSettingsFormContract.View? = null
    private lateinit var totpModel: TotpUiModel

    override fun argsRetrieved(mode: Mode, totpUiModel: TotpUiModel) {
        this.totpModel = totpUiModel

        when (mode) {
            CREATE -> view?.showCreateTitle()
            UPDATE -> throw NotImplementedError() // TODO
        }

        view?.apply {
            showExpiry(totpModel.expiry)
            showLength(totpModel.length)
            showAlgorithm(totpModel.algorithm)
        }
    }

    override fun totpPeriodChanged(period: String) {
        totpModel = totpModel.copy(expiry = period)
    }

    override fun totpDigitsChanged(digits: String) {
        totpModel = totpModel.copy(length = digits)
    }

    override fun totpAlgorithmChanged(algorithm: String) {
        totpModel = totpModel.copy(algorithm = algorithm)
    }

    override fun applyClick() {
        validation {
            of(totpModel.expiry) {
                withRules(StringIsAPositiveIntegerNumber) {
                    onInvalid { view?.showTotpPeriodError() }
                }
            }
            // digits and algorithm are selected from predefined list (no validation needed)
            onValid {
                view?.goBackWithResult(totpModel)
            }
        }
    }
}
