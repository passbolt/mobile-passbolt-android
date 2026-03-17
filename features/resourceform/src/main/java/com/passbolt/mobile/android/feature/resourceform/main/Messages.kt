package com.passbolt.mobile.android.feature.resourceform.main

import android.content.Context
import com.passbolt.mobile.android.common.extension.toSingleLine
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.note.NoteValidationError
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.note.NoteValidationError.MaxLengthExceeded
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpSecretValidationError
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpSecretValidationError.MustBeBase32
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpSecretValidationError.MustNotBeEmpty
import com.passbolt.mobile.android.ui.LeadingContentType.CUSTOM_FIELDS
import com.passbolt.mobile.android.ui.LeadingContentType.PASSWORD
import com.passbolt.mobile.android.ui.LeadingContentType.STANDALONE_NOTE
import com.passbolt.mobile.android.ui.LeadingContentType.TOTP
import com.passbolt.mobile.android.ui.ResourceFormMode
import com.passbolt.mobile.android.ui.ResourceFormMode.Create
import com.passbolt.mobile.android.ui.ResourceFormMode.Edit
import com.passbolt.mobile.android.core.localization.R as LocalizationR

internal fun getScreenTitle(
    context: Context,
    state: ResourceFormState,
): String =
    when (val mode = state.mode) {
        is Create ->
            when (state.leadingContentType) {
                TOTP -> context.getString(LocalizationR.string.resource_form_create_totp)
                PASSWORD -> context.getString(LocalizationR.string.resource_form_create_password)
                CUSTOM_FIELDS -> context.getString(LocalizationR.string.resource_form_create_custom_fields)
                STANDALONE_NOTE -> context.getString(LocalizationR.string.resource_form_create_note)
                null -> ""
            }
        is Edit -> context.getString(LocalizationR.string.resource_form_edit_resource, mode.resourceName.toSingleLine())
        null -> ""
    }

internal fun getPrimaryButtonText(
    context: Context,
    mode: ResourceFormMode?,
): String =
    when (mode) {
        is Create -> context.getString(LocalizationR.string.resource_form_create)
        is Edit -> context.getString(LocalizationR.string.resource_form_save)
        null -> ""
    }

internal fun getTotpSecretErrorMessage(
    context: Context,
    error: TotpSecretValidationError,
): String =
    when (error) {
        MustNotBeEmpty -> context.getString(LocalizationR.string.validation_is_required)
        MustBeBase32 -> context.getString(LocalizationR.string.validation_invalid_totp_secret)
    }

internal fun getNoteErrorMessage(
    context: Context,
    error: NoteValidationError,
): String =
    when (error) {
        is MaxLengthExceeded -> context.getString(LocalizationR.string.validation_max_length, error.maxLength)
    }
