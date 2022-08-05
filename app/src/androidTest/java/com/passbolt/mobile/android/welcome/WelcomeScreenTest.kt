package com.passbolt.mobile.android.welcome

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.passbolt.mobile.android.feature.setup.R
import com.passbolt.mobile.android.feature.setup.SetUpActivity
import com.passbolt.mobile.android.instrumentationTestsModule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.test.KoinTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

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

@RunWith(AndroidJUnit4::class)
@MediumTest
class WelcomeScreenTest : KoinTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(SetUpActivity::class.java)

    @BeforeTest
    fun setup() {
        loadKoinModules(instrumentationTestsModule)
    }

    @AfterTest
    fun tearDown() {
        unloadKoinModules(instrumentationTestsModule)
    }

    @Test
    fun asAMobileUserICanSeeTheWelcomeScreenWhenIOpenTheApplicationAndNoAccountIsSetup() {
        //    Given     that the application is not configured for any users
        //    When      I launch the application
        //    Then      I see a welcome screen
        //    And       I see a Passbolt logo
        onView(withId(R.id.logoImage)).check(matches(isDisplayed()))
        //    And       I see a welcome illustration
        onView(withId(R.id.appsImage)).check(matches(isDisplayed()))
        //    And       I see a welcome message
        onView(withId(R.id.titleLabel)).check(matches(isDisplayed()))
        onView(withId(R.id.descriptionLabel)).check(matches(isDisplayed()))
        //    And       I see a “connect to an existing account” primary action
        onView(withId(R.id.connectToAccountButton)).check(matches(isDisplayed()))
        //    And       I see an "I don’t have an account" secondary action
        onView(withId(R.id.noAccountButton)).check(matches(isDisplayed()))
        //    And       I see a "Help" side action
        onView(withId(R.id.helpButton)).check(matches(isDisplayed()))
    }
}
