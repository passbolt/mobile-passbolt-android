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
import com.passbolt.mobile.android.core.ui.compose.section.Section
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.GoToAdditionalUris
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.GoToAppearance
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.GoToMetadataDescription
import com.passbolt.mobile.android.ui.ResourceFormUiModel
import com.passbolt.mobile.android.ui.ResourceFormUiModel.Metadata.ADDITIONAL_URIS
import com.passbolt.mobile.android.ui.ResourceFormUiModel.Metadata.APPEARANCE
import com.passbolt.mobile.android.ui.ResourceFormUiModel.Metadata.DESCRIPTION
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
internal fun MetadataSection(
    metadata: List<ResourceFormUiModel.Metadata>,
    onIntent: (ResourceFormIntent) -> Unit,
) {
    val context = LocalContext.current

    Section(title = stringResource(LocalizationR.string.resource_form_view_metadata)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            metadata.forEach { metadataItem ->
                with(
                    metadataToSettingRowItem(
                        context = context,
                        metadataItem = metadataItem,
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

private fun metadataToSettingRowItem(
    context: Context,
    metadataItem: ResourceFormUiModel.Metadata,
    onIntent: (ResourceFormIntent) -> Unit,
): SettingRowItem =
    when (metadataItem) {
        DESCRIPTION ->
            SettingRowItem(
                text = context.getString(LocalizationR.string.resource_form_description),
                iconResId = CoreUiR.drawable.ic_description,
                onClick = { onIntent(GoToMetadataDescription) },
            )
        ADDITIONAL_URIS ->
            SettingRowItem(
                text = context.getString(LocalizationR.string.resource_form_additional_uris),
                iconResId = CoreUiR.drawable.ic_link,
                onClick = { onIntent(GoToAdditionalUris) },
            )
        APPEARANCE ->
            SettingRowItem(
                text = context.getString(LocalizationR.string.resource_form_appearance),
                iconResId = CoreUiR.drawable.ic_paintbrush,
                onClick = { onIntent(GoToAppearance) },
            )
    }

@Preview(showBackground = true)
@Composable
private fun MetadataSectionPreview() {
    PassboltTheme {
        MetadataSection(
            metadata =
                listOf(
                    DESCRIPTION,
                    ADDITIONAL_URIS,
                    APPEARANCE,
                ),
            onIntent = {},
        )
    }
}
