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
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import com.passbolt.mobile.android.matchers.withHint
import com.passbolt.mobile.android.matchers.withIndex
import com.passbolt.mobile.android.scenarios.resourcesedition.EditableFieldInput
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.hasToString
import com.google.android.material.R as MaterialR
import com.passbolt.mobile.android.core.ui.R as CoreUiR
import com.passbolt.mobile.android.feature.otp.R.id as OtpId
import com.passbolt.mobile.android.feature.permissions.R.id as PermissionsId

internal fun getString(
    @StringRes stringResId: Int,
    vararg formatArgs: String? = emptyArray(),
) = InstrumentationRegistry.getInstrumentation().targetContext.getString(stringResId, *formatArgs)

internal fun createNewPasswordFromHomeScreen(name: String) {
    onView(withId(com.passbolt.mobile.android.feature.home.R.id.createResourceFab)).perform(click())
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

internal fun signIn(passphrase: String) {
    onView(withId(CoreUiR.id.input)).perform(typeText(passphrase), closeSoftKeyboard())
    onView(withId(com.passbolt.mobile.android.feature.authentication.R.id.authButton)).perform(click())
    onView(withId(PermissionsId.rootLayout)).check(matches(isDisplayed()))
}

/**
 * Navigates to the details screen of the first resource with the given name.
 *
 * This function assumes the search field is already visible and performs the following steps:
 * 1. Clicks on the search field (identified by `searchEditText`).
 * 2. Types the given `name` into the search field.
 * 3. Clicks on the first item in the search results that matches the given `name`.
 *    Note: The search field itself is assumed to be at index 0, so the first resource is at index 1.
 *    Note: The filter is assumed to be previously set according to the test desire
 *
 * @param name The name of the resource to search for and navigate to.
 */
internal fun pickFirstResourceWithName(name: String) {
    onView(withId(OtpId.searchEditText)).perform(click(), typeText(name))
    onView(withIndex(index = 1, withText(name))).perform(click())
}

/**
 * Selects a filter from the filter options.
 *
 * This function assumes that the filter button is displayed in a UI. It clicks on the "text input start icon"
 * to open the filter options and then selects the specified filter.
 *
 * Note: This function uses the `withIndex` matcher to select the first view
 * that matches the start icon ID, as there might be multiple views with the same ID.
 *
 * @param filter The resource ID of the filter to select.
 */
internal fun chooseFilter(filter: Int) {
    onView(withIndex(index = 0, withId(MaterialR.id.text_input_start_icon))).perform(click())
    onView(withId(filter)).perform(click())
}
