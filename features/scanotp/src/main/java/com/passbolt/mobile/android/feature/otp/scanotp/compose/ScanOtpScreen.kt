package com.passbolt.mobile.android.feature.otp.scanotp.compose

import android.Manifest.permission.CAMERA
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.keys.OtpNavigationKey.Otp
import com.passbolt.mobile.android.core.navigation.compose.keys.OtpNavigationKey.ScanOtpSuccess
import com.passbolt.mobile.android.core.navigation.compose.results.NavigationResultEventBus
import com.passbolt.mobile.android.core.navigation.compose.results.OtpScanCompleteResult
import com.passbolt.mobile.android.core.navigation.compose.results.ScanOtpResultEvent
import com.passbolt.mobile.android.core.qrscan.SCAN_MANAGER_SCOPE
import com.passbolt.mobile.android.core.qrscan.manager.ScanManager
import com.passbolt.mobile.android.core.security.flagsecure.FlagSecureEffect
import com.passbolt.mobile.android.core.ui.compose.button.PrimaryButton
import com.passbolt.mobile.android.core.ui.compose.dialogs.CameraPermissionRequiredAlertDialog
import com.passbolt.mobile.android.core.ui.compose.dialogs.CameraRequiredAlertDialog
import com.passbolt.mobile.android.core.ui.compose.topbar.BackNavigationIcon
import com.passbolt.mobile.android.core.ui.compose.topbar.TitleAppBar
import com.passbolt.mobile.android.feature.otp.scanotp.ScanOtpMode
import com.passbolt.mobile.android.feature.otp.scanotp.compose.ScanOtpIntent.CreateTotpManually
import com.passbolt.mobile.android.feature.otp.scanotp.compose.ScanOtpIntent.DismissCameraPermissionRequiredDialog
import com.passbolt.mobile.android.feature.otp.scanotp.compose.ScanOtpIntent.DismissCameraRequiredDialog
import com.passbolt.mobile.android.feature.otp.scanotp.compose.ScanOtpIntent.GoBack
import com.passbolt.mobile.android.feature.otp.scanotp.compose.ScanOtpIntent.GoToSettings
import com.passbolt.mobile.android.feature.otp.scanotp.compose.ScanOtpIntent.Initialize
import com.passbolt.mobile.android.feature.otp.scanotp.compose.ScanOtpIntent.RejectCameraPermission
import com.passbolt.mobile.android.feature.otp.scanotp.compose.ScanOtpIntent.StartCameraError
import com.passbolt.mobile.android.feature.otp.scanotp.compose.ScanOtpSideEffect.NavigateBack
import com.passbolt.mobile.android.feature.otp.scanotp.compose.ScanOtpSideEffect.NavigateToAppSettings
import com.passbolt.mobile.android.feature.otp.scanotp.compose.ScanOtpSideEffect.NavigateToSuccess
import com.passbolt.mobile.android.feature.otp.scanotp.compose.ScanOtpSideEffect.RequestCameraPermission
import com.passbolt.mobile.android.feature.otp.scanotp.compose.ScanOtpSideEffect.SetManualCreationResultAndNavigateBack
import com.passbolt.mobile.android.feature.otp.scanotp.compose.ScanOtpSideEffect.SetResultAndNavigateBack
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.compose.scope.KoinScope
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.qualifier.named
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@OptIn(KoinExperimentalAPI::class)
@Composable
internal fun ScanOtpScreen(
    mode: ScanOtpMode,
    parentFolderId: String?,
    modifier: Modifier = Modifier,
    viewModel: ScanOtpViewModel = koinViewModel(),
    navigator: AppNavigator = koinInject(),
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val resultBus = NavigationResultEventBus.current

    FlagSecureEffect()

    val requestPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted: Boolean ->
            if (!isGranted) {
                viewModel.onIntent(RejectCameraPermission)
            }
        }

    KoinScope(
        scopeID = SCAN_MANAGER_SCOPE,
        scopeQualifier = named(SCAN_MANAGER_SCOPE),
    ) {
        val scanManager: ScanManager = koinInject()

        LaunchedEffect(scanManager) {
            viewModel.onIntent(
                Initialize(
                    barcodeScanFlow = scanManager.barcodeScanPublisher,
                    mode = mode,
                ),
            )
        }

        DisposableEffect(scanManager) {
            onDispose {
                scanManager.detach()
            }
        }

        ScanOtpScreen(
            modifier = modifier,
            state = state,
            onIntent = viewModel::onIntent,
            scanManager = scanManager,
            lifecycleOwner = lifecycleOwner,
        )
    }

    SideEffectDispatcher(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            RequestCameraPermission -> requestPermissionLauncher.launch(CAMERA)
            is NavigateToSuccess ->
                navigator.navigateToKey(
                    ScanOtpSuccess(
                        totpLabel = sideEffect.totpQr.label,
                        totpSecret = sideEffect.totpQr.secret,
                        totpIssuer = sideEffect.totpQr.issuer,
                        totpAlgorithm = sideEffect.totpQr.algorithm.name,
                        totpDigits = sideEffect.totpQr.digits,
                        totpPeriod = sideEffect.totpQr.period,
                        parentFolderId = parentFolderId,
                    ),
                )
            is SetResultAndNavigateBack -> {
                resultBus.sendResult(result = ScanOtpResultEvent(false, sideEffect.totpQr))
                navigator.navigateBack()
            }
            SetManualCreationResultAndNavigateBack ->
                when (mode) {
                    ScanOtpMode.SCAN_FOR_RESULT -> {
                        resultBus.sendResult(result = ScanOtpResultEvent(true, null))
                        navigator.navigateBack()
                    }
                    ScanOtpMode.SCAN_WITH_SUCCESS_SCREEN -> {
                        resultBus.sendResult(
                            result = OtpScanCompleteResult(otpCreated = false, otpManualCreationChosen = true),
                        )
                        navigator.popToKey(Otp)
                    }
                }
            NavigateToAppSettings -> {
                navigator.navigateBack()
                navigator.openAppOsSettings(context)
            }
            NavigateBack -> navigator.navigateBack()
        }
    }
}

@Composable
private fun ScanOtpScreen(
    state: ScanOtpState,
    onIntent: (ScanOtpIntent) -> Unit,
    scanManager: ScanManager,
    lifecycleOwner: LifecycleOwner,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TitleAppBar(
                title = stringResource(LocalizationR.string.otp_scan_title),
                navigationIcon = { BackNavigationIcon(onBackClick = { onIntent(GoBack) }) },
            )
        },
        containerColor = colorResource(CoreUiR.color.background_gray_dark),
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            Text(
                text = getTooltipMessage(state.tooltipMessage, state.scanErrorMessage),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(colorResource(CoreUiR.color.background))
                        .padding(vertical = 8.dp),
                color = colorResource(CoreUiR.color.text_primary),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
            ) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            try {
                                scanManager.attach(lifecycleOwner, this)
                            } catch (exception: Exception) {
                                onIntent(StartCameraError(exception))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .background(colorResource(CoreUiR.color.background)),
                contentAlignment = Alignment.Center,
            ) {
                PrimaryButton(
                    text = stringResource(LocalizationR.string.scan_qr_or_create_totp_manually),
                    onClick = { onIntent(CreateTotpManually) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }

    CameraRequiredAlertDialog(
        isVisible = state.showCameraRequiredDialog,
        onDismissRequest = { onIntent(DismissCameraRequiredDialog) },
    )

    CameraPermissionRequiredAlertDialog(
        isVisible = state.showCameraPermissionRequiredDialog,
        onDismissRequest = { onIntent(DismissCameraPermissionRequiredDialog) },
        onSettingsClick = { onIntent(GoToSettings) },
    )
}
