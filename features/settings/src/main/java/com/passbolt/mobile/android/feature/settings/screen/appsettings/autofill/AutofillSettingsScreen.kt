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

package com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.ui.compose.switch.SwitchWithDescriptionItem
import com.passbolt.mobile.android.core.ui.compose.topbar.BackNavigationIcon
import com.passbolt.mobile.android.core.ui.compose.topbar.TitleAppBar
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.AutofillScreenSideEffect.ErrorSnackbarType.NATIVE_AUTOFILL_NOT_SUPPORTED
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.AutofillScreenSideEffect.NavigateToChromeNativeAutofill
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.AutofillScreenSideEffect.NavigateToEncourageAccessibilityAutofill
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.AutofillScreenSideEffect.NavigateToEncourageNativeAutofill
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.AutofillScreenSideEffect.NavigateToNativeAutofillEnabled
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.AutofillScreenSideEffect.NavigateUp
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.AutofillScreenSideEffect.ShowErrorSnackBar
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.AutofillSettingsIntent.GoBack
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.AutofillSettingsIntent.ToggleAccessibilityAutofill
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.AutofillSettingsIntent.ToggleChromeNativeAutofill
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.AutofillSettingsIntent.ToggleNativeAutofill
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.AutofillSettingsIntent.UpdateAutofillState
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
internal fun AutofillSettingsScreen(
    navigation: AutofillSettingsNavigation,
    viewModel: AutofillSettingsViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val state = viewModel.viewState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    AutofillSettingsScreen(
        state = state.value,
        snackbarHostState = snackbarHostState,
        onIntent = viewModel::onIntent,
    )

    RefreshAutofillStateSideEffect(viewModel::onIntent)

    SideEffectDispatcher(viewModel.sideEffect) {
        when (it) {
            NavigateUp -> navigation.navigateUp()
            NavigateToEncourageNativeAutofill -> navigation.navigateToEncourageNativeAutofill()
            NavigateToNativeAutofillEnabled -> navigation.navigateToNativeAutofillEnabled()
            NavigateToChromeNativeAutofill -> navigation.navigateToChromeNativeAutofillSettings()
            NavigateToEncourageAccessibilityAutofill -> navigation.navigateToEncourageAccessibilityAutofill()
            is ShowErrorSnackBar ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(getSnackbarMessage(context, it.type), duration = SnackbarDuration.Short)
                }
        }
    }
}

@Composable
private fun RefreshAutofillStateSideEffect(onIntent: (AutofillSettingsIntent) -> Unit) {
    val latestOnIntent by rememberUpdatedState(onIntent)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            object : DefaultLifecycleObserver {
                override fun onResume(owner: LifecycleOwner) {
                    latestOnIntent(UpdateAutofillState)
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

private fun getSnackbarMessage(
    context: Context,
    type: AutofillScreenSideEffect.ErrorSnackbarType,
): String =
    when (type) {
        NATIVE_AUTOFILL_NOT_SUPPORTED -> context.getString(LocalizationR.string.settings_autofill_autofill_service_not_supported)
    }

@Composable
private fun AutofillSettingsScreen(
    state: AutofillSettingsState,
    snackbarHostState: SnackbarHostState,
    onIntent: (AutofillSettingsIntent) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
    ) {
        TitleAppBar(
            navigationIcon = { BackNavigationIcon(onBackClick = { onIntent(GoBack) }) },
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(LocalizationR.string.settings_autofill_autofill_title),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(LocalizationR.string.settings_autofill_autofill_desc),
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.height(32.dp))

        SwitchWithDescriptionItem(
            title = stringResource(LocalizationR.string.settings_autofill_autofill_service),
            description = stringResource(LocalizationR.string.settings_autofill_autofill_service_description),
            isChecked = state.isNativeAutofillChecked,
            onClick = { onIntent(ToggleNativeAutofill) },
        )

        SwitchWithDescriptionItem(
            title = stringResource(LocalizationR.string.settings_chrome_native_autofill_autofill_service),
            description = stringResource(LocalizationR.string.settings_chrome_native_autofill_autofill_service_description),
            isChecked = state.isChromeNativeAutofillChecked,
            additionalDescription =
                if (!state.isChromeNativeAutofillEnabled) {
                    stringResource(LocalizationR.string.settings_chrome_native_autofill_not_supported)
                } else {
                    null
                },
            onClick = { onIntent(ToggleChromeNativeAutofill) },
            isEnabled = state.isChromeNativeAutofillEnabled,
        )

        SwitchWithDescriptionItem(
            title = stringResource(LocalizationR.string.settings_autofill_accessibility),
            description = stringResource(LocalizationR.string.settings_autofill_accessibility_description),
            isChecked = state.isAccessibilityAutofillChecked,
            onClick = { onIntent(ToggleAccessibilityAutofill) },
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier =
                Modifier
                    .align(Alignment.CenterHorizontally)
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
    }
}

@Preview(showBackground = true)
@Composable
private fun AutofillSettingsPreview() {
    AutofillSettingsScreen(
        state = AutofillSettingsState(),
        snackbarHostState = SnackbarHostState(),
        onIntent = {},
    )
}
