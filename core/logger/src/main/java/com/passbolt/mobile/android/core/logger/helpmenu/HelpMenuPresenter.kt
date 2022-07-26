package com.passbolt.mobile.android.core.logger.helpmenu

import com.passbolt.mobile.android.core.logger.FileLoggingTree
import com.passbolt.mobile.android.storage.usecase.preferences.GetGlobalPreferencesUseCase
import com.passbolt.mobile.android.storage.usecase.preferences.SaveGlobalPreferencesUseCase
import timber.log.Timber

class HelpMenuPresenter(
    private val getGlobalPreferencesUseCase: GetGlobalPreferencesUseCase,
    private val saveGlobalPreferencesUseCase: SaveGlobalPreferencesUseCase,
    private val fileLoggingTree: FileLoggingTree
) : HelpMenuContract.Presenter {

    override var view: HelpMenuContract.View? = null

    override fun argsRetrieved(scanQrCodesHelpVisible: Boolean) {
        if (scanQrCodesHelpVisible) {
            view?.showScanQrCodesHelp()
        }
        logsSettingChanged(getGlobalPreferencesUseCase.execute(Unit).areDebugLogsEnabled)
    }

    override fun logsSettingChanged(areLogsEnabled: Boolean) {
        saveGlobalPreferencesUseCase.execute(SaveGlobalPreferencesUseCase.Input(areLogsEnabled))
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
