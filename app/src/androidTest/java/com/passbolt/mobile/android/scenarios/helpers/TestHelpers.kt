package com.passbolt.mobile.android.scenarios.helpers

import android.view.KeyEvent
import androidx.annotation.StringRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.pressKey
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.platform.app.InstrumentationRegistry
import com.passbolt.mobile.android.feature.setup.R
import com.passbolt.mobile.android.withHint
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.hasToString

/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021 Passbolt SA
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

internal fun getString(@StringRes stringResId: Int, vararg formatArgs: String? = emptyArray()) =
    InstrumentationRegistry.getInstrumentation().targetContext.getString(stringResId, *formatArgs)

internal fun createNewPasswordFromHomeScreen() {
    onView(withId(R.id.homeSpeedDialViewId)).perform(click())
    onView(
        allOf(
            isDescendantOfA(withHint(hasToString("Enter Name"))),
            withId(R.id.input)
        )
    )
        .perform(typeText("ResourcesEditionTestPK"), pressKey(KeyEvent.KEYCODE_BACK))
    onView(
        allOf(
            isDescendantOfA(withHint(hasToString("Enter URL"))),
            withId(R.id.input)
        )
    )
        .perform(typeText("TestURL"), pressKey(KeyEvent.KEYCODE_BACK))
    onView(
        allOf(
            isDescendantOfA(withHint(hasToString("Enter Username"))),
            withId(R.id.input)
        )
    )
        .perform(typeText("TestUsername"), pressKey(KeyEvent.KEYCODE_BACK))
    onView(withId(R.id.generatePasswordLayout)).perform(click())
    onView(
        allOf(
            isDescendantOfA(withHint(hasToString("Enter Description"))),
            withId(R.id.input)
        )
    )
        .perform(typeText("TestDescription"), pressKey(KeyEvent.KEYCODE_BACK))
    onView(withId(R.id.updateButton)).perform(scrollTo(), click())
}
