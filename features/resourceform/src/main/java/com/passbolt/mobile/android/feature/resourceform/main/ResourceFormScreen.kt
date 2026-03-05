package com.passbolt.mobile.android.feature.resourceform.main

import PassboltTheme
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.keys.OtpNavigationKey.ScanOtp
import com.passbolt.mobile.android.core.navigation.compose.keys.OtpNavigationKey.ScanOtpMode
import com.passbolt.mobile.android.core.navigation.compose.keys.ResourceFormNavigationKey.AdditionalUrisForm
import com.passbolt.mobile.android.core.navigation.compose.keys.ResourceFormNavigationKey.AppearanceForm
import com.passbolt.mobile.android.core.navigation.compose.keys.ResourceFormNavigationKey.CustomFieldsForm
import com.passbolt.mobile.android.core.navigation.compose.keys.ResourceFormNavigationKey.DescriptionForm
import com.passbolt.mobile.android.core.navigation.compose.keys.ResourceFormNavigationKey.NoteForm
import com.passbolt.mobile.android.core.navigation.compose.keys.ResourceFormNavigationKey.PasswordForm
import com.passbolt.mobile.android.core.navigation.compose.keys.ResourceFormNavigationKey.TotpAdvancedSettingsForm
import com.passbolt.mobile.android.core.navigation.compose.keys.ResourceFormNavigationKey.TotpForm
import com.passbolt.mobile.android.core.navigation.compose.results.NavigationResultEventBus
import com.passbolt.mobile.android.core.navigation.compose.results.ResourceFormCompleteResult
import com.passbolt.mobile.android.core.ui.compose.button.PrimaryButton
import com.passbolt.mobile.android.core.ui.compose.progressdialog.ProgressDialog
import com.passbolt.mobile.android.core.ui.compose.text.TextInput
import com.passbolt.mobile.android.core.ui.compose.topbar.BackNavigationIcon
import com.passbolt.mobile.android.core.ui.compose.topbar.TitleAppBar
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticationHandler
import com.passbolt.mobile.android.feature.metadatakeytrust.ui.compose.NewMetadataKeyTrustDialog
import com.passbolt.mobile.android.feature.metadatakeytrust.ui.compose.TrustedMetadataKeyDeletedDialog
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.CreateResource
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.DismissMetadataKeyDialog
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.ExpandAdvancedSettings
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.GoBack
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.NameTextChanged
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.TrustNewMetadataKey
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.TrustedMetadataKeyDeleted
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.UpdateResource
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateBack
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateBackWithCreateSuccess
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateBackWithEditSuccess
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateToAdditionalUris
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateToAppearance
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateToCustomFields
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateToDescription
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateToNote
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateToPassword
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateToScanOtp
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateToTotp
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateToTotpAdvancedSettings
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.ShowSnackbar
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.ShowToast
import com.passbolt.mobile.android.feature.resourceform.main.ui.AdditionalSecretsSection
import com.passbolt.mobile.android.feature.resourceform.main.ui.LeadingContent
import com.passbolt.mobile.android.feature.resourceform.main.ui.MetadataSection
import com.passbolt.mobile.android.ui.LeadingContentType
import com.passbolt.mobile.android.ui.PasswordStrength
import com.passbolt.mobile.android.ui.ResourceFormMode.Create
import com.passbolt.mobile.android.ui.ResourceFormMode.Edit
import com.passbolt.mobile.android.ui.ResourceFormUiModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import com.passbolt.mobile.android.core.localization.R as LocalizationR

@Suppress("CyclomaticComplexMethod")
@Composable
internal fun ResourceFormScreen(
    viewModel: ResourceFormViewModel,
    modifier: Modifier = Modifier,
    navigator: AppNavigator = koinInject(),
) {
    val state = viewModel.viewState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resultBus = NavigationResultEventBus.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    AuthenticationHandler(
        onAuthenticatedIntent = viewModel::onAuthenticationIntent,
        authenticationSideEffect = viewModel.authenticationSideEffect,
    )

    ResourceFormScreen(
        modifier = modifier,
        state = state.value,
        onIntent = viewModel::onIntent,
    )

    SideEffectDispatcher(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            is NavigateToPassword ->
                navigator.navigateToKey(PasswordForm(sideEffect.mode, sideEffect.passwordUiModel))
            is NavigateToTotp ->
                navigator.navigateToKey(TotpForm(sideEffect.mode, sideEffect.totpUiModel))
            is NavigateToTotpAdvancedSettings ->
                navigator.navigateToKey(TotpAdvancedSettingsForm(sideEffect.mode, sideEffect.totpUiModel))
            is NavigateToNote ->
                navigator.navigateToKey(NoteForm(sideEffect.mode, sideEffect.note))
            is NavigateToDescription ->
                navigator.navigateToKey(DescriptionForm(sideEffect.mode, sideEffect.metadataDescription))
            is NavigateToAdditionalUris ->
                navigator.navigateToKey(AdditionalUrisForm(sideEffect.mode, sideEffect.model))
            is NavigateToAppearance ->
                navigator.navigateToKey(AppearanceForm(sideEffect.mode, sideEffect.appearanceModel))
            is NavigateToCustomFields ->
                navigator.navigateToKey(
                    CustomFieldsForm(mode = sideEffect.mode, customFieldsUiModel = sideEffect.model),
                )
            NavigateToScanOtp ->
                navigator.navigateToKey(ScanOtp(ScanOtpMode.SCAN_FOR_RESULT))
            is NavigateBackWithCreateSuccess -> {
                resultBus.sendResult(
                    result =
                        ResourceFormCompleteResult(
                            resourceCreated = true,
                            resourceEdited = false,
                            resourceName = sideEffect.name,
                        ),
                )
                navigator.navigateBack()
            }
            is NavigateBackWithEditSuccess -> {
                resultBus.sendResult(
                    result =
                        ResourceFormCompleteResult(
                            resourceCreated = false,
                            resourceEdited = true,
                            resourceName = sideEffect.name,
                        ),
                )
                navigator.navigateBack()
            }
            NavigateBack -> navigator.navigateBack()
            is ShowSnackbar ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(getSnackbarMessage(context, sideEffect.type))
                }
            is ShowToast ->
                Toast
                    .makeText(
                        context,
                        getToastMessage(context, sideEffect.type, sideEffect.args),
                        Toast.LENGTH_LONG,
                    ).show()
        }
    }
}

@Composable
private fun ResourceFormScreen(
    state: ResourceFormState,
    onIntent: (ResourceFormIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Scaffold(
        modifier = modifier,
        topBar = {
            TitleAppBar(
                title = getScreenTitle(context, state),
                navigationIcon = { BackNavigationIcon(onBackClick = { onIntent(GoBack) }) },
            )
        },
        bottomBar = {
            if (state.isPrimaryButtonVisible) {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.background,
                ) {
                    PrimaryButton(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        text = getPrimaryButtonText(context, state.mode),
                        onClick = {
                            when (state.mode) {
                                is Create -> onIntent(CreateResource)
                                is Edit -> onIntent(UpdateResource)
                                null -> {}
                            }
                        },
                    )
                }
            }
        },
    ) { paddingValues ->
        if (state.shouldShowScreenProgress) {
            Box(
                modifier =
                    Modifier
                        .padding(paddingValues)
                        .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier =
                    Modifier
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
            ) {
                TextInput(
                    title = stringResource(LocalizationR.string.resource_form_resource_name),
                    hint = stringResource(LocalizationR.string.resource_form_name),
                    text = state.name,
                    onTextChange = { onIntent(NameTextChanged(it)) },
                )

                Spacer(modifier = Modifier.height(16.dp))

                LeadingContent(
                    leadingContentType = state.leadingContentType,
                    passwordData = state.passwordData,
                    totpData = state.totpData,
                    noteData = state.noteData,
                    onIntent = onIntent,
                )

                if (state.areAdvancedSettingsVisible) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(LocalizationR.string.resource_form_view_advanced_settings),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { onIntent(ExpandAdvancedSettings) }
                                .padding(vertical = 24.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }

                if (state.supportedAdditionalSecrets.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    AdditionalSecretsSection(
                        secrets = state.supportedAdditionalSecrets,
                        onIntent = onIntent,
                    )
                }

                if (state.supportedMetadata.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    MetadataSection(
                        metadata = state.supportedMetadata,
                        onIntent = onIntent,
                    )
                }
            }
        }

        ProgressDialog(isVisible = state.shouldShowDialogProgress)

        state.metadataKeyModifiedDialog?.let { model ->
            NewMetadataKeyTrustDialog(
                newKeyToTrustModel = model,
                onTrustClick = { onIntent(TrustNewMetadataKey(model)) },
                onDismiss = { onIntent(DismissMetadataKeyDialog) },
            )
        }

        state.metadataKeyDeletedDialog?.let { model ->
            TrustedMetadataKeyDeletedDialog(
                trustedKeyDeletedModel = model,
                onTrustClick = { onIntent(TrustedMetadataKeyDeleted) },
                onDismiss = { onIntent(DismissMetadataKeyDialog) },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ResourceFormScreenCreatePreview() {
    PassboltTheme {
        ResourceFormScreen(
            state =
                ResourceFormState(
                    mode = Create(LeadingContentType.PASSWORD, null),
                    name = "Passbolt",
                    shouldShowScreenProgress = false,
                    isPrimaryButtonVisible = true,
                    leadingContentType = LeadingContentType.PASSWORD,
                    passwordData =
                        PasswordData(
                            password = "p@ssb0lt!",
                            passwordStrength = PasswordStrength.Strong,
                            passwordEntropyBits = 60.0,
                            mainUri = "https://passbolt.com",
                            username = "ada@passbolt.com",
                        ),
                    areAdvancedSettingsVisible = true,
                    supportedAdditionalSecrets =
                        listOf(
                            ResourceFormUiModel.Secret.NOTE,
                            ResourceFormUiModel.Secret.TOTP,
                        ),
                    supportedMetadata =
                        listOf(
                            ResourceFormUiModel.Metadata.DESCRIPTION,
                            ResourceFormUiModel.Metadata.ADDITIONAL_URIS,
                            ResourceFormUiModel.Metadata.APPEARANCE,
                        ),
                ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ResourceFormScreenEditPreview() {
    PassboltTheme {
        ResourceFormScreen(
            state =
                ResourceFormState(
                    mode = Edit(resourceId = "1", resourceName = "Passbolt"),
                    name = "Passbolt",
                    shouldShowScreenProgress = false,
                    isPrimaryButtonVisible = true,
                    leadingContentType = LeadingContentType.PASSWORD,
                    passwordData =
                        PasswordData(
                            password = "p@ssb0lt!",
                            passwordStrength = PasswordStrength.Strong,
                            passwordEntropyBits = 60.0,
                            mainUri = "https://passbolt.com",
                            username = "ada@passbolt.com",
                        ),
                ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ResourceFormScreenInitializingPreview() {
    PassboltTheme {
        ResourceFormScreen(
            state =
                ResourceFormState(
                    mode = Create(LeadingContentType.PASSWORD, null),
                    shouldShowScreenProgress = true,
                ),
            onIntent = {},
        )
    }
}

private fun getSnackbarMessage(
    context: Context,
    type: SnackbarMessage,
): String =
    context.getString(
        when (type) {
            SnackbarMessage.COMMON_FAILURE -> LocalizationR.string.common_failure
            SnackbarMessage.CANNOT_CREATE_RESOURCE_WITH_CURRENT_CONFIG ->
                LocalizationR.string.common_cannot_create_resource_with_current_config
            SnackbarMessage.METADATA_KEY_VERIFICATION_FAILURE ->
                LocalizationR.string.common_metadata_key_verification_failure
            SnackbarMessage.JSON_SCHEMA_RESOURCE_VALIDATION_ERROR ->
                LocalizationR.string.common_json_schema_resource_validation_error
            SnackbarMessage.JSON_SCHEMA_SECRET_VALIDATION_ERROR ->
                LocalizationR.string.common_json_schema_secret_validation_error
            SnackbarMessage.METADATA_KEY_TRUST_FAILED -> LocalizationR.string.common_metadata_key_trust_failed
            SnackbarMessage.ENCRYPTION_FAILURE -> LocalizationR.string.common_encryption_failure
            SnackbarMessage.METADATA_KEY_IS_TRUSTED -> LocalizationR.string.common_metadata_key_is_trusted
        },
    )

@Suppress("SpreadOperator")
private fun getToastMessage(
    context: Context,
    type: ToastMessage,
    args: List<Any>,
): String =
    context.getString(
        when (type) {
            ToastMessage.UNABLE_TO_GENERATE_PASSWORD ->
                LocalizationR.string.dialog_unable_to_generate_password_message
            ToastMessage.CREATE_INITIALIZATION_ERROR -> LocalizationR.string.resource_form_create_init_error
            ToastMessage.EDIT_INITIALIZATION_ERROR -> LocalizationR.string.resource_form_edit_init_error
        },
        *args.toTypedArray(),
    )
