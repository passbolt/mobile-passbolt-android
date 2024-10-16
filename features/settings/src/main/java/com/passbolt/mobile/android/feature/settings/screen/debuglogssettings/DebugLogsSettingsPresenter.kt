package com.passbolt.mobile.android.feature.settings.screen.debuglogssettings

import com.passbolt.mobile.android.core.logger.FileLoggingTree
import com.passbolt.mobile.android.core.preferences.usecase.GetGlobalPreferencesUseCase
import com.passbolt.mobile.android.core.preferences.usecase.UpdateGlobalPreferencesUseCase
import timber.log.Timber

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

class DebugLogsSettingsPresenter(
    private val updateGlobalPreferencesUseCase: UpdateGlobalPreferencesUseCase,
    private val getGlobalPreferencesUseCase: GetGlobalPreferencesUseCase,
    private val fileLoggingTree: FileLoggingTree
) : DebugLogsSettingsContract.Presenter {

    override var view: DebugLogsSettingsContract.View? = null

    override fun attach(view: DebugLogsSettingsContract.View) {
        super.attach(view)
        logSettingChanged(getGlobalPreferencesUseCase.execute(Unit).areDebugLogsEnabled)
    }

    override fun enableDebugLogsChanged(areLogsEnabled: Boolean) {
        logSettingChanged(areLogsEnabled)
    }

    override fun logsClick() {
        view?.navigateToLogs()
    }

    override fun visitHelpClick() {
        view?.openHelpWebsite()
    }

    private fun logSettingChanged(areLogsEnabled: Boolean) {
        updateGlobalPreferencesUseCase.execute(UpdateGlobalPreferencesUseCase.Input(areLogsEnabled))
        if (areLogsEnabled) {
            view?.apply {
                setEnableLogsSwitchOn()
                enableAccessLogs()
                if (!Timber.forest().contains(fileLoggingTree)) {
                    Timber.plant(fileLoggingTree)
                }
            }
        } else {
            view?.apply {
                setEnableLogsSwitchOff()
                disableAccessLogs()
                if (Timber.forest().contains(fileLoggingTree)) {
                    Timber.uproot(fileLoggingTree)
                }
            }
        }
    }
}
