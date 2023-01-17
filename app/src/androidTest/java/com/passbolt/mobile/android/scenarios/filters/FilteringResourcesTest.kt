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
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.passbolt.mobile.android.core.idlingresource.ResourcesFullRefreshIdlingResource
import com.passbolt.mobile.android.core.idlingresource.SignInIdlingResource
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.feature.authentication.AuthenticationMainActivity
import com.passbolt.mobile.android.feature.setup.R
import com.passbolt.mobile.android.feature.setup.R.id.titleDrawable
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
class FilteringResourcesTest : KoinTest {

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
        onView(withId(R.id.authButton)).perform(scrollTo(), click())
    }

    @Test
    // https://passbolt.testrail.io/index.php?/cases/view/2621
    fun asALoggedInMobileUserOnTheHomepageICanChangeTheCurrentActiveFilter() {
        //        Given       that I am a logged in mobile user
        //        And         the active filter is   not   <filter>
        val filterList = ResourceFilterModel.values()
        filterList.indices.forEach { index ->
            //        When        I open the filter drawer
            onView(withId(R.id.text_input_start_icon)).perform(click())
            //        And         I click on the <filter> list item
            onView(withId(filterList[index].filterId)).perform(click())
            //        Then        I do not see the filter drawer
            //        And         I see the homepage
            onView(withId(R.id.rootLayout)).check(matches(isDisplayed()))
            //        And         I see the title is <filter> with its corresponding icon
            onView(withText(filterList[index].filterNameId)).check(matches(isDisplayed()))
            onView(withId(titleDrawable)).check(matches(hasDrawable(filterList[index].filterIconId)))
            //        And         I see the list of resources contains <filter> elements // TODO: unitTest?
            //        And         I see the list of resources is <sorted> // TODO: unitTest?
            //        Examples:
            //           | filter              | sorted         |
            //           | “All items”         | alphabetically |
            //           | “Favourites”        | chronologically with the latest modifications on top |
            //           | “Recently modified” | chronologically with the latest modifications on top |
            //           | “Shared with me”    | chronologically with the latest modifications on top |
            //           | “Owned by me”       | chronologically with the latest modifications on top |
            //           | “Folders”           | alphabetically |
            //           | “Tags”              | alphabetically |
            //           | “Groups”            | alphabetically |
        }
    }
}
