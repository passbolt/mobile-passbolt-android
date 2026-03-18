/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021-2026 Passbolt SA
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

package com.passbolt.mobile.android.helpers

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.platform.app.InstrumentationRegistry
import com.passbolt.mobile.android.testtags.composetags.Auth
import com.passbolt.mobile.android.testtags.composetags.Home
import com.passbolt.mobile.android.testtags.composetags.ResourceForm
import com.passbolt.mobile.android.core.localization.R as LocalizationR

internal fun getString(
    @StringRes stringResId: Int,
    vararg formatArgs: String? = emptyArray(),
) = InstrumentationRegistry.getInstrumentation().targetContext.getString(stringResId, *formatArgs)

/**
 * Creates a new password resource from the Home screen using the FAB -> Password flow.
 *
 * Behavior:
 * - Clicks the FAB button.
 * - Selects "Password" from the create resource menu.
 * - Fills in name, URI, username, and password fields.
 * - Clicks the save button.
 * - Waits for the home screen to be displayed.
 *
 * Assumptions:
 * - The Home screen is visible.
 * - CreateResourceIdlingResource is registered so the save operation is awaited.
 *
 * @param name The name for the new password resource.
 */
internal fun ComposeTestRule.createNewPasswordFromHomeScreen(name: String) {
    onNodeWithTag("home_fab").performClick()
    onNodeWithText(getString(LocalizationR.string.create_resource_menu_create_password)).performClick()
    waitForResourceForm()
    onNodeWithTag(ResourceForm.NAME_INPUT).performTextReplacement(name)
    onNodeWithTag(ResourceForm.URI_INPUT).performTextReplacement("TestURL")
    onNodeWithTag(ResourceForm.USERNAME_INPUT).performTextReplacement("TestUsername")
    onNodeWithTag(ResourceForm.PASSWORD_INPUT).performTextReplacement("TestPassword123!")
    onNodeWithTag(ResourceForm.SAVE_BUTTON).performClick()
    onNodeWithTag(Home.SCREEN).assertIsDisplayed()
}

/**
 * Extension variant of signIn so callers don’t need to pass the ComposeTestRule explicitly.
 *
 * Usage: composeTestRule.signIn(passphrase)
 */
internal fun ComposeTestRule.signIn(passphrase: String) {
    onNodeWithTag(Auth.PASSPHRASE_INPUT).performTextReplacement(passphrase)
    onNodeWithTag(Auth.SIGN_IN_BUTTON).performClick()
    onNodeWithTag(Home.SCREEN).assertIsDisplayed()
}

/**
 * Searches for a resource by name from the Home screen and opens the first matching result.
 *
 * Behavior:
 * - Focuses the search input (testTag: "home_search_input_field").
 * - Types the provided name.
 * - Creates matcher with any "Home" rows
 * - Waits for matched rows avoiding async operations
 * - Taps the first resource row in the results list (testTag: "home_resource_row").
 *
 * Assumptions:
 * - The Home screen is visible.
 * - Any desired filtering has been applied beforehand.
 *
 * @param name The resource name to search for.
 */
internal fun ComposeTestRule.searchAndOpenFirstResourceByName(name: String) {
    onNodeWithTag("home_search_input_field")
        .performClick()
        .performTextReplacement(name)

    val rowMatcher =
        hasTestTag("home_resource_row").and(
            hasAnyDescendant(hasText(name, substring = true, ignoreCase = true)),
        )

    waitUntil(conditionDescription = "Waiting for resource $name", timeoutMillis = 3_000) {
        onAllNodes(rowMatcher, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
    }
    onAllNodes(rowMatcher, useUnmergedTree = true)
        .onFirst()
        .performClick()
}

/**
 * Searches for a resource by name from the Home screen and opens the first matching result's More menu.
 *
 * This is a copy of `searchAndOpenFirstResourceByName` with the only difference
 * that at the end we click the row's More button instead of the whole row.
 *
 * Behavior:
 * - Focuses the search input (testTag: "home_search_input_field").
 * - Types the provided name.
 * - Creates matcher with any "Home" rows
 * - Waits for matched rows avoiding async operations
 * - Taps the first resource More button in the results list.
 *
 * Assumptions:
 * - The Home screen is visible.
 * - Any desired filtering has been applied beforehand.
 *
 * @param name The resource name to search for.
 */
internal fun ComposeTestRule.searchAndClickMoreOfFirstResource(name: String) {
    onNodeWithTag("home_search_input_field")
        .performClick()
        .performTextReplacement(name)

    val rowMatcher =
        hasTestTag("home_resource_row").and(
            hasAnyDescendant(hasText(name, substring = true, ignoreCase = true)),
        )

    waitUntil(conditionDescription = "Waiting for resource $name", timeoutMillis = 3_000) {
        onAllNodes(rowMatcher, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
    }
    onAllNodes(hasTestTag("home_resource_more"), useUnmergedTree = true)
        .onFirst()
        .performClick()
}

/**
 * Searches for a folder by name from the Home screen (Folders filter) and opens the first matching result.
 *
 * Behavior:
 * - Focuses the search input (testTag: "home_search_input_field").
 * - Types the provided name.
 * - Waits for a folder row containing the provided name (testTag: "home_folder_row").
 * - Clicks the first matching folder row.
 *
 * Notes:
 * - Ensure the Folders filter is selected before calling this helper.
 */
internal fun ComposeTestRule.searchAndOpenFirstFolderByName(name: String) {
    onNodeWithTag("home_search_input_field")
        .performClick()
        .performTextReplacement(name)

    val folderRowMatcher =
        hasTestTag("home_folder_row").and(
            hasAnyDescendant(hasText(name, substring = true, ignoreCase = true)),
        )

    waitUntil(conditionDescription = "Waiting for folder $name", timeoutMillis = 5_000) {
        onAllNodes(folderRowMatcher, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
    }

    onAllNodes(folderRowMatcher, useUnmergedTree = true)
        .onFirst()
        .performClick()
}

/**
 * Selects a filter from the filter options.
 *
 * This function assumes that the filter button is displayed in a UI. It clicks on the "home_search_filter"
 * to open the filter options and then selects the specified filter.
 *
 * Note: This function uses the `withIndex` matcher to select the first view
 * that matches the start icon ID, as there might be multiple views with the same ID.
 *
 * @param filter The resource ID of the filter to select.
 */
internal fun ComposeTestRule.chooseFilter(filter: Int) {
    onNodeWithTag("home_search_filter").performClick()
    onNode(
        hasClickAction().and(
            hasAnyDescendant(hasText(getString(filter))),
        ),
        useUnmergedTree = true,
    ).performClick()
}

/**
 * Gets the current text from the clipboard
 * @return The text from the clipboard, or null if clipboard is empty or doesn't contain text
 */
internal fun getClipboardText(clipboardManager: android.content.ClipboardManager): String? =
    clipboardManager.primaryClip
        ?.getItemAt(0)
        ?.text
        ?.toString()

/**
 * Waits for the resource form to be ready for interaction.
 *
 * // TODO: check if it can be change to sth similar to idling resources
 */
internal fun ComposeTestRule.waitForResourceForm() {
    waitUntil(timeoutMillis = 5_000, conditionDescription = "Waiting for resource form") {
        onAllNodes(hasTestTag(ResourceForm.NAME_INPUT))
            .fetchSemanticsNodes()
            .isNotEmpty()
    }
}

/**
 * Waits for the home screen to be ready for interaction.
 *
 * // TODO: check if it can be change to sth similar to idling resources
 */
internal fun ComposeTestRule.waitForHomeScreen() {
    waitUntil(timeoutMillis = 5_000, conditionDescription = "Waiting for home screen") {
        onAllNodes(hasTestTag(Home.SCREEN))
            .fetchSemanticsNodes()
            .isNotEmpty()
    }
}
