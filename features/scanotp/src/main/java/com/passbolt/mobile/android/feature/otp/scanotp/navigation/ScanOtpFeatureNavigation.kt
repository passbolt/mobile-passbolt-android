package com.passbolt.mobile.android.feature.otp.scanotp.navigation

import PassboltTheme
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.base.EntryProviderInstaller
import com.passbolt.mobile.android.core.navigation.compose.base.FeatureModuleNavigation
import com.passbolt.mobile.android.core.navigation.compose.keys.OtpNavigationKey.Otp
import com.passbolt.mobile.android.core.navigation.compose.keys.OtpNavigationKey.ResourcePicker
import com.passbolt.mobile.android.core.navigation.compose.keys.OtpNavigationKey.ScanOtp
import com.passbolt.mobile.android.core.navigation.compose.keys.OtpNavigationKey.ScanOtpMode.SCAN_FOR_RESULT
import com.passbolt.mobile.android.core.navigation.compose.keys.OtpNavigationKey.ScanOtpMode.SCAN_WITH_SUCCESS_SCREEN
import com.passbolt.mobile.android.core.navigation.compose.keys.OtpNavigationKey.ScanOtpSuccess
import com.passbolt.mobile.android.core.navigation.compose.results.NavigationResultEventBus
import com.passbolt.mobile.android.core.navigation.compose.results.OtpScanCompleteResult
import com.passbolt.mobile.android.core.navigation.compose.results.ResourcePickerResultEvent
import com.passbolt.mobile.android.core.navigation.compose.results.ResultEffect
import com.passbolt.mobile.android.core.navigation.compose.results.ResultEventBus
import com.passbolt.mobile.android.core.navigation.compose.results.ScanOtpResultEvent
import com.passbolt.mobile.android.feature.otp.scanotp.ScanOtpMode
import com.passbolt.mobile.android.feature.otp.scanotp.compose.ScanOtpNavigation
import com.passbolt.mobile.android.feature.otp.scanotp.compose.ScanOtpScreen
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessIntent.LinkedResourceReceived
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessNavigation
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessScreen
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessViewModel
import com.passbolt.mobile.android.ui.OtpParseResult
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import com.passbolt.mobile.android.core.navigation.compose.keys.OtpNavigationKey.ScanOtpMode as NavigationScanOtpMode

class ScanOtpFeatureNavigation : FeatureModuleNavigation {
    override fun provideEntryProviderInstaller(): EntryProviderInstaller =
        {
            entry<ScanOtp> { key ->
                val navigator: AppNavigator = koinInject()
                val resultBus = NavigationResultEventBus.current
                val mode = key.mode.toFeatureMode()

                PassboltTheme {
                    ScanOtpScreen(
                        mode = mode,
                        navigation = ScanOtpNavigator(navigator, resultBus, mode, key.parentFolderId),
                        navigator = navigator,
                    )
                }
            }

            entry<ScanOtpSuccess> { key ->
                val navigator: AppNavigator = koinInject()
                val resultBus = NavigationResultEventBus.current
                val scannedTotp = key.toTotpQr()

                val viewModel: ScanOtpSuccessViewModel =
                    koinViewModel { parametersOf(scannedTotp, key.parentFolderId) }

                ResultEffect<ResourcePickerResultEvent> { result ->
                    viewModel.onIntent(LinkedResourceReceived(result.resource))
                }

                PassboltTheme {
                    ScanOtpSuccessScreen(
                        scannedTotp = scannedTotp,
                        parentFolderId = key.parentFolderId,
                        navigation = ScanOtpSuccessNavigator(navigator, resultBus),
                        viewModel = viewModel,
                    )
                }
            }
        }
}

private class ScanOtpNavigator(
    private val navigator: AppNavigator,
    private val resultBus: ResultEventBus,
    private val mode: ScanOtpMode,
    private val parentFolderId: String?,
) : ScanOtpNavigation {
    override fun navigateBack() {
        navigator.navigateBack()
    }

    override fun navigateToSuccess(totpQr: OtpParseResult.OtpQr.TotpQr) {
        navigator.navigateToKey(
            ScanOtpSuccess(
                totpLabel = totpQr.label,
                totpSecret = totpQr.secret,
                totpIssuer = totpQr.issuer,
                totpAlgorithm = totpQr.algorithm.name,
                totpDigits = totpQr.digits,
                totpPeriod = totpQr.period,
                parentFolderId = parentFolderId,
            ),
        )
    }

    override fun setResultAndNavigateBack(totpQr: OtpParseResult.OtpQr.TotpQr) {
        resultBus.sendResult(
            result = ScanOtpResultEvent(false, totpQr),
        )
        navigator.navigateBack()
    }

    override fun setManualCreationResultAndNavigateBack() {
        when (mode) {
            ScanOtpMode.SCAN_FOR_RESULT -> {
                resultBus.sendResult(
                    result = ScanOtpResultEvent(true, null),
                )
                navigator.navigateBack()
            }
            ScanOtpMode.SCAN_WITH_SUCCESS_SCREEN -> {
                resultBus.sendResult(
                    result =
                        OtpScanCompleteResult(
                            otpCreated = false,
                            otpManualCreationChosen = true,
                        ),
                )
                navigator.popToKey(Otp)
            }
        }
    }
}

private class ScanOtpSuccessNavigator(
    private val navigator: AppNavigator,
    private val resultBus: ResultEventBus,
) : ScanOtpSuccessNavigation {
    override fun navigateToOtpList(
        totp: OtpParseResult.OtpQr.TotpQr,
        otpCreated: Boolean,
        resourceId: String,
    ) {
        resultBus.sendResult(
            result = OtpScanCompleteResult(otpCreated = otpCreated, otpManualCreationChosen = false),
        )
        navigator.popToKey(Otp)
    }

    override fun navigateToResourcePicker(suggestedUri: String?) {
        navigator.navigateToKey(ResourcePicker(suggestedUri))
    }
}

private fun NavigationScanOtpMode.toFeatureMode(): ScanOtpMode =
    when (this) {
        SCAN_FOR_RESULT -> ScanOtpMode.SCAN_FOR_RESULT
        SCAN_WITH_SUCCESS_SCREEN -> ScanOtpMode.SCAN_WITH_SUCCESS_SCREEN
    }

private fun ScanOtpSuccess.toTotpQr(): OtpParseResult.OtpQr.TotpQr =
    OtpParseResult.OtpQr.TotpQr(
        label = totpLabel,
        secret = totpSecret,
        issuer = totpIssuer,
        algorithm = OtpParseResult.OtpQr.Algorithm.valueOf(totpAlgorithm),
        digits = totpDigits,
        period = totpPeriod,
    )
