package com.passbolt.mobile.android.logs

import java.io.File

class LogsPresenter : LogsContract.Presenter {
    override var view: LogsContract.View? = null
    private lateinit var logLines: List<String>
    private lateinit var logFile: File

    override fun argsRetrieved(logFilePath: String?) {
        logFile = File(requireNotNull(logFilePath) { "Log file not initialized" })
        logLines = if (logFile.exists()) logFile.readLines() else emptyList()

        view?.apply {
            showLogs(logLines)
            scrollLogsToPosition(logLines.size - 1)
            showShareSheet(logFile)
        }
    }

    override fun shareClick() {
        view?.showShareSheet(logFile)
    }
}
