package com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess

import PassboltTheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.keys.OtpNavigationKey.Otp
import com.passbolt.mobile.android.core.navigation.compose.keys.OtpNavigationKey.ResourcePicker
import com.passbolt.mobile.android.core.navigation.compose.results.NavigationResultEventBus
import com.passbolt.mobile.android.core.navigation.compose.results.OtpScanCompleteResult
import com.passbolt.mobile.android.core.ui.button.PrimaryButton
import com.passbolt.mobile.android.core.ui.progressdialog.ProgressDialog
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessIntent.CreateStandaloneOtpClick
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessIntent.LinkToResourceClick
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessSideEffect.NavigateToOtpList
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessSideEffect.NavigateToResourcePicker
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessSideEffect.ShowSuccessSnackbar
import com.passbolt.mobile.android.ui.OtpParseResult
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
internal fun ScanOtpSuccessScreen(
    scannedTotp: OtpParseResult.OtpQr.TotpQr,
    parentFolderId: String?,
    modifier: Modifier = Modifier,
    viewModel: ScanOtpSuccessViewModel = koinViewModel { parametersOf(scannedTotp, parentFolderId) },
    navigator: AppNavigator = koinInject(),
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val resultBus = NavigationResultEventBus.current

    ScanOtpSuccessScreen(
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )

    MetadataKeyDialogs(
        state = state,
        onIntent = viewModel::onIntent,
    )

    SideEffectDispatcher(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            is NavigateToOtpList -> {
                resultBus.sendResult(
                    result = OtpScanCompleteResult(otpCreated = sideEffect.otpCreated, otpManualCreationChosen = false),
                )
                navigator.popToKey(Otp)
            }
            is NavigateToResourcePicker ->
                navigator.navigateToKey(ResourcePicker(sideEffect.suggestedUri))
            is ShowErrorSnackbar ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(getErrorSnackbarMessage(context, sideEffect))
                }
            is ShowSuccessSnackbar ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(getSuccessSnackbarMessage(context, sideEffect))
                }
        }
    }
}

@Composable
private fun ScanOtpSuccessScreen(
    state: ScanOtpSuccessState,
    onIntent: (ScanOtpSuccessIntent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Image(
                painter = painterResource(CoreUiR.drawable.ic_success),
                contentDescription = null,
                modifier = Modifier.size(148.dp),
            )

            Text(
                text = stringResource(LocalizationR.string.otp_create_success),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 32.dp),
                textAlign = TextAlign.Center,
                fontSize = 28.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.W600,
                color = colorResource(CoreUiR.color.text_primary),
                maxLines = 2,
            )

            Spacer(modifier = Modifier.weight(1f))

            PrimaryButton(
                text = stringResource(LocalizationR.string.otp_create_totp_create_standalone),
                onClick = { onIntent(CreateStandaloneOtpClick) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(LocalizationR.string.otp_create_totp_link_to_password),
                modifier =
                    Modifier
                        .clickable { onIntent(LinkToResourceClick) }
                        .padding(16.dp),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.labelLarge,
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    ProgressDialog(isVisible = state.showProgress)
}

@Preview(showBackground = true)
@Composable
private fun ScanOtpSuccessScreenPreview() {
    PassboltTheme {
        ScanOtpSuccessScreen(
            state = ScanOtpSuccessState(),
            onIntent = {},
            snackbarHostState = SnackbarHostState(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ScanOtpSuccessScreenProgressPreview() {
    PassboltTheme {
        ScanOtpSuccessScreen(
            state = ScanOtpSuccessState(showProgress = true),
            onIntent = {},
            snackbarHostState = SnackbarHostState(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ScanOtpSuccessScreenDarkPreview() {
    PassboltTheme(darkTheme = true) {
        ScanOtpSuccessScreen(
            state = ScanOtpSuccessState(),
            onIntent = {},
            snackbarHostState = SnackbarHostState(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ScanOtpSuccessScreenProgressDarkPreview() {
    PassboltTheme(darkTheme = true) {
        ScanOtpSuccessScreen(
            state = ScanOtpSuccessState(showProgress = true),
            onIntent = {},
            snackbarHostState = SnackbarHostState(),
        )
    }
}
