/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2026 Passbolt SA
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * The name "Passbolt" is a registered trademark of Passbolt SA, and Passbolt SA hereby declines to grant a trademark
 * license to "Passbolt" pursuant to the GNU Affero General Public License version 3 Section 7(e), without a separate
 * agreement with Passbolt SA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Passbolt SA (https://www.passbolt.com)
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://www.passbolt.com Passbolt (tm)
 * @since v1.0
 */

package com.passbolt.mobile.android.feature.resourceform.additionalsecrets.note

import PassboltTheme
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.ui.compose.button.PrimaryButton
import com.passbolt.mobile.android.core.ui.compose.text.TextInput
import com.passbolt.mobile.android.core.ui.compose.topbar.BackNavigationIcon
import com.passbolt.mobile.android.core.ui.compose.topbar.TitleAppBar
import com.passbolt.mobile.android.core.ui.textinputfield.StatefulInput.State.Default
import com.passbolt.mobile.android.core.ui.textinputfield.StatefulInput.State.Error
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.note.NoteFormIntent.ApplyChanges
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.note.NoteFormIntent.GoBack
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.note.NoteFormIntent.NoteTextChanged
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.note.NoteFormIntent.RemoveNote
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.note.NoteFormSideEffect.ApplyAndGoBack
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.note.NoteFormSideEffect.NavigateBack
import com.passbolt.mobile.android.ui.LeadingContentType
import com.passbolt.mobile.android.ui.ResourceFormMode
import com.passbolt.mobile.android.ui.ResourceFormMode.Create
import com.passbolt.mobile.android.ui.ResourceFormMode.Edit
import org.koin.androidx.compose.koinViewModel
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
internal fun NoteFormScreen(
    navigation: NoteFormNavigation,
    modifier: Modifier = Modifier,
    viewModel: NoteFormViewModel = koinViewModel(),
) {
    val state = viewModel.viewState.collectAsStateWithLifecycle()

    NoteFormScreen(
        modifier = modifier,
        state = state.value,
        onIntent = viewModel::onIntent,
    )

    SideEffectDispatcher(viewModel.sideEffect) {
        when (it) {
            is ApplyAndGoBack -> navigation.navigateBackWithResult(it.note)
            NavigateBack -> navigation.navigateBack()
        }
    }
}

@Composable
private fun NoteFormScreen(
    onIntent: (NoteFormIntent) -> Unit,
    state: NoteFormState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val sectionColor = colorResource(CoreUiR.color.section_background)

    Scaffold(
        modifier = modifier,
        topBar = {
            TitleAppBar(
                title = getScreenTitle(context, state.resourceFormMode),
                navigationIcon = { BackNavigationIcon(onBackClick = { onIntent(GoBack) }) },
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.background,
            ) {
                PrimaryButton(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(LocalizationR.string.apply),
                    onClick = { onIntent(ApplyChanges) },
                )
            }
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
        ) {
            Text(
                text = stringResource(LocalizationR.string.resource_form_note),
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(sectionColor)
                        .padding(12.dp),
            ) {
                TextInput(
                    title = stringResource(LocalizationR.string.resource_form_note_content),
                    hint = stringResource(LocalizationR.string.resource_form_enter_note),
                    text = state.note,
                    onTextChange = { onIntent(NoteTextChanged(it)) },
                    state =
                        if (state.noteValidationErrors.isEmpty()) {
                            Default
                        } else {
                            Error(getNoteErrorMessage(context, state.noteValidationErrors))
                        },
                    minLines = 3,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(LocalizationR.string.resource_form_note_info),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier =
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .clickable { onIntent(RemoveNote) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(CoreUiR.drawable.ic_trash),
                    contentDescription = null,
                    tint = colorResource(CoreUiR.color.icon_tint),
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = stringResource(LocalizationR.string.resource_form_remove_note),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}

private fun getScreenTitle(
    context: Context,
    resourceFormMode: ResourceFormMode?,
): String =
    when (resourceFormMode) {
        is Create -> context.getString(LocalizationR.string.resource_form_create_note)
        is Edit -> context.getString(LocalizationR.string.resource_form_edit_resource, resourceFormMode.resourceName)
        null -> ""
    }

private fun getNoteErrorMessage(
    context: Context,
    noteValidationErrors: List<NoteValidationError>,
): String =
    when (val error = noteValidationErrors.first()) {
        is NoteValidationError.MaxLengthExceeded ->
            context.getString(LocalizationR.string.validation_max_length, error.maxLength)
    }

@Preview(showBackground = true)
@Composable
private fun NoteFormScreenPreview() {
    PassboltTheme {
        NoteFormScreen(
            onIntent = {},
            state =
                NoteFormState(
                    resourceFormMode =
                        Create(
                            leadingContentType = LeadingContentType.PASSWORD,
                            parentFolderId = null,
                        ),
                    note = "This is a sample note",
                ),
        )
    }
}
