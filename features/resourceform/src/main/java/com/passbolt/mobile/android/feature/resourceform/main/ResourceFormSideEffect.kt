package com.passbolt.mobile.android.feature.resourceform.main

import com.passbolt.mobile.android.ui.AdditionalUrisUiModel
import com.passbolt.mobile.android.ui.CustomFieldsUiModel
import com.passbolt.mobile.android.ui.PasswordUiModel
import com.passbolt.mobile.android.ui.ResourceAppearanceModel
import com.passbolt.mobile.android.ui.ResourceFormMode
import com.passbolt.mobile.android.ui.TotpUiModel

sealed interface ResourceFormSideEffect {
    data class NavigateToPassword(
        val mode: ResourceFormMode,
        val passwordUiModel: PasswordUiModel,
    ) : ResourceFormSideEffect

    data class NavigateToTotp(
        val mode: ResourceFormMode,
        val totpUiModel: TotpUiModel,
    ) : ResourceFormSideEffect

    data class NavigateToTotpAdvancedSettings(
        val mode: ResourceFormMode,
        val totpUiModel: TotpUiModel,
    ) : ResourceFormSideEffect

    data class NavigateToNote(
        val mode: ResourceFormMode,
        val note: String,
    ) : ResourceFormSideEffect

    data class NavigateToDescription(
        val mode: ResourceFormMode,
        val metadataDescription: String,
    ) : ResourceFormSideEffect

    data class NavigateToAdditionalUris(
        val mode: ResourceFormMode,
        val model: AdditionalUrisUiModel,
    ) : ResourceFormSideEffect

    data class NavigateToAppearance(
        val mode: ResourceFormMode,
        val appearanceModel: ResourceAppearanceModel,
    ) : ResourceFormSideEffect

    data class NavigateToCustomFields(
        val mode: ResourceFormMode,
        val model: CustomFieldsUiModel,
    ) : ResourceFormSideEffect

    data object NavigateToScanOtp : ResourceFormSideEffect

    data class NavigateBackWithCreateSuccess(
        val name: String,
        val resourceId: String,
    ) : ResourceFormSideEffect

    data class NavigateBackWithEditSuccess(
        val name: String,
    ) : ResourceFormSideEffect

    data object NavigateBack : ResourceFormSideEffect

    data class ShowSnackbar(
        val type: SnackbarMessage,
    ) : ResourceFormSideEffect

    data class ShowToast(
        val type: ToastMessage,
        val args: List<Any> = emptyList(),
    ) : ResourceFormSideEffect
}

enum class SnackbarMessage {
    COMMON_FAILURE,
    CANNOT_CREATE_RESOURCE_WITH_CURRENT_CONFIG,
    METADATA_KEY_VERIFICATION_FAILURE,
    JSON_SCHEMA_RESOURCE_VALIDATION_ERROR,
    JSON_SCHEMA_SECRET_VALIDATION_ERROR,
    METADATA_KEY_TRUST_FAILED,
    ENCRYPTION_FAILURE,
    METADATA_KEY_IS_TRUSTED,
}

enum class ToastMessage {
    UNABLE_TO_GENERATE_PASSWORD,
    CREATE_INITIALIZATION_ERROR,
    EDIT_INITIALIZATION_ERROR,
}
