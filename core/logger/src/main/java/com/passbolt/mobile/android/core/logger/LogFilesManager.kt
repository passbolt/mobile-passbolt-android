package com.passbolt.mobile.android.core.logger

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    private val appContext: Context
) {

    fun initializeLogFile(): String {
        val directory = File(appContext.filesDir, LOG_DIR_NAME).apply { mkdir() }
        return File(directory, logFileName()).apply {
            if (!exists()) createNewFile()
        }.absolutePath
    }

    fun clearIrrelevantLogFiles(relevantLogAbsoluteFilePath: String) {
        File(appContext.filesDir, LOG_DIR_NAME)
            .listFiles { file, _ -> file.absolutePath != relevantLogAbsoluteFilePath }
            .orEmpty()
            .forEach { it.delete() }
    }

    companion object {
        const val LOG_DIR_NAME = "logs"

        private const val HOUR_PATTERN = "dd-MM-yyy HH"
        private val LOG_FILE_NAME_FORMAT = SimpleDateFormat(HOUR_PATTERN, Locale.US)

        fun logFileName() =
            LOG_FILE_NAME_FORMAT.format(Date())
    }
}
