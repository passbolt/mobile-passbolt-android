package com.passbolt.mobile.android.feature.autofill.resources

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.core.compose.PassboltTheme
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.ActivityIntents.AuthConfig.SignIn
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.core.navigation.AutofillType
import com.passbolt.mobile.android.core.clipboard.ClipboardAccess
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.NavigationActivity.Start
import com.passbolt.mobile.android.core.ui.progressdialog.ProgressDialog
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticationHandler
import com.passbolt.mobile.android.feature.autofill.resources.AutofillResourcesIntent.UserAuthenticated
import com.passbolt.mobile.android.feature.autofill.resources.AutofillResourcesSideEffect.AutofillReturn
import com.passbolt.mobile.android.feature.autofill.resources.AutofillResourcesSideEffect.NavigateToAuth
import com.passbolt.mobile.android.feature.autofill.resources.AutofillResourcesSideEffect.NavigateToSetup
import com.passbolt.mobile.android.feature.autofill.resources.AutofillResourcesSideEffect.ShowToast
import com.passbolt.mobile.android.feature.autofill.resources.datasetstrategy.ReturnAutofillDatasetStrategy
import com.passbolt.mobile.android.feature.home.navigation.HomeTabContent
import com.passbolt.mobile.android.feature.otp.navigation.TotpTabContent
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Composable
fun AutofillResourcesScreen(
    autofillUri: String?,
    autofillType: AutofillType,
    returnAutofillDatasetStrategy: ReturnAutofillDatasetStrategy,
    modifier: Modifier = Modifier,
    viewModel: AutofillResourcesViewModel =
        koinViewModel(
            parameters = { parametersOf(autofillUri, autofillType) },
        ),
    appNavigator: AppNavigator = koinInject(),
    clipboardAccess: ClipboardAccess = koinInject(),
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current

    val authLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.onIntent(UserAuthenticated)
            } else {
                activity?.setResult(Activity.RESULT_CANCELED)
                activity?.finish()
            }
        }

    AuthenticationHandler()

    SideEffectDispatcher(viewModel.sideEffect) {
        when (it) {
            NavigateToAuth ->
                authLauncher.launch(
                    ActivityIntents.authentication(
                        context,
                        SignIn,
                        appContext = AppContext.AUTOFILL,
                    ),
                )
            NavigateToSetup -> {
                appNavigator.apply {
                    startNavigationActivity(context, Start)
                    finishActivity(activity)
                }
            }
            is AutofillReturn -> {
                // Must happen BEFORE returnDataset(): the strategies finish the
                // activity from inside that call.
                it.payload.totpCodeToCopy?.let { totp ->
                    clipboardAccess.setPrimaryClip(
                        context = context,
                        label = context.getString(LocalizationR.string.settings_autofill_copy_totp_clipboard_label),
                        value = totp,
                        isSensitive = true,
                    )
                }
                returnAutofillDatasetStrategy.returnDataset(it.payload)
            }
            is ShowToast ->
                Toast.makeText(context, getToastMessage(context, it.type), Toast.LENGTH_SHORT).show()
        }
    }

    AutofillResourcesScreen(
        showHome = state.showHome,
        showProgress = state.showProgress,
        autofillType = autofillType,
        modifier = modifier,
    )
}

@Composable
private fun AutofillResourcesScreen(
    showHome: Boolean,
    showProgress: Boolean,
    autofillType: AutofillType,
    modifier: Modifier = Modifier,
) {
    PassboltTheme {
        Scaffold(modifier = modifier) { innerPadding ->
            Box(
                modifier =
                    Modifier
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding),
            ) {
                if (showHome) {
                    when (autofillType) {
                        AutofillType.CREDENTIALS, AutofillType.CREDENTIALS_AND_TOTP -> HomeTabContent()
                        AutofillType.TOTP -> TotpTabContent()
                    }
                }

                ProgressDialog(showProgress)
            }
        }
    }
}
