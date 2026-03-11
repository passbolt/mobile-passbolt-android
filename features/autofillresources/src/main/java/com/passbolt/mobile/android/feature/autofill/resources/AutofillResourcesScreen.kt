package com.passbolt.mobile.android.feature.autofill.resources

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.ActivityIntents.AuthConfig.SignIn
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.HomeNavigation
import com.passbolt.mobile.android.core.navigation.compose.NavigationActivity.Start
import com.passbolt.mobile.android.core.preferences.usecase.GetHomeDisplayViewPrefsUseCase
import com.passbolt.mobile.android.core.ui.compose.progressdialog.ProgressDialog
import com.passbolt.mobile.android.feature.autofill.resources.AutofillResourcesIntent.UserAuthenticated
import com.passbolt.mobile.android.feature.autofill.resources.AutofillResourcesSideEffect.AutofillReturn
import com.passbolt.mobile.android.feature.autofill.resources.AutofillResourcesSideEffect.NavigateToAuth
import com.passbolt.mobile.android.feature.autofill.resources.AutofillResourcesSideEffect.NavigateToSetup
import com.passbolt.mobile.android.feature.autofill.resources.AutofillResourcesSideEffect.ShowToast
import com.passbolt.mobile.android.feature.autofill.resources.datasetstrategy.ReturnAutofillDatasetStrategy
import com.passbolt.mobile.android.mappers.HomeDisplayViewMapper
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Composable
fun AutofillResourcesScreen(
    autofillUri: String?,
    returnAutofillDatasetStrategy: ReturnAutofillDatasetStrategy,
    modifier: Modifier = Modifier,
    viewModel: AutofillResourcesViewModel =
        koinViewModel(
            parameters = { parametersOf(autofillUri) },
        ),
    appNavigator: AppNavigator = koinInject(),
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
                activity?.finish()
            }
        }

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
            is AutofillReturn ->
                returnAutofillDatasetStrategy.returnDataset(it.username, it.password, it.uri)
            is ShowToast ->
                Toast.makeText(context, getToastMessage(context, it.type), Toast.LENGTH_SHORT).show()
        }
    }

    AutofillResourcesScreen(
        showHome = state.showHome,
        showProgress = state.showProgress,
        modifier = modifier,
    )
}

@Composable
private fun AutofillResourcesScreen(
    showHome: Boolean,
    showProgress: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        if (showHome) {
            val filterPreferencesUseCase: GetHomeDisplayViewPrefsUseCase = koinInject()
            val homeDisplayMapper: HomeDisplayViewMapper = koinInject()

            val filterPreferences = remember { filterPreferencesUseCase.execute(Unit) }
            val initialHomeDisplay =
                remember {
                    homeDisplayMapper.map(
                        filterPreferences.userSetHomeView,
                        filterPreferences.lastUsedHomeView,
                    )
                }

            HomeNavigation(initialHomeDisplay = initialHomeDisplay)
        }

        ProgressDialog(showProgress)
    }
}
