package com.passbolt.mobile.android.feature.resourcedetails.details.ui

import PassboltTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.core.ui.header.ActionIcon
import com.passbolt.mobile.android.core.ui.header.ItemWithHeader
import com.passbolt.mobile.android.core.ui.header.ValueStyle
import com.passbolt.mobile.android.core.ui.section.Section
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsIntent
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsIntent.CopyCustomField
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsIntent.ToggleCustomField
import com.passbolt.mobile.android.ui.CustomFieldModel
import com.passbolt.mobile.android.ui.CustomFieldModel.BooleanCustomField
import com.passbolt.mobile.android.ui.CustomFieldModel.NumberCustomField
import com.passbolt.mobile.android.ui.CustomFieldModel.PasswordCustomField
import com.passbolt.mobile.android.ui.CustomFieldModel.TextCustomField
import com.passbolt.mobile.android.ui.CustomFieldModel.UriCustomField
import java.util.UUID
import com.passbolt.mobile.android.core.localization.R as LocalizationR

@Suppress("CyclomaticComplexMethod")
@Composable
internal fun CustomFieldsSection(
    customFields: Map<UUID, String>,
    visibleCustomFields: Map<UUID, CustomFieldModel?>,
    onIntent: (ResourceDetailsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Section(title = stringResource(LocalizationR.string.resource_details_custom_fields_header), modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            customFields.forEach { (key, label) ->
                val isVisible = visibleCustomFields.containsKey(key)
                val fieldModel = visibleCustomFields[key]
                val displayValue =
                    if (isVisible) {
                        when (fieldModel) {
                            is BooleanCustomField -> fieldModel.secretValue?.toString()
                            is NumberCustomField -> fieldModel.secretValue?.toString()
                            is PasswordCustomField -> fieldModel.secretValue
                            is UriCustomField -> fieldModel.secretValue
                            is TextCustomField -> fieldModel.secretValue
                            null -> null
                        }.orEmpty()
                    } else {
                        ""
                    }
                ItemWithHeader(
                    headerText = label,
                    value = displayValue,
                    valueStyle = ValueStyle.Secret(isRevealed = isVisible),
                    actionIcon = if (isVisible) ActionIcon.HIDE else ActionIcon.VIEW,
                    onItemClick = { onIntent(CopyCustomField(key)) },
                    onActionClick = { onIntent(ToggleCustomField(key)) },
                )
            }
        }
    }
}

private val previewField1Id = UUID.randomUUID()
private val previewField2Id = UUID.randomUUID()
private val previewField3Id = UUID.randomUUID()

@Preview(showBackground = true)
@Composable
private fun CustomFieldsSectionHiddenPreview() {
    PassboltTheme {
        CustomFieldsSection(
            customFields =
                mapOf(
                    previewField1Id to "API Key",
                    previewField2Id to "Security Question",
                    previewField3Id to "Recovery Code",
                ),
            visibleCustomFields = emptyMap(),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CustomFieldsSectionVisiblePreview() {
    PassboltTheme {
        CustomFieldsSection(
            customFields =
                mapOf(
                    previewField1Id to "API Key",
                    previewField2Id to "Security Question",
                    previewField3Id to "Recovery Code",
                ),
            visibleCustomFields =
                mapOf(
                    previewField1Id to
                        TextCustomField(
                            metadataKey = "API Key",
                            secretValue = "sk-1234567890abcdef",
                            id = UUID.randomUUID(),
                        ),
                    previewField2Id to
                        TextCustomField(
                            metadataKey = "Security Question",
                            secretValue = "My first pet's name",
                            id = UUID.randomUUID(),
                        ),
                ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CustomFieldsSectionDarkPreview() {
    PassboltTheme(darkTheme = true) {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            CustomFieldsSection(
                customFields =
                    mapOf(
                        previewField1Id to "API Key",
                        previewField2Id to "Security Question",
                    ),
                visibleCustomFields =
                    mapOf(
                        previewField1Id to
                            TextCustomField(
                                metadataKey = "API Key",
                                secretValue = "sk-1234567890abcdef",
                                id = UUID.randomUUID(),
                            ),
                    ),
                onIntent = {},
            )
        }
    }
}
