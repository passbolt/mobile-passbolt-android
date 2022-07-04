package com.passbolt.mobile.android.feature.setup.summary

import com.passbolt.mobile.android.common.UuidProvider
import com.passbolt.mobile.android.storage.usecase.account.SaveAccountUseCase
import com.passbolt.mobile.android.storage.usecase.database.SaveResourcesDatabasePassphraseUseCase
import com.passbolt.mobile.android.storage.usecase.input.UserIdInput

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
class SummaryPresenter(
    private val saveAccountUseCase: SaveAccountUseCase,
    private val saveResourcesDatabasePassphraseUseCase: SaveResourcesDatabasePassphraseUseCase,
    private val uuidProvider: UuidProvider
) : SummaryContract.Presenter {

    override var view: SummaryContract.View? = null
    private lateinit var status: ResultStatus

    override fun start(status: ResultStatus) {
        this.status = status
        setupView(status)
    }

    private fun setupView(status: ResultStatus) {
        view?.apply {
            setTitle(status.title)
            setButtonLabel(status.buttonText)
            if (status is ResultStatus.Failure) {
                setDescription(status.message)
                view?.showHelpButton()
            }
            setIcon(status.icon)
        }
    }

    override fun buttonClick() {
        when (val currentStatus = status) {
            is ResultStatus.AlreadyLinked -> view?.navigateToManageAccounts()
            is ResultStatus.Success -> view?.navigateToSignIn(currentStatus.userId)
            is ResultStatus.Failure -> view?.navigateToScanQr()
            is ResultStatus.HttpNotSupported -> view?.navigateToScanQr()
            is ResultStatus.NoNetwork -> view?.navigateToScanQr()
        }
    }

    override fun authenticationSucceeded() {
        when (val currentStatus = status) {
            is ResultStatus.Success -> saveAccountUseCase.execute(UserIdInput(currentStatus.userId))
        }
        val pass = uuidProvider.get()
        saveResourcesDatabasePassphraseUseCase.execute(SaveResourcesDatabasePassphraseUseCase.Input(pass))
        view?.navigateToFingerprintSetup()
    }

    override fun backClick() {
        if (status is ResultStatus.Success) {
            view?.showLeaveConfirmationDialog()
        } else {
            view?.navigateToScanQr()
        }
    }

    override fun leaveConfirmationClick() {
        view?.navigateToStart()
    }

    override fun helpClick() {
        view?.showHelpMenu()
    }
}
