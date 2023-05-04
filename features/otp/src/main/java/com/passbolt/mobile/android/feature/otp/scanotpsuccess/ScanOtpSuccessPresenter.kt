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

package com.passbolt.mobile.android.feature.otp.scanotpsuccess

import com.passbolt.mobile.android.core.mvp.authentication.BaseAuthenticatedPresenter
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.resources.usecase.CreateTotpResourceUseCase
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.Companion.SLUG_TOTP
import com.passbolt.mobile.android.database.impl.resourcetypes.GetResourceTypeWithFieldsBySlugUseCase
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import com.passbolt.mobile.android.feature.otp.scanotp.parser.OtpParseResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ScanOtpSuccessPresenter(
    private val createTotpResourceUseCase: CreateTotpResourceUseCase,
    private val getResourceTypeWithFieldsBySlugUseCase: GetResourceTypeWithFieldsBySlugUseCase,
    coroutineLaunchContext: CoroutineLaunchContext
) : BaseAuthenticatedPresenter<ScanOtpSuccessContract.View>(coroutineLaunchContext),
    ScanOtpSuccessContract.Presenter {

    override var view: ScanOtpSuccessContract.View? = null
    private lateinit var scannedTotp: OtpParseResult.OtpQr.TotpQr
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    override fun argsRetrieved(scannedTotp: OtpParseResult.OtpQr.TotpQr) {
        this.scannedTotp = scannedTotp
    }

    override fun createStandaloneOtpClick() {
        scope.launch {
            view?.showProgress()
            when (val result = runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
                createTotpResourceUseCase.execute(createResourceInput())
            }) {
                is CreateTotpResourceUseCase.Output.Failure<*> -> view?.showGenericError()
                is CreateTotpResourceUseCase.Output.OpenPgpError -> view?.showEncryptionError(result.message)
                is CreateTotpResourceUseCase.Output.PasswordExpired -> {
                    /* will not happen in BaseAuthenticatedPresenter */
                }
                is CreateTotpResourceUseCase.Output.Success -> {
                    view?.navigateToOtpList(otpCreated = true)
                }
            }
            view?.hideProgress()
        }
    }

    private suspend fun createResourceInput(): CreateTotpResourceUseCase.Input {
        val totpResourceType = getResourceTypeWithFieldsBySlugUseCase.execute(
            GetResourceTypeWithFieldsBySlugUseCase.Input(SLUG_TOTP)
        )
        return CreateTotpResourceUseCase.Input(
            resourceTypeId = totpResourceType.resourceTypeId,
            issuer = scannedTotp.issuer,
            label = scannedTotp.label,
            period = scannedTotp.period,
            digits = scannedTotp.digits,
            algorithm = scannedTotp.algorithm.name,
            secretKey = scannedTotp.secret
        )
    }
}
