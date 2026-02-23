package com.passbolt.mobile.android.feature.resourceform.additionalsecrets.password

import PassboltTheme
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.ui.compose.button.PrimaryButton
import com.passbolt.mobile.android.core.ui.compose.text.TextInput
import com.passbolt.mobile.android.core.ui.compose.topbar.BackNavigationIcon
import com.passbolt.mobile.android.core.ui.compose.topbar.TitleAppBar
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.password.PasswordFormIntent.ApplyChanges
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.password.PasswordFormIntent.GeneratePassword
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.password.PasswordFormIntent.GoBack
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.password.PasswordFormIntent.MainUriTextChanged
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.password.PasswordFormIntent.PasswordTextChanged
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.password.PasswordFormIntent.UsernameTextChanged
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.password.PasswordFormSideEffect.ApplyAndGoBack
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.password.PasswordFormSideEffect.NavigateBack
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.password.PasswordFormSideEffect.ShowUnableToGeneratePassword
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.password.ui.PasswordGenerationInput
import com.passbolt.mobile.android.ui.LeadingContentType
import com.passbolt.mobile.android.ui.PasswordStrength
import com.passbolt.mobile.android.ui.PasswordUiModel
import com.passbolt.mobile.android.ui.ResourceFormMode
import com.passbolt.mobile.android.ui.ResourceFormMode.Create
import com.passbolt.mobile.android.ui.ResourceFormMode.Edit
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
internal fun PasswordFormScreen(
    mode: ResourceFormMode,
    passwordModel: PasswordUiModel,
    navigation: PasswordFormNavigation,
    modifier: Modifier = Modifier,
    viewModel: PasswordFormViewModel =
        koinViewModel(
            parameters = {
                parametersOf(mode, passwordModel)
            },
        ),
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()

    PasswordFormScreen(
        modifier = modifier,
        state = state,
        onIntent = viewModel::onIntent,
    )

    SideEffectDispatcher(viewModel.sideEffect) {
        when (it) {
            is ApplyAndGoBack -> navigation.navigateBackWithResult(it.model)
            NavigateBack -> navigation.navigateBack()
            is ShowUnableToGeneratePassword -> navigation.showUnableToGeneratePassword(it.minimumEntropyBits)
        }
    }
}

@Composable
private fun PasswordFormScreen(
    state: PasswordFormState,
    onIntent: (PasswordFormIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Scaffold(
        modifier = modifier,
        topBar = {
            TitleAppBar(
                title = getScreenTitle(context, state.resourceFormMode),
                navigationIcon = { BackNavigationIcon(onBackClick = { onIntent(GoBack) }) },
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.background,
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                PrimaryButton(
                    text = stringResource(LocalizationR.string.apply),
                    onClick = { onIntent(ApplyChanges) },
                )
            }
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
        ) {
            Text(
                text = stringResource(LocalizationR.string.resource_form_password),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colorResource(CoreUiR.color.section_background))
                        .padding(12.dp),
            ) {
                TextInput(
                    title = stringResource(LocalizationR.string.resource_form_main_uri),
                    text = state.mainUri,
                    onTextChange = { onIntent(MainUriTextChanged(it)) },
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextInput(
                    title = stringResource(LocalizationR.string.resource_form_username),
                    text = state.username,
                    onTextChange = { onIntent(UsernameTextChanged(it)) },
                )
                Spacer(modifier = Modifier.height(16.dp))
                PasswordGenerationInput(
                    password = state.password,
                    passwordStrength = state.passwordStrength,
                    entropy = state.entropy,
                    onPasswordChange = { onIntent(PasswordTextChanged(it)) },
                    onGenerateClick = { onIntent(GeneratePassword) },
                )
            }
        }
    }
}

private fun getScreenTitle(
    context: Context,
    resourceFormMode: ResourceFormMode?,
): String =
    when (resourceFormMode) {
        is Create -> context.getString(LocalizationR.string.resource_form_create_password)
        is Edit -> context.getString(LocalizationR.string.resource_form_edit_resource, resourceFormMode.resourceName)
        null -> ""
    }

@Preview(showBackground = true)
@Composable
private fun PasswordFormScreenPreview() {
    PassboltTheme {
        PasswordFormScreen(
            state =
                PasswordFormState(
                    resourceFormMode = Create(LeadingContentType.PASSWORD, null),
                    password = "p@ssb0lt!",
                    passwordStrength = PasswordStrength.Strong,
                    entropy = 60.0,
                    mainUri = "https://passbolt.com",
                    username = "ada@passbolt.com",
                ),
            onIntent = {},
        )
    }
}
