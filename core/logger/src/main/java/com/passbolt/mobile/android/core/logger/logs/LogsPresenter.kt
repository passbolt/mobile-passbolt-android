package com.passbolt.mobile.android.core.logger.logs

import java.io.File

class LogsPresenter : LogsContract.Presenter {

    override var view: LogsContract.View? = null
    private lateinit var logLines: List<String>

    override fun argsRetrieved(logFilePath: String, deviceName: String, osName: String, appName: String) {
        val logFile = File(logFilePath)
        logLines = prepareInfoLines(deviceName, osName, appName) +
                if (logFile.exists()) logFile.readLines() else emptyList()
        view?.apply {
            showLogs(logLines)
            scrollLogsToPosition(logLines.size - 1)
            showShareSheet(
                logLines.joinToString(separator = System.lineSeparator())
            )
        }
    }

    private fun prepareInfoLines(deviceName: String, osName: String, appName: String) =
        listOf(
            "Device: %s".format(deviceName),
            "Android %s".format(osName),
            "Passbolt %s".format(appName),
            System.lineSeparator()
        )

    override fun shareClick() {
        view?.showShareSheet(
            logLines.joinToString(separator = System.lineSeparator())
        )
    }
}
