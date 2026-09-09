package com.passbolt.mobile.android.feature.settings.screen.appsettings.offlinemode

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf

internal fun Module.offlineModeSettingsModule() {
    viewModelOf(::OfflineModeSettingsViewModel)
}
