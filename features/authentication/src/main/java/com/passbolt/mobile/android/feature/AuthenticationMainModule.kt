package com.passbolt.mobile.android.feature

import androidx.biometric.BiometricPrompt
import com.passbolt.mobile.android.core.navigation.compose.base.Feature
import com.passbolt.mobile.android.core.navigation.compose.base.FeatureModuleNavigation
import com.passbolt.mobile.android.feature.authentication.AuthenticationStartUpResolver
import com.passbolt.mobile.android.feature.authentication.accountslist.accountsListModule
import com.passbolt.mobile.android.feature.authentication.auth.authModule
import com.passbolt.mobile.android.feature.authentication.auth.usecase.RefreshSessionUseCase
import com.passbolt.mobile.android.feature.authentication.mfa.duo.authWithDuoModule
import com.passbolt.mobile.android.feature.authentication.mfa.totp.enterTotpModule
import com.passbolt.mobile.android.feature.authentication.mfa.unknown.unknownProviderModule
import com.passbolt.mobile.android.feature.authentication.mfa.yubikey.scanYubikeyModule
import com.passbolt.mobile.android.feature.authentication.navigation.AuthenticationFeatureNavigation
import org.koin.core.module.dsl.factoryOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val authenticationModule =
    module {
        single<FeatureModuleNavigation>(named(Feature.AUTHENTICATION)) {
            AuthenticationFeatureNavigation()
        }
        single { BiometricPrompt.PromptInfo.Builder() }
        factoryOf(::RefreshSessionUseCase)
        factoryOf(::AuthenticationStartUpResolver)

        accountsListModule()
        authModule()
        scanYubikeyModule()
        enterTotpModule()
        unknownProviderModule()
        authWithDuoModule()
    }
