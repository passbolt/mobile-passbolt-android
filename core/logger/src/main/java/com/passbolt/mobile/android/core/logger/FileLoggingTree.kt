package com.passbolt.mobile.android.core.logger

import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date

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
class FileLoggingTree : Timber.Tree() {
    private lateinit var logFile: File

    fun initialize(logFilePath: String) {
        logFile = File(logFilePath)
    }

    override fun log(
        priority: Int,
        tag: String?,
        message: String,
        throwable: Throwable?,
    ) {
        val logTime = timeFormat.format(Date())
        FileOutputStream(logFile, true).use {
            PrintWriter(it.writer()).use { printWriter ->
                printWriter.appendLine("$logTime $message")
                throwable?.printStackTrace(printWriter)
            }
        }
    }

    companion object {
        private val timeFormat = SimpleDateFormat.getTimeInstance()
    }
}
