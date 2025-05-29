package com.passbolt.mobile.android.helpmenu

import com.passbolt.mobile.android.core.logger.FileLoggingTree
import com.passbolt.mobile.android.core.preferences.usecase.GetGlobalPreferencesUseCase
import com.passbolt.mobile.android.core.preferences.usecase.UpdateGlobalPreferencesUseCase
import com.passbolt.mobile.android.ui.HelpMenuModel
import timber.log.Timber

class HelpMenuPresenter(
    private val getGlobalPreferencesUseCase: GetGlobalPreferencesUseCase,
    private val updateGlobalPreferencesUseCase: UpdateGlobalPreferencesUseCase,
    private val fileLoggingTree: FileLoggingTree,
) : HelpMenuContract.Presenter {
    override var view: HelpMenuContract.View? = null

    override fun argsRetrieved(helpMenuModel: HelpMenuModel) {
        if (helpMenuModel.shouldShowShowQrCodesHelp) {
            view?.showScanQrCodesHelp()
        }
        if (helpMenuModel.shouldShowImportProfile) {
            view?.showImportProfileHelp()
        }
        if (helpMenuModel.shouldShowImportAccountKit) {
            view?.showImportAccountKitHelp()
        }
        logsSettingChanged(getGlobalPreferencesUseCase.execute(Unit).areDebugLogsEnabled)
    }

    override fun logsSettingChanged(areLogsEnabled: Boolean) {
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
