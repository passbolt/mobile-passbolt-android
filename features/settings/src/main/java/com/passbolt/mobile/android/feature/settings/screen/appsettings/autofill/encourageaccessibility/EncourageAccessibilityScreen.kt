package com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encourageaccessibility

import PassboltTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.core.compose.OnResumeEffect
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.ui.compose.switch.SwitchWithDescriptionItem
import com.passbolt.mobile.android.core.ui.compose.topbar.BackNavigationIcon
import com.passbolt.mobile.android.core.ui.compose.topbar.TitleAppBar
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encourageaccessibility.EncourageAccessibilityIntent.Close
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encourageaccessibility.EncourageAccessibilityIntent.ConsentToEnableAccessibility
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encourageaccessibility.EncourageAccessibilityIntent.DismissEnableAccessibilityConsent
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encourageaccessibility.EncourageAccessibilityIntent.EnableAccessibilityService
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encourageaccessibility.EncourageAccessibilityIntent.GrantOverlayPermission
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encourageaccessibility.EncourageAccessibilityIntent.RefreshState
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encourageaccessibility.EncourageAccessibilitySideEffect.NavigateBack
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encourageaccessibility.EncourageAccessibilitySideEffect.OpenAccessibilitySettings
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encourageaccessibility.EncourageAccessibilitySideEffect.OpenOverlaySettings
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import com.passbolt.mobile.android.core.localization.R as LocalizationR

@Composable
internal fun EncourageAccessibilityScreen(
    navigator: AppNavigator = koinInject(),
    viewModel: EncourageAccessibilityViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val state = viewModel.viewState.collectAsStateWithLifecycle()

    EncourageAccessibilityScreen(
        state = state.value,
        onIntent = viewModel::onIntent,
    )

    OnResumeEffect { viewModel.onIntent(RefreshState) }

    SideEffectDispatcher(viewModel.sideEffect) {
        when (it) {
            NavigateBack -> navigator.navigateBack()
            OpenOverlaySettings -> navigator.openAppOsSettings(context)
            OpenAccessibilitySettings -> navigator.openAccessibilitySettings(context)
        }
    }
}

@Composable
private fun EncourageAccessibilityScreen(
    state: EncourageAccessibilityState,
    onIntent: (EncourageAccessibilityIntent) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TitleAppBar(
                navigationIcon = { BackNavigationIcon(onBackClick = { onIntent(Close) }) },
            )
        },
        content = { paddingValues ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = stringResource(LocalizationR.string.dialog_encourage_autofill_accessibility_title),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = stringResource(LocalizationR.string.dialog_encourage_autofill_accessibility_description),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                )

                Spacer(modifier = Modifier.height(32.dp))

                SwitchWithDescriptionItem(
                    title = stringResource(LocalizationR.string.autofill_service_enable_service),
                    description = stringResource(LocalizationR.string.autofill_service_enable_service_description),
                    isChecked = state.isAccessibilityServiceEnabled,
                    onClick = { onIntent(EnableAccessibilityService) },
                )

                SwitchWithDescriptionItem(
                    title = stringResource(LocalizationR.string.autofill_service_enable_overlay),
                    description = stringResource(LocalizationR.string.autofill_service_overlay_description),
                    isChecked = state.isOverlayPermissionGranted,
                    onClick = { onIntent(GrantOverlayPermission) },
                )
            }

            if (state.showAccessibilityConsent) {
                AlertDialog(
                    onDismissRequest = { onIntent(DismissEnableAccessibilityConsent) },
                    title = { Text(stringResource(LocalizationR.string.dialog_accessibility_consent_title)) },
                    text = { Text(stringResource(LocalizationR.string.dialog_accessibility_consent_message)) },
                    confirmButton = {
                        TextButton(onClick = { onIntent(ConsentToEnableAccessibility) }) {
                            Text(stringResource(LocalizationR.string.consent))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { onIntent(DismissEnableAccessibilityConsent) }) {
                            Text(stringResource(LocalizationR.string.cancel))
                        }
                    },
                )
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun EncourageAccessibilityScreenPreview() {
    PassboltTheme {
        EncourageAccessibilityScreen(
            state = EncourageAccessibilityState(),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EncourageAccessibilityScreenEnabledPreview() {
    PassboltTheme {
        EncourageAccessibilityScreen(
            state =
                EncourageAccessibilityState(
                    isAccessibilityServiceEnabled = true,
                    isOverlayPermissionGranted = true,
                ),
            onIntent = {},
        )
    }
}
