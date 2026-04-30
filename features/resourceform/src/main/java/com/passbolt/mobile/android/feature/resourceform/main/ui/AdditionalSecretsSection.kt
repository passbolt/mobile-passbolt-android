package com.passbolt.mobile.android.feature.resourceform.main.ui

import PassboltTheme
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.core.ui.section.Section
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.GoToAdditionalNote
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.GoToAdditionalPassword
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.GoToAdditionalTotp
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.GoToCustomFields
import com.passbolt.mobile.android.ui.ResourceFormUiModel
import com.passbolt.mobile.android.ui.ResourceFormUiModel.Secret.CUSTOM_FIELDS
import com.passbolt.mobile.android.ui.ResourceFormUiModel.Secret.NOTE
import com.passbolt.mobile.android.ui.ResourceFormUiModel.Secret.PASSWORD
import com.passbolt.mobile.android.ui.ResourceFormUiModel.Secret.TOTP
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
internal fun AdditionalSecretsSection(
    secrets: List<ResourceFormUiModel.Secret>,
    onIntent: (ResourceFormIntent) -> Unit,
) {
    val context = LocalContext.current

    Section(title = stringResource(LocalizationR.string.resource_form_view_additional_secrets)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            secrets.forEach { secret ->
                with(
                    secretToSettingRowItem(
                        context = context,
                        secret = secret,
                        onIntent = onIntent,
                    ),
                ) {
                    SettingRow(
                        leadingIconResId = iconResId,
                        text = text,
                        onClick = onClick,
                    )
                }
            }
        }
    }
}

private fun secretToSettingRowItem(
    context: Context,
    secret: ResourceFormUiModel.Secret,
    onIntent: (ResourceFormIntent) -> Unit,
): SettingRowItem =
    when (secret) {
        PASSWORD ->
            SettingRowItem(
                text = context.getString(LocalizationR.string.resource_form_password),
                iconResId = CoreUiR.drawable.ic_key,
                onClick = { onIntent(GoToAdditionalPassword) },
            )
        NOTE ->
            SettingRowItem(
                text = context.getString(LocalizationR.string.resource_form_note),
                iconResId = CoreUiR.drawable.ic_notes,
                onClick = { onIntent(GoToAdditionalNote) },
            )
        TOTP ->
            SettingRowItem(
                text = context.getString(LocalizationR.string.resource_form_totp),
                iconResId = CoreUiR.drawable.ic_time_lock,
                onClick = { onIntent(GoToAdditionalTotp) },
            )
        CUSTOM_FIELDS ->
            SettingRowItem(
                text = context.getString(LocalizationR.string.resource_form_custom_fields),
                iconResId = CoreUiR.drawable.ic_custom_fields,
                onClick = { onIntent(GoToCustomFields) },
            )
    }

@Preview(showBackground = true)
@Composable
private fun AdditionalSecretsSectionPreview() {
    PassboltTheme {
        AdditionalSecretsSection(
            secrets =
                listOf(
                    PASSWORD,
                    NOTE,
                    TOTP,
                    CUSTOM_FIELDS,
                ),
            onIntent = {},
        )
    }
}
