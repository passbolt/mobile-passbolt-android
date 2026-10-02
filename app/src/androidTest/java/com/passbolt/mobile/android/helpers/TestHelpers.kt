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

import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.platform.app.InstrumentationRegistry
import com.passbolt.mobile.android.testtags.composetags.Auth
import com.passbolt.mobile.android.testtags.composetags.FiltersMenu
import com.passbolt.mobile.android.testtags.composetags.Home
import com.passbolt.mobile.android.testtags.composetags.ResourceForm
import com.passbolt.mobile.android.testtags.composetags.SearchField
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
    waitForCreateButton()
    onNodeWithTag(Home.FAB).performClick()
    waitForText(getString(LocalizationR.string.create_resource_menu_create_password))
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
    waitForHomeScreen()
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
    onNodeWithTag(SearchField.INPUT)
        .performClick()
        .performTextReplacement(name)

    val rowMatcher =
        hasTestTag(Home.RESOURCE_ROW).and(
            hasAnyDescendant(hasText(name, substring = true, ignoreCase = true)),
        )

    waitUntil(conditionDescription = "Waiting for resource $name", timeoutMillis = 4_000) {
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
    onNodeWithTag(SearchField.INPUT)
        .performClick()
        .performTextReplacement(name)

    val rowMatcher =
        hasTestTag(Home.RESOURCE_ROW).and(
            hasAnyDescendant(hasText(name, substring = true, ignoreCase = true)),
        )

    waitUntil(conditionDescription = "Waiting for resource $name", timeoutMillis = 10_000) {
        onAllNodes(rowMatcher, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
    }

    openFirstResourceMoreMenu()
}

/**
 * Taps the first resource row's More button and waits for the More bottom sheet to open.
 *
 * The tap can be silently lost while the freshly filtered list is still recomposing - the
 * bottom sheet never opens, so nothing inside it (e.g. the Edit/Delete items) is ever composed
 * and a follow-up `onNodeWithText(...)` fails with "could not find any node". This retries the
 * tap until the sheet is actually shown, detected via the always-present "Copy URL" item, so
 * callers can rely on the menu being open.
 *
 * Why retry instead of an idling resource:
 * - The Home list is Paging 3 (`LazyPagingItems`), so there is no single async call with a clean
 *   start/end to wrap in a `setIdle(false)`/`setIdle(true)` pair. The closest signal,
 *   `loadState.refresh: Loading -> NotLoading`, lives at the *data* layer and fires one frame too
 *   early: it means the page data is available, not that the `LazyColumn` has finished recomposing
 *   and laid the row out at stable, hit-testable coordinates.
 * - The `waitUntil` in the caller already waits for "the row's node exists", but that is satisfied
 *   before the row's relayout settles. The residual race is the sub-frame between the finder
 *   computing the button's bounds and the tap being injected, during which the list can still
 *   swap a placeholder for content or reorder, moving the button out from under the tap.
 * - Waiting on any pre-tap signal is therefore necessary-but-not-sufficient and can always be
 *   beaten by that sub-frame reflow. Retrying and verifying the actual *effect* (the sheet opened)
 *   is self-correcting, so it is the robust tool for this last-mile layout/hit-test race.
 */
private fun ComposeTestRule.openFirstResourceMoreMenu() {
    val sheetOpenMatcher = hasText(getString(LocalizationR.string.more_copy_uri))

    repeat(MORE_MENU_OPEN_ATTEMPTS) { attempt ->
        onAllNodes(hasTestTag(Home.RESOURCE_MORE), useUnmergedTree = true)
            .onFirst()
            .performClick()
        try {
            waitUntil(
                conditionDescription = "Waiting for more menu to open (attempt ${attempt + 1})",
                timeoutMillis = MORE_MENU_OPEN_TIMEOUT_MILLIS,
            ) {
                onAllNodes(sheetOpenMatcher, useUnmergedTree = true)
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            // Surface any retry so it is not silently absorbed:
            // if this regularly logs attempt > 1, the app-side fix is needed, not a bigger retry cap.
            if (attempt > 0) {
                Log.w(MORE_MENU_LOG_TAG, "More menu opened only on attempt ${attempt + 1}; first tap(s) were lost")
            }
            return
        } catch (ignored: ComposeTimeoutException) {
            Log.w(MORE_MENU_LOG_TAG, "More menu tap lost on attempt ${attempt + 1}; retrying")
        }
    }
    error("More menu did not open after $MORE_MENU_OPEN_ATTEMPTS attempts")
}

private const val MORE_MENU_OPEN_ATTEMPTS = 3
private const val MORE_MENU_OPEN_TIMEOUT_MILLIS = 5_000L
private const val MORE_MENU_LOG_TAG = "MoreMenuHelper"

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
    onNodeWithTag(SearchField.INPUT)
        .performClick()
        .performTextReplacement(name)

    val folderRowMatcher =
        hasTestTag(Home.FOLDER_ROW).and(
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
    waitUntil(conditionDescription = "Waiting for previous filters menu to dismiss", timeoutMillis = 5_000) {
        onAllNodes(hasTestTag(FiltersMenu.SHEET))
            .fetchSemanticsNodes()
            .isEmpty()
    }

    onNodeWithTag(Home.SEARCH_FILTER).performClick()

    val filterMatcher =
        hasClickAction().and(
            hasAnyDescendant(hasText(getString(filter))),
        )

    // Wait until the filter item is laid out in the opened sheet; clicking earlier fails with
    // "Failed to inject touch input" because the node isn't present yet.
    waitUntil(conditionDescription = "Waiting for filter ${getString(filter)}", timeoutMillis = 5_000) {
        onAllNodes(filterMatcher, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
    }

    onNode(filterMatcher, useUnmergedTree = true)
        .performScrollTo()
        .performClick()
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
 * [timeoutMillis] defaults to 5s (enough after a normal sign-in).
 *
 * // TODO: check if it can be change to sth similar to idling resources
 */
internal fun ComposeTestRule.waitForHomeScreen(timeoutMillis: Long = 5_000) {
    waitUntil(timeoutMillis = timeoutMillis, conditionDescription = "Waiting for home screen") {
        onAllNodes(hasTestTag(Home.SCREEN))
            .fetchSemanticsNodes()
            .isNotEmpty()
    }
}

/**
 * Waits for the Create (FAB) button to be present on the Home screen.
 *
 * The FAB is gated on `canCreateResource`, which is recomputed asynchronously after a
 * resource list reload (e.g. after selecting a filter). Clicking it immediately races the
 * recomposition and fails with "Failed to inject touch input ... could not find any node
 * that satisfies (TestTag = 'HomeFab')" - intermittently when run in isolation, reliably
 * when run in the middle of a suite where accumulated state slows the reload down.
 */
internal fun ComposeTestRule.waitForCreateButton() {
    waitUntil(timeoutMillis = 5_000, conditionDescription = "Waiting for create button") {
        onAllNodes(hasTestTag(Home.FAB))
            .fetchSemanticsNodes()
            .isNotEmpty()
    }
}

/**
 * Waits until a node whose text matches [text] is present before it is interacted with.
 *
 * Screen transitions (e.g. after re-authentication, or navigating to the setup
 * biometric/autofill screens) recompose asynchronously. Querying the target button
 * immediately races the transition and fails with "Failed to inject touch input ... could
 * not find any node that satisfies (Text ... contains '...')" - sometimes reporting the
 * node only exists in the unmerged tree, which is the very same race caught mid-merge.
 */
internal fun ComposeTestRule.waitForText(
    text: String,
    timeoutMillis: Long = 5_000,
) {
    waitUntil(timeoutMillis = timeoutMillis, conditionDescription = "Waiting for text '$text'") {
        onAllNodes(hasText(text))
            .fetchSemanticsNodes()
            .isNotEmpty()
    }
}

internal fun ComposeTestRule.waitForTextDisplayed(
    text: String,
    timeoutMillis: Long = 5_000,
) {
    waitUntil(
        timeoutMillis = timeoutMillis,
        conditionDescription = "Waiting for text '$text' to be displayed",
    ) {
        onNodeWithText(text).isDisplayed()
    }
}

/**
 * Accepts the "How Passbolt uses the Accessibility Service" consent screen that is shown at the
 * end of the first-run setup flow (after the biometric and autofill steps), before the home
 * screen is reached. In the setup flow both Accept and Decline proceed to home.
 */
internal fun ComposeTestRule.acceptAccessibilityPolicies() {
    val accept = getString(LocalizationR.string.accessibility_policies_accept)
    waitForText(accept)
    onNodeWithText(accept).performClick()
}
