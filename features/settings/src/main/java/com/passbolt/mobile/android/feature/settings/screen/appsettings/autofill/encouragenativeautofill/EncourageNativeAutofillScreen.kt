package com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encouragenativeautofill

import PassboltTheme
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.NavigationActivity.Home
import com.passbolt.mobile.android.core.navigation.compose.keys.SettingsNavigationKey.DismissBehavior
import com.passbolt.mobile.android.core.navigation.compose.keys.SettingsNavigationKey.DismissBehavior.FINISH_TO_HOME
import com.passbolt.mobile.android.core.navigation.compose.keys.SettingsNavigationKey.DismissBehavior.NAVIGATE_BACK
import com.passbolt.mobile.android.core.ui.compose.button.PrimaryButton
import com.passbolt.mobile.android.core.ui.compose.circlestepsview.CircleStepItemModel
import com.passbolt.mobile.android.core.ui.compose.circlestepsview.CircleStepsView
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encouragenativeautofill.EncourageNativeAutofillIntent.Close
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encouragenativeautofill.EncourageNativeAutofillIntent.DismissAutofillNotSupported
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encouragenativeautofill.EncourageNativeAutofillIntent.EnableAutofillService
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encouragenativeautofill.EncourageNativeAutofillIntent.SettingsResult
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encouragenativeautofill.EncourageNativeAutofillIntent.Skip
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encouragenativeautofill.EncourageNativeAutofillSideEffect.AutofillEnabled
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encouragenativeautofill.EncourageNativeAutofillSideEffect.NavigateBack
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encouragenativeautofill.EncourageNativeAutofillSideEffect.OpenAutofillSettings
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

private const val PACKAGE_URI_FORMAT = "package:%s"

@Composable
internal fun EncourageNativeAutofillScreen(
    dismissBehavior: DismissBehavior = NAVIGATE_BACK,
    navigator: AppNavigator = koinInject(),
    viewModel: EncourageNativeAutofillViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val state = viewModel.viewState.collectAsStateWithLifecycle()
    var navigateToHomeOnResume by remember { mutableStateOf(false) }
    val autofillSettingsLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) {
            viewModel.onIntent(SettingsResult)
        }

    EncourageNativeAutofillScreen(
        state = state.value,
        onIntent = viewModel::onIntent,
    )

    // After enabling autofill in OS settings the foreground auth check pauses this screen.
    // Wait for a full pause/resume cycle (auth completes) before navigating to Home to prevent both
    // Foreground auth mechanism and home asking for auth at once
    if (navigateToHomeOnResume) {
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            var hasPausedSinceEnabled = false
            val observer =
                object : DefaultLifecycleObserver {
                    override fun onPause(owner: LifecycleOwner) {
                        hasPausedSinceEnabled = true
                    }

                    override fun onResume(owner: LifecycleOwner) {
                        if (hasPausedSinceEnabled) {
                            navigator.startNavigationActivity(context, Home)
                            activity?.finish()
                        }
                    }
                }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
    }

    SideEffectDispatcher(viewModel.sideEffect) {
        when (it) {
            NavigateBack ->
                when (dismissBehavior) {
                    NAVIGATE_BACK -> navigator.navigateBack()
                    FINISH_TO_HOME -> {
                        navigator.startNavigationActivity(context, Home)
                        activity?.finish()
                    }
                }
            AutofillEnabled ->
                when (dismissBehavior) {
                    NAVIGATE_BACK -> navigator.navigateBack()
                    FINISH_TO_HOME -> navigateToHomeOnResume = true
                }
            OpenAutofillSettings ->
                autofillSettingsLauncher.launch(
                    Intent(
                        Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE,
                        PACKAGE_URI_FORMAT.format(context.packageName).toUri(),
                    ),
                )
        }
    }
}

@Composable
private fun EncourageNativeAutofillScreen(
    state: EncourageNativeAutofillState,
    onIntent: (EncourageNativeAutofillIntent) -> Unit,
) {
    val rawSteps = stringArrayResource(id = LocalizationR.array.dialog_encourage_autofill_setup_steps)
    val steps =
        remember(rawSteps) {
            rawSteps.mapIndexed { index, text ->
                CircleStepItemModel(
                    text = AnnotatedString.fromHtml(text),
                    icon = AUTOFILL_SETUP_STEPS_ICONS.getOrNull(index),
                )
            }
        }

    Scaffold { contentPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
        ) {
            IconButton(
                onClick = { onIntent(Close) },
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 16.dp, end = 16.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            }

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = stringResource(LocalizationR.string.dialog_encourage_autofill_header),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(48.dp))

                CircleStepsView(
                    steps = steps,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp),
                )

                Spacer(modifier = Modifier.weight(1f))

                PrimaryButton(
                    text = stringResource(LocalizationR.string.dialog_encourage_autofill_go_to_settings),
                    onClick = { onIntent(EnableAutofillService) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                )

                TextButton(
                    onClick = { onIntent(Skip) },
                    modifier = Modifier.padding(bottom = 32.dp),
                ) {
                    Text(
                        text = stringResource(LocalizationR.string.common_maybe_later),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
    }

    if (state.showAutofillNotSupported) {
        AlertDialog(
            onDismissRequest = { onIntent(DismissAutofillNotSupported) },
            title = { Text(stringResource(LocalizationR.string.dialog_encourage_autofill_autofill_not_supported_title)) },
            text = { Text(stringResource(LocalizationR.string.dialog_encourage_autofill_autofill_not_supported_message)) },
            confirmButton = {
                TextButton(onClick = { onIntent(DismissAutofillNotSupported) }) {
                    Text(stringResource(LocalizationR.string.ok))
                }
            },
        )
    }
}

private val AUTOFILL_SETUP_STEPS_ICONS =
    listOf(
        CoreUiR.drawable.autofill_with_bg,
        CoreUiR.drawable.passbolt_with_bg,
    )

@Preview(showBackground = true)
@Composable
private fun EncourageNativeAutofillScreenPreview() {
    PassboltTheme {
        EncourageNativeAutofillScreen(
            state = EncourageNativeAutofillState(),
            onIntent = {},
        )
    }
}
