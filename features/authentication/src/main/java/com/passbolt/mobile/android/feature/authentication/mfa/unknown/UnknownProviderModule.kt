package com.passbolt.mobile.android.feature.authentication.mfa.unknown

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf

fun Module.unknownProviderModule() {
    viewModelOf(::UnknownProviderViewModel)
}
