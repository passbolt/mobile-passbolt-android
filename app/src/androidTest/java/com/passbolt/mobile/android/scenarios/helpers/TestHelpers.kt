/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021-2024 Passbolt SA
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

package com.passbolt.mobile.android.scenarios.helpers

import android.view.KeyEvent
import androidx.annotation.StringRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.pressKey
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.R
import com.passbolt.mobile.android.feature.otp.R.id.searchEditText
import com.passbolt.mobile.android.withHint
import com.passbolt.mobile.android.withIndex
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.hasToString
import com.passbolt.mobile.android.core.ui.R as CoreUiR


internal fun getString(@StringRes stringResId: Int, vararg formatArgs: String? = emptyArray()) =
    InstrumentationRegistry.getInstrumentation().targetContext.getString(stringResId, *formatArgs)

internal fun createNewPasswordFromHomeScreen(name: String) {
    onView(withId(com.passbolt.mobile.android.feature.home.R.id.homeSpeedDialViewId)).perform(click())
    onView(
        allOf(
            isDescendantOfA(withHint(hasToString("Enter Name"))),
            withId(CoreUiR.id.input)
        )
    )
        .perform(typeText(name), pressKey(KeyEvent.KEYCODE_BACK))
    onView(
        allOf(
            isDescendantOfA(withHint(hasToString("Enter URL"))),
            withId(CoreUiR.id.input)
        )
    )
        .perform(typeText("TestURL"), pressKey(KeyEvent.KEYCODE_BACK))
    onView(
        allOf(
            isDescendantOfA(withHint(hasToString("Enter Username"))),
            withId(CoreUiR.id.input)
        )
    )
        .perform(typeText("TestUsername"), pressKey(KeyEvent.KEYCODE_BACK))
    onView(withId(CoreUiR.id.generatePasswordLayout)).perform(click())
    onView(
        allOf(
            isDescendantOfA(withHint(hasToString("Enter Description"))),
            withId(CoreUiR.id.input)
        )
    )
        .perform(typeText("TestDescription"), pressKey(KeyEvent.KEYCODE_BACK))
    onView(withId(com.passbolt.mobile.android.feature.resources.R.id.updateButton)).perform(scrollTo(), click())
}

internal fun signIn(passphrase: String) {
    onView(withId(CoreUiR.id.input)).perform(typeText(passphrase), pressKey(KeyEvent.KEYCODE_ENTER))
    onView(withId(com.passbolt.mobile.android.feature.authentication.R.id.authButton)).perform(click())
    //TODO: There is failure here when testing on real devices https://app.clickup.com/t/2593179/MOB-1835
    //    sleep(300) usually helps
    onView(withId(com.passbolt.mobile.android.feature.permissions.R.id.rootLayout)).check(matches(isDisplayed()))
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
    onView(withId(searchEditText)).perform(click(), typeText(name))
    onView(withIndex(index = 1, withText(name))).perform(click())
}

internal fun chooseFilter(filter: Int) {
    onView(withId(R.id.text_input_start_icon)).perform(click())
    onView(withId(filter)).perform(click())
}