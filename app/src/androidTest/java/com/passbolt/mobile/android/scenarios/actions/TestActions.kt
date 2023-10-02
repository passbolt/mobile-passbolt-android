package com.passbolt.mobile.android.scenarios.actions

import android.view.View
import android.widget.Checkable
import androidx.test.espresso.Espresso
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import com.passbolt.mobile.android.scenarios.helpers.getString
import com.passbolt.mobile.android.withHint
import org.hamcrest.BaseMatcher
import org.hamcrest.CoreMatchers.isA
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import com.google.android.material.R as MaterialR
import com.passbolt.mobile.android.core.localization.R as LocalizationR


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

internal fun clickOnPasswordToggle() {
    Espresso.onView(
        Matchers.allOf(
            ViewMatchers.isDescendantOfA(withHint(Matchers.hasToString(getString(LocalizationR.string.resource_update_password_hint)))),
            ViewMatchers.withId(MaterialR.id.text_input_end_icon)
        )
    ).perform(ViewActions.click())
}

internal fun setChecked(checked: Boolean) = object : ViewAction {

    override fun getDescription(): String {
        return "checking the checkable view"
    }

    override fun getConstraints(): Matcher<View> = object : BaseMatcher<View>() {

        override fun describeTo(description: Description?) {
            description?.appendText("is checkable")
        }

        override fun matches(item: Any?): Boolean {
            return isA(Checkable::class.java).matches(item)
        }
    }

    override fun perform(uiController: UiController?, view: View?) {
        (view as Checkable).isChecked = checked
    }
}
