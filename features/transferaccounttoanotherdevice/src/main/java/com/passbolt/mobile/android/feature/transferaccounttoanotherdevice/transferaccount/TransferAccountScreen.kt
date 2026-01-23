/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021 Passbolt SA
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * The name "Passbolt" is a registered trademark of Passbolt SA, and Passbolt SA hereby declines to grant a trademark
 * license to "Passbolt" pursuant to the GNU Affero General Public License version 3 Section 7(e), without a separate
 * agreement with Passbolt SA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Passbolt SA (https://www.passbolt.com)
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://www.passbolt.com Passbolt (tm)
 * @since v1.0
 */

package com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount

import PassboltTheme
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration.Short
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.keys.TransferAccountToAnotherDeviceKey.TransferStatus
import com.passbolt.mobile.android.core.ui.compose.button.PrimaryButton
import com.passbolt.mobile.android.core.ui.compose.dialogs.CancelAccountTransferAlertDialog
import com.passbolt.mobile.android.core.ui.compose.progressdialog.ProgressDialog
import com.passbolt.mobile.android.core.ui.compose.topbar.BackNavigationIcon
import com.passbolt.mobile.android.core.ui.compose.topbar.TitleAppBar
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticationHandler
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.TransferAccountNavigation
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount.TransferAccountIntent.CancelTransfer
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount.TransferAccountIntent.ConfirmCancelTransfer
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount.TransferAccountIntent.DismissCancelDialog
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount.TransferAccountIntent.GoBack
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount.TransferAccountScreenSideEffect.NavigateToResult
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount.TransferAccountScreenSideEffect.ShowErrorSnackbar
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.qualifier.named
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
internal fun TransferAccountScreen(
    modifier: Modifier = Modifier,
    navigator: AppNavigator = koinInject(),
    viewModel: TransferAccountViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val state = viewModel.viewState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    TransferAccountScreen(
        modifier = modifier,
        state = state.value,
        snackbarHostState = snackbarHostState,
        onIntent = viewModel::onIntent,
    )

    AuthenticationHandler(
        onAuthenticatedIntent = viewModel::onAuthenticationIntent,
        authenticationSideEffect = viewModel.authenticationSideEffect,
    )

    SideEffectDispatcher(viewModel.sideEffect) {
        when (it) {
            is NavigateToResult ->
                if (context is TransferAccountNavigation) {
                    context.navigateToKey(TransferStatus(it.statusType))
                } else {
                    navigator.navigateToKey(TransferStatus(it.statusType))
                }
            is ShowErrorSnackbar ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(getSnackbarMessage(context, it), duration = Short)
                }
        }
    }
}

@Composable
private fun TransferAccountScreen(
    state: TransferAccountState,
    snackbarHostState: SnackbarHostState,
    onIntent: (TransferAccountIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Column {
            TitleAppBar(
                title = stringResource(LocalizationR.string.transfer_account_title),
                navigationIcon = { BackNavigationIcon(onBackClick = { onIntent(GoBack) }) },
            )

            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                if (state.qrCodeContent.isNotEmpty()) {
                    QrCodeImage(
                        content = state.qrCodeContent,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp),
                    )
                }
            }

            PrimaryButton(
                text = stringResource(LocalizationR.string.transfer_account_cancel_button),
                onClick = { onIntent(CancelTransfer) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = colorResource(CoreUiR.color.red),
                        contentColor = colorResource(CoreUiR.color.white),
                    ),
            )
        }

        CancelAccountTransferAlertDialog(
            isVisible = state.showCancelDialog,
            onConfirm = { onIntent(ConfirmCancelTransfer) },
            onDismiss = { onIntent(DismissCancelDialog) },
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 16.dp),
            snackbar = { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = colorResource(CoreUiR.color.red),
                    contentColor = colorResource(CoreUiR.color.white),
                )
            },
        )

        ProgressDialog(isVisible = state.showProgress)
    }
}

@Composable
private fun QrCodeImage(
    content: String,
    modifier: Modifier = Modifier,
    qrSizePx: Int = 399,
    barcodeEncoder: BarcodeEncoder = koinInject(),
    qrCodeGenHints: Map<EncodeHintType, Any> = koinInject(named(QR_CODE_GEN_HINTS)),
) {
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = content) {
        value =
            if (content.isNotEmpty()) {
                barcodeEncoder.encodeBitmap(
                    content,
                    BarcodeFormat.QR_CODE,
                    qrSizePx,
                    qrSizePx,
                    qrCodeGenHints,
                )
            } else {
                null
            }
    }

    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = stringResource(LocalizationR.string.transfer_account_title),
            modifier =
                modifier
                    .size(qrSizePx.dp)
                    .testTag("QrCode"), // TODO: move it to :testtags module once MOB-3312 gets resolved
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TransferAccountScreenPreview() {
    PassboltTheme {
        TransferAccountScreen(
            state =
                TransferAccountState(
                    qrCodeContent = "sample-qr-code-content",
                    currentPage = 0,
                    totalPages = 5,
                    showProgress = false,
                    showCancelDialog = false,
                ),
            snackbarHostState = SnackbarHostState(),
            onIntent = {},
            modifier = Modifier,
        )
    }
}
