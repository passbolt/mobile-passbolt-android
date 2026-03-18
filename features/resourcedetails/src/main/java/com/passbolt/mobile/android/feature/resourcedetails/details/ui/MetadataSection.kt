package com.passbolt.mobile.android.feature.resourcedetails.details.ui

import PassboltTheme
import android.text.format.DateUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.core.ui.compose.header.ItemWithHeader
import com.passbolt.mobile.android.core.ui.compose.section.Section
import com.passbolt.mobile.android.core.ui.compose.text.SeparatedText
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsIntent
import com.passbolt.mobile.android.feature.resourcedetails.details.ui.metadata.AdditionalUrisItem
import com.passbolt.mobile.android.feature.resourcedetails.details.ui.metadata.TagsItem
import java.time.ZonedDateTime
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
internal fun MetadataSection(
    showMetadataDescriptionSection: Boolean,
    canViewLocation: Boolean,
    canViewTags: Boolean,
    metadataDescription: String,
    additionalUris: List<String>,
    tags: List<String>,
    locationPath: List<String>,
    expiry: ZonedDateTime?,
    onIntent: (ResourceDetailsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Section(title = stringResource(LocalizationR.string.resource_details_metadata_header), modifier = modifier) {
        Column {
            if (showMetadataDescriptionSection) {
                ItemWithHeader(
                    headerText = stringResource(LocalizationR.string.resource_details_description_header),
                    value = metadataDescription,
                    isTextSelectable = true,
                )
            }

            if (canViewLocation) {
                ItemWithHeader(
                    headerText = stringResource(LocalizationR.string.location),
                    onItemClick = { onIntent(ResourceDetailsIntent.GoToLocation) },
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SeparatedText(
                            segments = listOf(stringResource(LocalizationR.string.folder_root)) + locationPath,
                            modifier = Modifier.weight(1f),
                        )
                        Image(
                            painter = painterResource(CoreUiR.drawable.ic_chevron_right),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            if (canViewTags) {
                ItemWithHeader(
                    headerText = stringResource(LocalizationR.string.resource_details_tags_header),
                    onItemClick = { onIntent(ResourceDetailsIntent.GoToTags) },
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TagsItem(
                            tags = tags,
                            modifier = Modifier.weight(1f),
                        )
                        Image(
                            painter = painterResource(CoreUiR.drawable.ic_chevron_right),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            if (additionalUris.isNotEmpty()) {
                AdditionalUrisItem(urls = additionalUris, modifier = Modifier.padding(top = 16.dp))
            }

            expiry?.let {
                val expiryText =
                    DateUtils
                        .getRelativeTimeSpanString(
                            it.toInstant().toEpochMilli(),
                            ZonedDateTime.now().toInstant().toEpochMilli(),
                            DateUtils.MINUTE_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_RELATIVE,
                        ).toString()

                ItemWithHeader(
                    headerText = stringResource(LocalizationR.string.resource_details_expiry_header),
                    value = expiryText,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MetadataSectionDescriptionPreview() {
    PassboltTheme {
        MetadataSection(
            showMetadataDescriptionSection = true,
            canViewLocation = false,
            canViewTags = false,
            metadataDescription = "This is a sample resource description with important details.",
            additionalUris = emptyList(),
            tags = emptyList(),
            locationPath = listOf("Projects", "Mobile", "Android"),
            expiry = null,
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MetadataSectionLocationPreview() {
    PassboltTheme {
        MetadataSection(
            showMetadataDescriptionSection = false,
            canViewLocation = true,
            canViewTags = false,
            metadataDescription = "",
            additionalUris = emptyList(),
            tags = emptyList(),
            locationPath = listOf("Projects", "Mobile", "Android"),
            expiry = null,
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MetadataSectionTagsPreview() {
    PassboltTheme {
        MetadataSection(
            showMetadataDescriptionSection = false,
            canViewLocation = false,
            canViewTags = true,
            metadataDescription = "",
            additionalUris = emptyList(),
            tags = listOf("work", "important", "api", "production"),
            locationPath = listOf("Projects", "Mobile", "Android"),
            expiry = null,
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MetadataSectionFullPreview() {
    PassboltTheme {
        MetadataSection(
            showMetadataDescriptionSection = true,
            canViewLocation = true,
            canViewTags = true,
            metadataDescription = "This is a sample resource description.",
            additionalUris = listOf("https://api.example.com", "https://staging.example.com"),
            tags = listOf("work", "important"),
            locationPath = listOf("Projects", "Mobile", "Android"),
            expiry = ZonedDateTime.now().plusDays(30),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun MetadataSectionDarkPreview() {
    PassboltTheme(darkTheme = true) {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            MetadataSection(
                showMetadataDescriptionSection = true,
                canViewLocation = true,
                canViewTags = true,
                metadataDescription = "This is a sample resource description.",
                additionalUris = emptyList(),
                tags = listOf("work", "important"),
                locationPath = listOf("Projects", "Mobile", "Android"),
                expiry = null,
                onIntent = {},
            )
        }
    }
}
