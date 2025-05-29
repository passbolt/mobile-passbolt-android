package com.passbolt.mobile.android.core.logger

import android.content.Context
import com.passbolt.mobile.android.core.envinfo.EnvInfoProvider
import com.passbolt.mobile.android.core.preferences.usecase.GetGlobalPreferencesUseCase
import com.passbolt.mobile.android.core.preferences.usecase.UpdateGlobalPreferencesUseCase
import java.io.File
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

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
class LogFilesManager(
    private val appContext: Context,
    private val envInfoProvider: EnvInfoProvider,
    private val getGlobalPreferencesUseCase: GetGlobalPreferencesUseCase,
    private val updateGlobalPreferencesUseCase: UpdateGlobalPreferencesUseCase,
) {
    /**
     * Initializes the log file. If no file exists a new one is created. If a log file exists, expiry time is checked -
     * if still valid the current log file is returned, if not the current log file is deleted and a new one is created.
     *
     * @return Log file path
     */
    fun initializeLogFile(): String {
        val logFileCreationDateTime = getGlobalPreferencesUseCase.execute(Unit).debugLogFileCreationDateTime
        return if (logFileCreationDateTime == null) {
            // log creation date is null - log file has never been created before
            logFileDirectory().listFiles()?.forEach { it.delete() }
            createNewLogFile()
        } else {
            if (logFileCreationDateTime.until(LocalDateTime.now(), ChronoUnit.HOURS) > LOG_FILE_EXPIRY_HRS) {
                // logs are expired - creation date is before { now - logs_expiry_time }
                logFileDirectory().listFiles()?.forEach { it.delete() }
                createNewLogFile()
            } else {
                // log file is still valid - creation date is in range of (now - logs_expiry_time , now)
                logFilePath()
            }
        }
    }

    fun logFilePath(): String =
        File(logFileDirectory(), LOG_FILE_NAME)
            .absolutePath

    private fun logFileDirectory() = File(appContext.filesDir, LOG_DIR_NAME)

    private fun createNewLogFile(): String {
        val directory = logFileDirectory().apply { mkdir() }
        val logFilePath =
            File(directory, LOG_FILE_NAME)
                .apply {
                    createNewFile()
                    writeText(prepareInfoHeader())
                }.absolutePath
        updateGlobalPreferencesUseCase.execute(
            UpdateGlobalPreferencesUseCase.Input(
                debugLogFileCreationDateTime = LocalDateTime.now(),
            ),
        )
        return logFilePath
    }

    private fun prepareInfoHeader(): String {
        val envInfo = envInfoProvider.provideEnvInfo()
        return listOf(
            "Device: %s".format(envInfo.deviceName),
            "Android %s".format(envInfo.osName),
            "Passbolt %s".format(envInfo.appName),
            System.lineSeparator(),
        ).joinToString(separator = System.lineSeparator())
    }

    companion object {
        private const val LOG_DIR_NAME = "logs"
        private const val LOG_FILE_NAME = "logs_24hrs"

        private const val LOG_FILE_EXPIRY_HRS = 24
    }
}
