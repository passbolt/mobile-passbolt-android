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

package com.passbolt.mobile.android.feature.setup.transferdetails

import PassboltTheme
import android.Manifest.permission.CAMERA
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.keys.SetupNavigationKey.ScanQrCodes
import com.passbolt.mobile.android.core.ui.button.PrimaryButton
import com.passbolt.mobile.android.core.ui.circlestepsview.CircleStepItemModel
import com.passbolt.mobile.android.core.ui.circlestepsview.CircleStepsView
import com.passbolt.mobile.android.core.ui.dialogs.CameraPermissionRequiredAlertDialog
import com.passbolt.mobile.android.core.ui.dialogs.CameraRequiredAlertDialog
import com.passbolt.mobile.android.core.ui.topbar.BackNavigationIcon
import com.passbolt.mobile.android.core.ui.topbar.TitleAppBar
import com.passbolt.mobile.android.feature.setup.transferdetails.TransferDetailsIntent.DismissCameraPermissionRequiredDialog
import com.passbolt.mobile.android.feature.setup.transferdetails.TransferDetailsIntent.DismissCameraRequiredDialog
import com.passbolt.mobile.android.feature.setup.transferdetails.TransferDetailsIntent.GoBack
import com.passbolt.mobile.android.feature.setup.transferdetails.TransferDetailsIntent.GoToSettings
import com.passbolt.mobile.android.feature.setup.transferdetails.TransferDetailsIntent.GrantCameraPermission
import com.passbolt.mobile.android.feature.setup.transferdetails.TransferDetailsIntent.RejectCameraPermission
import com.passbolt.mobile.android.feature.setup.transferdetails.TransferDetailsIntent.StartQrCodeScanning
import com.passbolt.mobile.android.feature.setup.transferdetails.TransferDetailsSideEffect.NavigateBack
import com.passbolt.mobile.android.feature.setup.transferdetails.TransferDetailsSideEffect.NavigateToAppSettings
import com.passbolt.mobile.android.feature.setup.transferdetails.TransferDetailsSideEffect.NavigateToScanQr
import com.passbolt.mobile.android.feature.setup.transferdetails.TransferDetailsSideEffect.RequestCameraPermission
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
internal fun TransferDetailsScreen(
    modifier: Modifier = Modifier,
    viewModel: TransferDetailsViewModel = koinViewModel(),
    navigator: AppNavigator = koinInject(),
) {
    val state = viewModel.viewState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val requestPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted: Boolean ->
            if (isGranted) {
                viewModel.onIntent(GrantCameraPermission)
            } else {
                viewModel.onIntent(RejectCameraPermission)
            }
        }

    TransferDetailsScreen(
        modifier = modifier,
        state = state.value,
        onIntent = viewModel::onIntent,
    )

    SideEffectDispatcher(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            NavigateBack -> navigator.navigateBack()
            NavigateToScanQr -> navigator.navigateToKey(ScanQrCodes)
            NavigateToAppSettings -> navigator.openAppOsSettings(context)
            RequestCameraPermission -> requestPermissionLauncher.launch(CAMERA)
        }
    }
}

@Composable
private fun TransferDetailsScreen(
    state: TransferDetailsState,
    onIntent: (TransferDetailsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val stepModel =
        remember(context) {
            resources
                .getStringArray(LocalizationR.array.transfer_details_steps_array)
                .map { CircleStepItemModel(AnnotatedString.fromHtml(it)) }
        }

    Scaffold(
        modifier = modifier,
        topBar = {
            TitleAppBar(
                title = stringResource(LocalizationR.string.transfer_account_title),
                navigationIcon = { BackNavigationIcon(onBackClick = { onIntent(GoBack) }) },
            )
        },
    ) { contentPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
        ) {
            Text(
                text = stringResource(LocalizationR.string.transfer_details_header),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
            )

            CircleStepsView(
                steps = stepModel,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
            )

            Box(
                modifier =
                    Modifier
                        .padding(top = 80.dp)
                        .size(200.dp)
                        .align(Alignment.CenterHorizontally)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(8.dp),
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(CoreUiR.drawable.ic_sample_qr_code),
                    contentDescription = null,
                    modifier = Modifier.size(180.dp),
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
            Spacer(modifier = Modifier.weight(1f))

            PrimaryButton(
                text = stringResource(LocalizationR.string.transfer_details_scan_button),
                onClick = { onIntent(StartQrCodeScanning) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
            )
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

@Preview(showBackground = true)
@Composable
private fun TransferDetailsScreenPreview() {
    PassboltTheme {
        TransferDetailsScreen(
            state = TransferDetailsState(),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TransferDetailsScreenWithCameraRequiredDialogPreview() {
    PassboltTheme {
        TransferDetailsScreen(
            state = TransferDetailsState(showCameraRequiredDialog = true),
            onIntent = {},
        )
    }
}
