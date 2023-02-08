package com.passbolt.mobile.android.feature.settings.screen

import org.koin.core.module.Module
import org.koin.core.module.dsl.scopedOf
import org.koin.dsl.bind

fun Module.settingsModule() {
    scope<SettingsFragment> {
        scopedOf(::SettingsPresenter) bind SettingsContract.Presenter::class
    }
}
