package com.passbolt.mobile.android.feature.authentication.mfa.unknown

import PassboltTheme
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.NavigationActivity.Start
import com.passbolt.mobile.android.core.ui.compose.progressdialog.ProgressDialog
import com.passbolt.mobile.android.feature.authentication.mfa.unknown.UnknownProviderIntent.Close
import com.passbolt.mobile.android.feature.authentication.mfa.unknown.UnknownProviderSideEffect.CloseAndNavigateToStartup
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
internal fun UnknownProviderScreen(
    viewModel: UnknownProviderViewModel = koinViewModel(),
    appNavigator: AppNavigator = koinInject(),
) {
    val context = LocalContext.current
    val state by viewModel.viewState.collectAsStateWithLifecycle()

    BackHandler { viewModel.onIntent(Close) }

    SideEffectDispatcher(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            is CloseAndNavigateToStartup -> appNavigator.startNavigationActivity(context, Start)
        }
    }

    UnknownProviderScreen(
        state = state,
        onIntent = viewModel::onIntent,
    )
}

@Composable
private fun UnknownProviderScreen(
    state: UnknownProviderState,
    onIntent: (UnknownProviderIntent) -> Unit,
) {
    Scaffold { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IconButton(
                onClick = { onIntent(Close) },
                modifier =
                    Modifier
                        .align(Alignment.End)
                        .padding(top = 16.dp, end = 16.dp),
            ) {
                Image(
                    painter = painterResource(CoreUiR.drawable.ic_close),
                    contentDescription = null,
                )
            }

            Spacer(modifier = Modifier.height(144.dp))

            Image(
                painter = painterResource(CoreUiR.drawable.ic_failed),
                contentDescription = null,
                modifier = Modifier.size(148.dp),
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(LocalizationR.string.dialog_mfa_unknown_provider_header),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(LocalizationR.string.dialog_mfa_unknown_provider_description),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
            )
        }
    }

    ProgressDialog(isVisible = state.showProgress)
}

@Preview(showBackground = true)
@Composable
private fun UnknownProviderScreenPreview() {
    PassboltTheme {
        UnknownProviderScreen(
            state = UnknownProviderState(),
            onIntent = {},
        )
    }
}
