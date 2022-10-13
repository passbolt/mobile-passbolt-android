package com.passbolt.mobile.android.initializers

import android.content.Context
import androidx.startup.Initializer
import com.passbolt.mobile.android.BuildConfig
import com.passbolt.mobile.android.core.logger.FileLoggingTree
import com.passbolt.mobile.android.core.logger.LogFilesManager
import com.passbolt.mobile.android.storage.usecase.preferences.GetGlobalPreferencesUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
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

/**
 * Initializes the logging library.
 * Apart from default logger in DEBUG mode the application also uses a logger instance
 * capable of writing logs into files. This additional logger is used only if enabled in
 * the global preferences.
 *
 * @property logFilesManager initializes and clears log files
 * @property fileLoggingTree a logger instance that stores logs in a file
 * @property getGlobalPreferencesUseCase a use case for reading global application preferences
 */
@Suppress("unused")
class TimberInitializer : Initializer<Unit>, KoinComponent {

    private val fileLoggingTree: FileLoggingTree by inject()
    private val logFilesManager: LogFilesManager by inject()
    private val getGlobalPreferencesUseCase: GetGlobalPreferencesUseCase by inject()

    override fun create(context: Context) {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        val logFilePath = logFilesManager.initializeLogFile()
        fileLoggingTree.initialize(logFilePath)
        if (getGlobalPreferencesUseCase.execute(Unit).areDebugLogsEnabled &&
            !Timber.forest().contains(fileLoggingTree)
        ) {
            Timber.plant(fileLoggingTree)
            Timber.d("File logging tree planted")
        }
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> =
        mutableListOf(KoinInitializer::class.java)
}
