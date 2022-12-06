package com.passbolt.mobile.android.scenarios.resourcesdetails

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.jakewharton.espresso.OkHttp3IdlingResource
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
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
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
class ResourcesDetailsTest : KoinTest {

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
        val okHttpIdlingResource: OkHttp3IdlingResource by inject()
        IdlingResourceRule(arrayOf(signInIdlingResource, resourcesFullRefreshIdlingResource, okHttpIdlingResource))
    }

    @BeforeTest
    fun setup() {
        onView(withId(R.id.input)).perform(typeText(managedAccountIntentCreator.getUsername()))
        onView(withId(R.id.authButton)).perform(click())
    }

    @Test
    fun asALoggedInMobileUserOnTheHomepageICanAccessTheResourceViewPage() {
        //    Given     that I am a mobile user with the application installed
        //    And       the Passbolt application is already opened
        //    And       I completed the login step
        //    And       I am on the homepage
        onView(withId(R.id.rootLayout)).check(matches(isDisplayed()))
        onView(withId(R.id.searchEditText)).perform(click(), typeText("cake"))
        //    When      I click on a resource
        onView(withText("cakephp")).perform(click())
        //    Then      I see the resource display screen
        //    And       I see an arrow on the top left corner to go back to the previous page
        onView(withId(R.id.backArrow))
            .check(matches(isDisplayed()))
            .check(matches(hasDrawable(id = R.drawable.ic_arrow_left, tint = R.color.icon_tint)))
        //    And       I see a “3 dots” icon on the top right corner
        onView(withId(R.id.moreIcon))
            .check(matches(isDisplayed()))
            .check(matches(hasDrawable(id = R.drawable.ic_more, tint = R.color.icon_tint)))
        //    And       I see the resource favicon or a default icon
        onView(withId(R.id.icon)).check(matches(isDisplayed()))
        //    And       I see the resource name
        onView(allOf(withId(R.id.name), withText("cakephp"))).check(matches(isDisplayed()))
        //    And       I see the “Website URL” list item with title, value and a copy icon
        onView(withText(R.string.resource_details_url_header)).check(matches(isDisplayed()))
        onView(withId(R.id.urlValue)).check(matches(isDisplayed()))
        onView(withId(R.id.urlIcon))
            .check(matches(isDisplayed()))
            .check(matches(hasDrawable(id = R.drawable.ic_copy, tint = R.color.icon_tint)))
        //    And       I see the “Username” list item with title, value and a copy icon
        onView(withText(R.string.resource_details_username_header)).check(matches(isDisplayed()))
        onView(withId(R.id.usernameValue)).check(matches(isDisplayed()))
        onView(withId(R.id.usernameIcon))
            .check(matches(isDisplayed()))
            .check(matches(hasDrawable(id = R.drawable.ic_copy, tint = R.color.icon_tint)))
        //    And       I see the “Password” list item with title, hidden value and a show icon
        onView(withText(R.string.resource_details_password_header)).check(matches(isDisplayed()))
        onView(withId(R.id.passwordValue)).check(matches(isDisplayed()))
        onView(withId(R.id.passwordIcon))
            .check(matches(isDisplayed()))
            .check(matches(hasDrawable(id = R.drawable.ic_eye_visible, tint = R.color.icon_tint)))
        //    And       I see the “Description” list item with title, hidden value and a show icon
        onView(withText(R.string.resource_details_description_header)).check(matches(isDisplayed()))
        onView(withId(R.id.descriptionValue)).check(matches(isDisplayed()))
        onView(withText(R.string.resource_details_see_description)).check(matches(not(isDisplayed())))
    }

    @Test
    fun asALoggedInMobileUserOnTheResourceDisplayICanShowOrHideResourceDescription() {
        //    Given     that I am a mobile user with the application installed
        //    And       the Passbolt application is already opened
        //    And       I completed the login step
        //    And       I am on a resource display screen
        // TODO in the future, when you change your environment, please change this resource
        onView(withId(R.id.searchEditText)).perform(click(), typeText("acft ed"))
        onView(withText("acft edt 2")).perform(click())
        //    When      I click on the show icon in the “Description” item list
        onView(withText(R.string.resource_details_see_description)).perform(click())
        //    Then      I see the description
        onView(withText("Tyuo")).check(matches(isDisplayed()))
    }

    @Test
    fun asALoggedInMobileUserOnTheResourceDisplayICanSeeTheRestOfALongDescriptionThatIsHigherThanTheScreen() {
        //    Given     that I am a mobile user with the application installed
        onView(withId(R.id.searchEditText)).perform(click(), typeText("long d"))
        //    When      I click on a resource
        onView(withText("long desc")).perform(click())
        onView(withText(R.string.resource_details_see_description)).perform(click())
        //    And       the description is unhidden
        //    And       the description height is taller than the height of the page
        //    When      I scroll the page
        onView(withId(R.id.root)).perform(swipeUp())
        //    Then      I see the rest of the description
    }

    @Test
    fun asALoggedInMobileUserOnTheResourceDisplayICanTriggerTheActionMenuAndCopyCredentialsToTheClipboard() {
        //    Given     that I am a mobile user with the application installed
        onView(withId(R.id.searchEditText)).perform(click(), typeText("face"))
        //    When      I click on a resource
        onView(withText("facebook")).perform(click())
        //    When      I click on the “3 dots” icon
        onView(withId(R.id.moreIcon)).perform(click())
        //    Then      I see the action menu drawer
        //    And       I see the name of the resource
        onView(withId(R.id.title)).check(matches(isDisplayed()))
        //    And       I see a cross button to go back to the resource display
        onView(withId(R.id.close)).check(matches(isDisplayed()))
        //    And       I see a { “Launch website”, "Copy url", "Copy password", "Copy username", "Copy description",
        //              "Add to favourite", "Share password", "Edit password", "Delete password" } item
        ResourcesDetailsItemModel.values().forEach { resourceItem ->
            onView(withId(resourceItem.resourceId))
                .check(matches(isDisplayed()))
                .check(matches(hasDrawable(id = resourceItem.resourceIconId, tint = resourceItem.resourceTintColorId)))
        }
    }
}
