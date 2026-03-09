package com.passbolt.mobile.android.feature.authentication.navigation

import PassboltTheme
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.LocalAuthenticationParams
import com.passbolt.mobile.android.core.navigation.compose.base.EntryProviderInstaller
import com.passbolt.mobile.android.core.navigation.compose.base.FeatureModuleNavigation
import com.passbolt.mobile.android.core.navigation.compose.keys.AuthenticationNavigationKey.AccountsList
import com.passbolt.mobile.android.core.navigation.compose.keys.AuthenticationNavigationKey.Auth
import com.passbolt.mobile.android.core.navigation.compose.keys.AuthenticationNavigationKey.MfaDuo
import com.passbolt.mobile.android.core.navigation.compose.keys.AuthenticationNavigationKey.MfaTotp
import com.passbolt.mobile.android.core.navigation.compose.keys.AuthenticationNavigationKey.MfaUnknownProvider
import com.passbolt.mobile.android.core.navigation.compose.keys.AuthenticationNavigationKey.MfaYubikey
import com.passbolt.mobile.android.core.navigation.compose.results.NavigationResultEventBus
import com.passbolt.mobile.android.feature.authentication.accountslist.AccountsListScreen
import com.passbolt.mobile.android.feature.authentication.auth.AuthScreen
import com.passbolt.mobile.android.feature.authentication.mfa.MfaDialogState
import com.passbolt.mobile.android.feature.authentication.mfa.MfaResult
import com.passbolt.mobile.android.feature.authentication.mfa.duo.AuthWithDuoScreen
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpScreen
import com.passbolt.mobile.android.feature.authentication.mfa.unknown.UnknownProviderScreen
import com.passbolt.mobile.android.feature.authentication.mfa.yubikey.ScanYubikeyScreen
import org.koin.compose.koinInject

class AuthenticationFeatureNavigation : FeatureModuleNavigation {
    @Suppress("LongMethod")
    override fun provideEntryProviderInstaller(): EntryProviderInstaller =
        {
            entry<AccountsList> {
                val params = LocalAuthenticationParams.current
                PassboltTheme {
                    AccountsListScreen(
                        authConfig = params.authConfig,
                    )
                }
            }

            entry<Auth> { key ->
                val params = LocalAuthenticationParams.current

                PassboltTheme {
                    AuthScreen(
                        authConfig = key.authConfig ?: params.authConfig,
                        userId = key.userId,
                        appContext = params.appContext,
                    )
                }
            }

            entry<MfaTotp> { key ->
                val resultBus = NavigationResultEventBus.current
                val navigator: AppNavigator = koinInject()
                PassboltTheme {
                    EnterTotpScreen(
                        mfaState = MfaDialogState.Totp(key.authToken, key.hasOtherProviders),
                        onMfaResult = {
                            resultBus.sendResult<MfaResult>(result = it)
                            navigator.navigateBack()
                        },
                    )
                }
            }

            entry<MfaYubikey> { key ->
                val resultBus = NavigationResultEventBus.current
                val navigator: AppNavigator = koinInject()
                PassboltTheme {
                    ScanYubikeyScreen(
                        mfaState = MfaDialogState.Yubikey(key.authToken, key.hasOtherProviders),
                        onMfaResult = {
                            resultBus.sendResult<MfaResult>(result = it)
                            navigator.navigateBack()
                        },
                    )
                }
            }

            entry<MfaDuo> { key ->
                val resultBus = NavigationResultEventBus.current
                val navigator: AppNavigator = koinInject()
                PassboltTheme {
                    AuthWithDuoScreen(
                        mfaState = MfaDialogState.Duo(key.authToken, key.hasOtherProviders),
                        onMfaResult = {
                            resultBus.sendResult<MfaResult>(result = it)
                            navigator.navigateBack()
                        },
                    )
                }
            }

            entry<MfaUnknownProvider> {
                PassboltTheme {
                    UnknownProviderScreen()
                }
            }
        }
}
