package com.passbolt.mobile.android.core.navigation.compose.keys

import androidx.navigation3.runtime.NavKey
import com.passbolt.mobile.android.ui.AdditionalUrisUiModel
import com.passbolt.mobile.android.ui.CustomFieldsUiModel
import com.passbolt.mobile.android.ui.PasswordUiModel
import com.passbolt.mobile.android.ui.ResourceAppearanceModel
import com.passbolt.mobile.android.ui.ResourceFormMode
import com.passbolt.mobile.android.ui.TotpUiModel
import kotlinx.serialization.Serializable

sealed interface ResourceFormNavigationKey : NavKey {
    @Serializable
    data class MainResourceForm(
        val mode: ResourceFormMode,
    ) : ResourceFormNavigationKey

    @Serializable
    data class PasswordForm(
        val mode: ResourceFormMode,
        val passwordModel: PasswordUiModel,
    ) : ResourceFormNavigationKey

    @Serializable
    data class TotpForm(
        val mode: ResourceFormMode,
        val totpUiModel: TotpUiModel,
    ) : ResourceFormNavigationKey

    @Serializable
    data class TotpAdvancedSettingsForm(
        val mode: ResourceFormMode,
        val totpUiModel: TotpUiModel,
    ) : ResourceFormNavigationKey

    @Serializable
    data class NoteForm(
        val mode: ResourceFormMode,
        val note: String,
    ) : ResourceFormNavigationKey

    @Serializable
    data class DescriptionForm(
        val mode: ResourceFormMode,
        val metadataDescription: String,
    ) : ResourceFormNavigationKey

    @Serializable
    data class AdditionalUrisForm(
        val mode: ResourceFormMode,
        val additionalUris: AdditionalUrisUiModel,
    ) : ResourceFormNavigationKey

    @Serializable
    data class AppearanceForm(
        val mode: ResourceFormMode,
        val appearanceModel: ResourceAppearanceModel,
    ) : ResourceFormNavigationKey

    @Serializable
    data class CustomFieldsForm(
        val mode: ResourceFormMode,
        val customFieldsUiModel: CustomFieldsUiModel,
    ) : ResourceFormNavigationKey
}
