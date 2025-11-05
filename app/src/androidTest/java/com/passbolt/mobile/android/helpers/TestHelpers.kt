/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021-2025 Passbolt SA
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
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.platform.app.InstrumentationRegistry
import com.passbolt.mobile.android.matchers.withHint
import com.passbolt.mobile.android.scenarios.resourcesedition.EditableFieldInput
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.hasToString
import com.passbolt.mobile.android.core.ui.R as CoreUiR

internal fun getString(
    @StringRes stringResId: Int,
    vararg formatArgs: String? = emptyArray(),
) = InstrumentationRegistry.getInstrumentation().targetContext.getString(stringResId, *formatArgs)

internal fun createNewPasswordFromHomeScreen(name: String) {
//    onView(withId(com.passbolt.mobile.android.feature.home.R.id.createResourceFab)).perform(click())
    onView(withId(com.passbolt.mobile.android.feature.createresourcemenu.R.id.createPassword)).perform(click())

    onView(withId(CoreUiR.id.generatePasswordLayout)).perform(click())
    onView(
        allOf(
            isDescendantOfA(withHint(hasToString(EditableFieldInput.NAME.hintName))),
            withId(CoreUiR.id.input),
        ),
    ).perform(
        typeText(name),
    )
    onView(
        allOf(
            isDescendantOfA(withHint(hasToString(EditableFieldInput.ENTER_URL.hintName))),
            withId(CoreUiR.id.input),
        ),
    ).perform(
        typeText("TestURL"),
    )
    onView(
        allOf(
            isDescendantOfA(withHint(hasToString(EditableFieldInput.ENTER_USERNAME.hintName))),
            withId(CoreUiR.id.input),
        ),
    ).perform(
        typeText("TestUsername"),
    )
    onView(withId(com.passbolt.mobile.android.feature.resourceform.R.id.primaryButton)).perform(click())
}

/**
 * Extension variant of signIn so callers don’t need to pass the ComposeTestRule explicitly.
 *
 * Usage: composeTestRule.signIn(passphrase)
 */
internal fun ComposeTestRule.signIn(passphrase: String) {
    onView(withId(CoreUiR.id.input)).perform(typeText(passphrase), closeSoftKeyboard())
    onView(withId(com.passbolt.mobile.android.feature.authentication.R.id.authButton)).perform(click())
    this.onNodeWithTag("home_screen").assertIsDisplayed()
}

/**
 * Searches for a resource by name from the Home screen and opens the first matching result.
 *
 * Behavior:
 * - Focuses the search input (testTag: "home_search_input").
 * - Types the provided name.
 * - Taps the first resource row in the results list (testTag: "home_resource_row").
 *
 * Assumptions:
 * - The Home screen is visible.
 * - Any desired filtering has been applied beforehand.
 *
 * @param name The resource name to search for.
 */
internal fun ComposeTestRule.pickFirstResourceWithName(name: String) {
    this
        .onNodeWithTag("home_search_input")
        .performClick()
        .performTextInput(name)
    this
        .onAllNodesWithTag("home_resource_row")
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
    this.onNodeWithTag("home_search_filter").performClick()
    this
        .onNode(
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
