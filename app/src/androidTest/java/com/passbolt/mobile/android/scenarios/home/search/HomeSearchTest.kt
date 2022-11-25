package com.passbolt.mobile.android.scenarios.home.search

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.passbolt.mobile.android.atPosition
import com.passbolt.mobile.android.core.idlingresource.ResourcesFullRefreshIdlingResource
import com.passbolt.mobile.android.core.idlingresource.SignInIdlingResource
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.feature.authentication.AuthenticationMainActivity
import com.passbolt.mobile.android.feature.setup.R
import com.passbolt.mobile.android.hasDrawable
import com.passbolt.mobile.android.instrumentationTestsModule
import com.passbolt.mobile.android.intents.ManagedAccountIntentCreator
import com.passbolt.mobile.android.rules.IdlingResourceRule
import com.passbolt.mobile.android.rules.lazyActivitySetupScenarioRule
import org.hamcrest.CoreMatchers.not
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
class HomeSearchTest : KoinTest {

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
        val resourcesFullRefreshIdlingResource: ResourcesFullRefreshIdlingResource by inject()
        IdlingResourceRule(arrayOf(signInIdlingResource, resourcesFullRefreshIdlingResource))
    }

    @BeforeTest
    fun setup() {
        onView(withId(R.id.input)).perform(typeText(managedAccountIntentCreator.getUsername()))
        onView(withId(R.id.authButton)).perform(click())
    }

    @Test
    fun asALoggedInMobileUserOnTheHomepageICanPutTheFocusOnTheSearchField() {
        //    Given     I have biometrics configured on my device
        //    Given     that I am a mobile user with the application installed
        //    And       I am on homepage
        onView(withId(R.id.rootLayout)).check(matches(isDisplayed()))
        //    When      I click on the search bar
        onView(withId(R.id.searchEditText)).perform(click())
        //    Then      I see the search field is focused
        //    And       I see the OS keyboard
        //    When      I type at least one character
        onView(withId(R.id.searchEditText)).perform(typeText("cakephp"))
        //    And       I do not see the account avatar
        onView(withId(R.id.text_input_end_icon)).check(matches(not(hasDrawable(R.drawable.ic_avatar_placeholder))))
        //    Then      I see a cross button inside the search field on the right
        onView(withId(R.id.text_input_end_icon)).check(matches(hasDrawable(R.drawable.ic_close)))
    }

    @Test
    fun asALoggedInMobileUserICanSeeTheResultOfASuccessfulSearchQuery() {
        //    Given     that I am a mobile user with the application installed
        //    And       I have at least one resource
        //    And       I put the focus on the search field
        onView(withId(R.id.searchEditText)).perform(click())
        //    When      I start typing a search query
        //    And       the query has at least one match
        onView(withId(R.id.searchEditText)).perform(typeText("cakephp"))
        //    Then      I see the resource(s) matching the query
        //    When      I press Go or Enter
        onView(withId(R.id.searchEditText)).perform(pressImeActionButton())
        //    Then      I do not see the keyboard
        //    And       I see the home page with the search results
        onView(withId(R.id.recyclerView)).check(matches(atPosition(0, hasDescendant(withText("cakephp")))))
        //    And       I see my search term in the search input
        onView(withId(R.id.searchEditText)).check(matches(withText("cakephp")))
        //    And       I see a cross button
        onView(withId(R.id.text_input_end_icon)).check(matches(hasDrawable(R.drawable.ic_close)))
    }

    @Test
    fun asALoggedInMobileUserOnTheSearchFieldICanSeeTheResultOfAnUnsuccessfulSearchQuery() {
        //    Given     that I am a mobile user with the application installed
        //    And       the Passbolt application is already opened
        //    And       I am put the focus on the search input field
        onView(withId(R.id.searchEditText)).perform(click())
        //    When      I type a query
        onView(withId(R.id.searchEditText)).perform(typeText("Does Not Exist On List"))
        //    And       the query has no match
        //    Then      I see a “There is no password” title
        onView(withText(R.string.no_passwords)).check(matches(isDisplayed()))
        //    And       I see an illustration
        onView(withId(R.id.emptyListImage)).check(matches(isDisplayed()))
    }

    @Test
    fun asALoggedInMobileUserOnTheSearchFieldICanGoBackToTheHomepageViaTheCrossButton() {
        //    Given     that I am a mobile user with the application installed
        //    And       I have filtered the home page using the search
        onView(withId(R.id.searchEditText)).perform(click())
        onView(withId(R.id.searchEditText)).perform(typeText("Does Not Exist On List"))
        //    When      I click on the cross button next to the search input
        onView(withId(R.id.text_input_end_icon)).perform(click())
        //    Then      I see all the resources available
        onView(withId(R.id.recyclerView)).check(matches(isDisplayed()))
        onView(withId(R.id.emptyListImage)).check(matches(not(isDisplayed())))
    }

    @Test
    fun asALoggedInMobileUserOnTheSearchFieldICanGoBackToTheHomepageWhenThereAreNoCharactersInTheSearchField() {
        //    Given     that I am a mobile user with the application installed
        //    And       the Passbolt application is already opened
        //    And       I completed the login step
        //    And       I am on the focused search field
        onView(withId(R.id.searchEditText)).perform(click())
        //    When      I delete all characters in the search input
        onView(withId(R.id.searchEditText)).perform(typeText("Does Not Exist On List"))
        onView(withId(R.id.searchEditText)).perform(replaceText(""))
        //    And       I click outside the search field or the OS keyboard
        onView(withId(R.id.screenTitleLabel)).perform(click())
        //    Then      I see all the resources available
        onView(withId(R.id.recyclerView)).check(matches(isDisplayed()))
        onView(withId(R.id.emptyListImage)).check(matches(not(isDisplayed())))
        onView(withText(R.id.text_input_end_icon)).check(doesNotExist())
    }
}
