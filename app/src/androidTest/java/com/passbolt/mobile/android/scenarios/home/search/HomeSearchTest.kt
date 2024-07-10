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

package com.passbolt.mobile.android.scenarios.home.search

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
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
import com.passbolt.mobile.android.hasDrawable
import com.passbolt.mobile.android.instrumentationTestsModule
import com.passbolt.mobile.android.intents.ManagedAccountIntentCreator
import com.passbolt.mobile.android.rules.IdlingResourceRule
import com.passbolt.mobile.android.rules.lazyActivitySetupScenarioRule
import com.passbolt.mobile.android.scenarios.helpers.signIn
import com.passbolt.mobile.android.scenarios.home.filters.ResourceFilterModel
import org.hamcrest.CoreMatchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.component.inject
import org.koin.test.KoinTest
import kotlin.test.BeforeTest
import com.google.android.material.R as MaterialR
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR


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
        signIn(managedAccountIntentCreator.getPassphrase())
        onView(withId(MaterialR.id.text_input_start_icon)).perform(click())
        onView(withId(ResourceFilterModel.entries[0].filterId)).perform(click())
    }

    @Test
    // https://passbolt.testrail.io/index.php?/cases/view/2460
    fun asALoggedInMobileUserOnTheHomepageICanPutTheFocusOnTheSearchField() {
        //    Given     I am on homepage
        onView(withId(com.passbolt.mobile.android.feature.permissions.R.id.rootLayout)).check(matches(isDisplayed()))
        //    When      I click on the search bar
        onView(withId(com.passbolt.mobile.android.feature.otp.R.id.searchEditText)).perform(click())
        //    Then      I see the search field is focused
        //    And       I see the OS keyboard
    }

    @Test
    //  https://passbolt.testrail.io/index.php?/cases/view/2629
    fun asALoggedInMobileUserOnTheHomepageICanSeeTheResourcesCorrespondingToMySearchQuery() {
        //      Given   I am a logged in mobile user
        //      And     the active filter is [Active Filter]
        //      When    I type a query in the search bar
        //      And     the query matches at least one resource within [Active Filter]
        onView(withId(com.passbolt.mobile.android.feature.otp.R.id.searchEditText)).perform(typeText("cakephp"))
        //      Then    I see the list of resources matching the query
        onView(withId(com.passbolt.mobile.android.feature.otp.R.id.recyclerView)).check(matches(atPosition(0, hasDescendant(withText("cakephp")))))
        onView(withId(com.passbolt.mobile.android.feature.otp.R.id.searchEditText)).check(matches(withText("cakephp")))
        //      And     I see the resources are sorted [sort]
        //
        //   | Active Filter       |  sort                                  |
        //   | “All items”         |  "alphabetically"                      |
        //   | “Favorites”         |  "chronologically by modification date"  |
        //   | “Recently modified” |  "chronologically by modification date"  |
        //   | “Shared with me”    |  "chronologically by modification date"  |
        //   | “Owned by me”       |  "chronologically by modification date"  |
    }

    @Test
    //  https://passbolt.testrail.io/index.php?/cases/view/2630
    fun asALoggedInMobileUserOnTheHomepageICanSeeAMessageWhenThereIsNoMatchToMySearchQuery() {
        //      Given   I am a logged in mobile user
        //      And     the active filter is [Active Filter]
        //      When    I type a query in the search bar
        onView(withId(com.passbolt.mobile.android.feature.otp.R.id.searchEditText)).perform(typeText("Does Not Exist On List"))
        //      And     the query does not match any resources within [Active Filter]
        //      Then    I see a “There is no item” message
        onView(withText(LocalizationR.string.no_passwords)).check(matches(isDisplayed()))
        //      And     I see an illustration
        onView(withId(com.passbolt.mobile.android.feature.otp.R.id.emptyListImage)).check(matches(isDisplayed()))
        //
        //   | Active Filter       |
        //   | “All items”         |
        //   | “Favorites”         |
        //   | “Recently modified” |
        //   | “Shared with me”    |
        //   | “Owned by me”       |
    }

    @Test
    // https://passbolt.testrail.io/index.php?/cases/view/2627
    fun asALoggedInMobileUserOnTheHomepageICanSeeTheCurrentAvatarSwitchesToACloseButtonWhenIFocusTheSearchBar() {
        //      Given   that I am a logged in mobile user
        //      When    I focus the search bar and start typing
        onView(withId(com.passbolt.mobile.android.feature.otp.R.id.searchEditText)).perform(click())
        onView(withId(com.passbolt.mobile.android.feature.otp.R.id.searchEditText)).perform(typeText("cakephp"))
        //      Then    I do not see the user's current avatar
        //      And     I see a close button
        onView(withId(MaterialR.id.text_input_end_icon)).check(matches(hasDrawable(CoreUiR.drawable.ic_close)))
    }

    @Test
    // https://passbolt.testrail.io/index.php?/cases/view/2628
    fun asALoggedInMobileUserOnTheHomepageICanCancelASearchAndGoBackToTheHomepage() {
        //      Given   I am a logged in mobile user
        //      And     the search bar is focused
        onView(withId(com.passbolt.mobile.android.feature.otp.R.id.searchEditText)).perform(click())
        onView(withId(com.passbolt.mobile.android.feature.otp.R.id.searchEditText)).perform(typeText("Does Not Exist On List"))
        //      When    I click on the close button
        onView(withId(MaterialR.id.text_input_end_icon)).perform(click())
        //      Then    I do not see the focus status on the search bar
        //      And     I see the homepage with the current active filter
        onView(withId(com.passbolt.mobile.android.feature.otp.R.id.recyclerView)).check(matches(isDisplayed()))
        onView(withId(com.passbolt.mobile.android.feature.otp.R.id.emptyListImage)).check(matches(not(isDisplayed())))
    }

    @Test
    // https://passbolt.testrail.io/index.php?/cases/view/2464
    fun asALoggedInMobileUserOnTheSearchFieldICanGoBackToTheHomepageWhenThereAreNoCharactersInTheSearchField() {
        //      Given   I am on the focused search field
        onView(withId(com.passbolt.mobile.android.feature.otp.R.id.searchEditText)).perform(click())
        //      When    I delete all characters in the search input
        onView(withId(com.passbolt.mobile.android.feature.otp.R.id.searchEditText)).perform(typeText("Does Not Exist On List"))
        onView(withId(com.passbolt.mobile.android.feature.otp.R.id.searchEditText)).perform(replaceText(""))
        //      Then    I see all the resources available
        onView(withId(com.passbolt.mobile.android.feature.otp.R.id.recyclerView)).check(matches(isDisplayed()))
        onView(withId(com.passbolt.mobile.android.feature.otp.R.id.emptyListImage)).check(matches(not(isDisplayed())))
        onView(withText(MaterialR.id.text_input_end_icon)).check(doesNotExist())
    }
}
