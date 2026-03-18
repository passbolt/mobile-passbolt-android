package com.passbolt.mobile.android.feature.resourceform.main

import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.note.NoteValidationError
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpSecretValidationError
import com.passbolt.mobile.android.ui.LeadingContentType
import com.passbolt.mobile.android.ui.NewMetadataKeyToTrustModel
import com.passbolt.mobile.android.ui.PasswordStrength
import com.passbolt.mobile.android.ui.ResourceFormMode
import com.passbolt.mobile.android.ui.ResourceFormUiModel
import com.passbolt.mobile.android.ui.TotpUiModel
import com.passbolt.mobile.android.ui.TrustedKeyDeletedModel

data class ResourceFormState(
    val mode: ResourceFormMode? = null,
    val name: String = "",
    val shouldShowScreenProgress: Boolean = true,
    val shouldShowDialogProgress: Boolean = false,
    val leadingContentType: LeadingContentType? = null,
    val isPrimaryButtonVisible: Boolean = false,
    val areAdvancedSettingsExpanded: Boolean = false,
    val areAdvancedSettingsVisible: Boolean = true,
    val supportedAdditionalSecrets: List<ResourceFormUiModel.Secret> = emptyList(),
    val supportedMetadata: List<ResourceFormUiModel.Metadata> = emptyList(),
    val passwordData: PasswordData = PasswordData(),
    val totpData: TotpData = TotpData(),
    val noteData: NoteData = NoteData(),
    val metadataKeyModifiedDialog: NewMetadataKeyToTrustModel? = null,
    val metadataKeyDeletedDialog: TrustedKeyDeletedModel? = null,
)

data class PasswordData(
    val password: String = "",
    val passwordStrength: PasswordStrength = PasswordStrength.Empty,
    val passwordEntropyBits: Double = 0.0,
    val mainUri: String = "",
    val username: String = "",
)

data class TotpData(
    val totpSecret: String = "",
    val totpIssuer: String = "",
    val totpUiModel: TotpUiModel? = null,
    val totpSecretError: TotpSecretValidationError? = null,
)

data class NoteData(
    val note: String = "",
    val noteError: NoteValidationError? = null,
)
