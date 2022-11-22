package com.passbolt.mobile.android.scenarios.filters

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.passbolt.mobile.android.core.idlingresource.SignInIdlingResource
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.feature.authentication.AuthenticationMainActivity
import com.passbolt.mobile.android.feature.setup.R
import com.passbolt.mobile.android.hasBackgroundColor
import com.passbolt.mobile.android.hasDrawable
import com.passbolt.mobile.android.instrumentationTestsModule
import com.passbolt.mobile.android.intents.ManagedAccountIntentCreator
import com.passbolt.mobile.android.rules.IdlingResourceRule
import com.passbolt.mobile.android.rules.lazyActivitySetupScenarioRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.component.inject
import org.koin.test.KoinTest
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
class FilterDrawerTest : KoinTest {

    @get:Rule
    val startUpActivityRule = lazyActivitySetupScenarioRule<AuthenticationMainActivity>(
        koinOverrideModules = listOf(instrumentationTestsModule),
        intentSupplier = {
            ActivityIntents.authentication(
                InstrumentationRegistry.getInstrumentation().targetContext,
                ActivityIntents.AuthConfig.Startup,
                AppContext.APP,
                managedAccountIntentCreator.getUserLocalId()
            )
        }
    )

    private val managedAccountIntentCreator: ManagedAccountIntentCreator by inject()

    @get:Rule
    val idlingResourceRule = let {
        val signInIdlingResource: SignInIdlingResource by inject()
        IdlingResourceRule(arrayOf(signInIdlingResource))
    }

    @BeforeTest
    fun setup() {
        onView(withId(R.id.input)).perform(typeText(managedAccountIntentCreator.getUsername()))
        onView(withId(R.id.authButton)).perform(click())
    }

    @Test
    fun asALoggedInMobileUserOnTheHomepageICanSeeAFilterIconInTheSearchBar() {
        //    Given     that I am a logged in mobile user
        //    When      I am on the homepage
        onView(withId(R.id.rootLayout)).check(matches(isDisplayed()))
        //    And       the search bar is not focused
        //    Then      I see an icon filter in the left side of the search bar
        onView(withId(R.id.text_input_start_icon)).check(matches(isDisplayed()))
    }

    @Test
    fun asALoggedInMobileUserOnTheHomepageICanSeeTheFilterDrawer() {
        //    Given     that I am a logged in mobile user on the homepage
        //    When      I click on the filter icon
        onView(withId(R.id.text_input_start_icon)).perform(click())
        //    Then      I see the “filter” drawer
        onView(withId(R.id.root)).check(matches(isDisplayed()))
        //    And       I see the homepage is greyed out in the background
        //    And       I see a “Filter view by” title
        onView(withText(R.string.filters_menu_title)).check(matches(isDisplayed()))
        //    And       I see a close button
        onView(withId(R.id.close)).check(matches(isDisplayed()))
        //    And       I see a list of filters with their corresponding icons
        //    And       I see { “All items”, "Favorites", "Recently modified", "Shared with me" , "Owned by me",
        //              "Folders", "Tags", "Groups" } items
        ResourceFilterModel.values().forEach { filterItem ->
            onView(withId(filterItem.filterId))
                .check(matches(isDisplayed()))
                .check(matches(hasDrawable(filterItem.filterIconId, R.color.icon_tint)))
        }
    }

    // This scenario is combination of scenarios: C2618, C2619, C2620
    @Test
    fun asALoggedInMobileUserOnTheHomepageICanSeeTheActiveFilterInTheDrawerIsTheSameATheHomepageTitle() {
        //    Given     that I am a logged in mobile user on the filter drawer
        val filterList = ResourceFilterModel.values()
        filterList.indices.forEach { index ->
            onView(withId(R.id.text_input_start_icon)).perform(click())
            onView(withId(filterList[index].filterId)).perform(click())
            //    When      the [filter] filter is active
            //    Then      I see the title of the homepage is [filter] with its corresponding icon
            onView(withText(filterList[index].filterNameId)).check(matches(isDisplayed()))
            onView(withId(R.id.titleDrawable)).check(matches(hasDrawable(filterList[index].filterIconId)))
            //    When      I open the filter drawer
            onView(withId(R.id.text_input_start_icon)).perform(click())
            //    Then      I see the current active [filter] filter in the list with an active status
            onView(withId(filterList[index].filterId))
                .check(matches(isDisplayed()))
                .check(matches(hasBackgroundColor(R.color.primary)))
            //    When      I click on the close button
            onView(withId(R.id.close)).perform(click())
        }
    }
}
