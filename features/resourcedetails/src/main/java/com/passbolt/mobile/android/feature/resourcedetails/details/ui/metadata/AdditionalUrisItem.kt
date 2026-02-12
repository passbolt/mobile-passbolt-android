package com.passbolt.mobile.android.feature.resourcedetails.details.ui.metadata

import PassboltTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.passbolt.mobile.android.core.localization.R
import com.passbolt.mobile.android.core.ui.compose.header.ItemWithHeader

@Composable
internal fun AdditionalUrisItem(
    urls: List<String>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val formattedUrls = urls.joinToString(separator = "\n") { url -> context.getString(R.string.additional_uri_format, url) }

    ItemWithHeader(
        headerText = stringResource(R.string.resource_details_additional_uris),
        value = formattedUrls,
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun AdditionalUrisItemPreview() {
    PassboltTheme {
        AdditionalUrisItem(
            urls = listOf("https://api.example.com", "https://staging.example.com"),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AdditionalUrisItemDarkPreview() {
    PassboltTheme(darkTheme = true) {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            AdditionalUrisItem(
                urls = listOf("https://api.example.com", "https://staging.example.com", "https://dev.example.com"),
            )
        }
    }
}
