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

package com.passbolt.mobile.android.scenarios.filters

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.passbolt.mobile.android.core.idlingresource.ResourcesFullRefreshIdlingResource
import com.passbolt.mobile.android.core.idlingresource.SignInIdlingResource
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.feature.authentication.AuthenticationMainActivity
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
import com.google.android.material.R as MaterialR
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR


@RunWith(AndroidJUnit4::class)
@LargeTest
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
        val resourcesFullRefreshIdlingResource: ResourcesFullRefreshIdlingResource by inject()
        IdlingResourceRule(arrayOf(signInIdlingResource, resourcesFullRefreshIdlingResource))
    }

    @BeforeTest
    fun setup() {
        onView(withId(CoreUiR.id.input)).perform(typeText(managedAccountIntentCreator.getUsername()))
        onView(withId(com.passbolt.mobile.android.feature.authentication.R.id.authButton)).perform(scrollTo(), click())
    }

    @Test
    // https://passbolt.testrail.io/index.php?/cases/view/2616
    fun asALoggedInMobileUserOnTheHomepageICanSeeAFilterIconInTheSearchBar() {
        //    Given     that I am a logged in mobile user
        //    When      I am on the homepage
        onView(withId(com.passbolt.mobile.android.feature.permissions.R.id.rootLayout)).check(matches(isDisplayed()))
        //    And       the search bar is not focused
        //    Then      I see an icon filter in the left side of the search bar
        onView(withId(MaterialR.id.text_input_start_icon)).check(matches(isDisplayed()))
    }

    @Test
    // https://passbolt.testrail.io/index.php?/cases/view/2617
    fun asALoggedInMobileUserOnTheHomepageICanSeeTheFilterDrawer() {
        //    Given     that I am a logged in mobile user on the homepage
        //    When      I click on the filter icon
        onView(withId(MaterialR.id.text_input_start_icon)).perform(click())
        //    Then      I see the “filter” drawer
        onView(withId(com.passbolt.mobile.android.feature.resources.R.id.root)).check(matches(isDisplayed()))
        //    And       I see the homepage is greyed out in the background
        //    And       I see a “Filter view by” title
        onView(withText(LocalizationR.string.filters_menu_title)).check(matches(isDisplayed()))
        //    And       I see a close button
        onView(withId(com.passbolt.mobile.android.feature.autofill.R.id.close)).check(matches(isDisplayed()))
        //    And       I see a list of filters
        //    And       I see <filter> list item with their corresponding icon
        ResourceFilterModel.entries.forEach { model ->
            onView(withId(model.filterId))
                .check(matches(isDisplayed()))
                .check(matches(hasDrawable(model.filterIconId, CoreUiR.color.icon_tint)))
            //    Examples:
            //      | filter              |
            //      | “All items”         |
            //      | “Favorites”         |
            //      | “Recently modified” |
            //      | “Shared with me”    |
            //      | “Owned by me”       |
            //      | “Folders”           |
            //      | “Tags”              |
            //      | “Groups”            |
        }
    }

    @Test
    // https://passbolt.testrail.io/index.php?/cases/view/2618
    fun asALoggedInMobileUserOnTheFilterDrawerICanSeeTheActiveFilterUsedToFilterOrSortResources() {
        // Given      that I am a logged in mobile user
        ResourceFilterModel.entries.forEach { model ->
            // And        the <filter> is active
            // When       I open the filter drawer
            onView(withId(MaterialR.id.text_input_start_icon)).perform(click())
            onView(withId(model.filterId)).perform(click())
            // Then       I see the current active <filter> in the list with an active status
            onView(withId(MaterialR.id.text_input_start_icon)).perform(click())
            onView(withId(model.filterId))
                .check(matches(isDisplayed()))
                .check(matches(hasBackgroundColor(CoreUiR.color.primary)))
            // And        I can close the drawer
            onView(withId(com.passbolt.mobile.android.feature.autofill.R.id.close)).perform(click())
            // Examples:
            //    | filter              |
            //    | “All items”         |
            //    | “Favorites”         |
            //    | “Recently modified” |
            //    | “Shared with me”    |
            //    | “Owned by me”       |
            //    | “Folders”           |
            //    | “Tags”              |
            //    | “Groups”            |
        }
    }
}
