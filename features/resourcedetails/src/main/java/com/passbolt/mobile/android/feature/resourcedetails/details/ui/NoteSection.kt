package com.passbolt.mobile.android.feature.resourcedetails.details.ui

import PassboltTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.passbolt.mobile.android.core.localization.R
import com.passbolt.mobile.android.core.ui.header.ActionIcon
import com.passbolt.mobile.android.core.ui.header.ItemWithHeader
import com.passbolt.mobile.android.core.ui.header.ValueStyle
import com.passbolt.mobile.android.core.ui.section.Section
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsIntent
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsIntent.CopyNote
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsIntent.ToggleNoteVisibility

@Composable
internal fun NoteSection(
    note: String,
    isNoteVisible: Boolean,
    onIntent: (ResourceDetailsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Section(title = stringResource(R.string.resource_details_note_header), modifier = modifier) {
        ItemWithHeader(
            headerText = stringResource(R.string.resource_details_note_content),
            value = if (isNoteVisible) note else "",
            valueStyle = if (isNoteVisible) ValueStyle.Plain else ValueStyle.Concealed,
            actionIcon = if (isNoteVisible) ActionIcon.HIDE else ActionIcon.VIEW,
            isTextSelectable = isNoteVisible,
            onItemClick = { onIntent(CopyNote) },
            onActionClick = { onIntent(ToggleNoteVisibility) },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NoteSectionConcealedPreview() {
    PassboltTheme {
        NoteSection(
            note = "This is a secret note with sensitive information.",
            isNoteVisible = false,
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NoteSectionUnconcealedPreview() {
    PassboltTheme {
        NoteSection(
            note = "This is a secret note with sensitive information.\nIt can have multiple lines.",
            isNoteVisible = true,
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun NoteSectionDarkPreview() {
    PassboltTheme(darkTheme = true) {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            NoteSection(
                note = "This is a secret note with sensitive information.",
                isNoteVisible = true,
                onIntent = {},
            )
        }
    }
}
