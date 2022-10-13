package com.passbolt.mobile.android.core.logger

import android.content.Context
import com.passbolt.mobile.android.core.envinfo.EnvInfoProvider
import timber.log.Timber
import java.io.File
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.io.path.absolutePathString
import kotlin.streams.toList

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
    private val envInfoProvider: EnvInfoProvider
) {

    /**
     * Initializes the log file. If no file exists a new one is created. If a log file exists, expiry time is checked -
     * if still valid the current log file is returned, if not the current log file is deleted and a new one is created.
     *
     * @return Log file path
     */
    fun initializeLogFile(): String {
        val directory = logFileDirectory().apply { mkdir() }
        val directoryFiles = Files.list(directory.toPath()).toList()

        return if (directoryFiles.isEmpty()) {
            createNewLogFile(directory)
        } else {
            val logFilePath = directoryFiles.first().toAbsolutePath().toString()
            val logFileName = directoryFiles.first().fileName.toString()
            return try {
                val logFileCreationTime = LocalDateTime.ofEpochSecond(logFileName.toLong(), 0, ZoneOffset.UTC)
                if (logFileCreationTime.until(LocalDateTime.now(), ChronoUnit.HOURS) > LOG_FILE_EXPIRY_HRS) {
                    File(logFileName).delete()
                    createNewLogFile(directory)
                } else {
                    logFilePath
                }
            } catch (exception: Exception) {
                // delete files with the old log format
                Timber.d("Deleting legacy log file")
                File(logFilePath).delete()
                createNewLogFile(directory)
            }
        }
    }

    fun logFilePath() =
        Files.list(File(appContext.filesDir, LOG_DIR_NAME).toPath())
            .toList()
            .firstOrNull()
            ?.absolutePathString()

    private fun logFileDirectory() =
        File(appContext.filesDir, LOG_DIR_NAME)

    private fun createNewLogFile(directory: File) =
        File(directory, LocalDateTime.now().toEpochSecond(ZoneOffset.UTC).toString()).apply {
            if (!exists()) {
                createNewFile()
                writeText(prepareInfoHeader())
            }
        }.absolutePath

    private fun prepareInfoHeader(): String {
        val envInfo = envInfoProvider.provideEnvInfo()
        return listOf(
            "Device: %s".format(envInfo.deviceName),
            "Android %s".format(envInfo.osName),
            "Passbolt %s".format(envInfo.appName),
            System.lineSeparator()
        )
            .joinToString(separator = System.lineSeparator())
    }

    companion object {
        private const val LOG_DIR_NAME = "logs"
        private const val LOG_FILE_EXPIRY_HRS = 24
    }
}
