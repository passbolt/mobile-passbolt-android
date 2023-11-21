package com.passbolt.mobile.android.scenarios.resourcesdetails

import android.view.KeyEvent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.pressKey
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.passbolt.mobile.android.core.idlingresource.ResourceDetailActionIdlingResource
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.component.inject
import org.koin.test.KoinTest
import kotlin.test.BeforeTest
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

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
        val resourceDetailActionIdlingResource: ResourceDetailActionIdlingResource by inject()
        IdlingResourceRule(
            arrayOf(
                signInIdlingResource,
                resourcesFullRefreshIdlingResource,
                resourceDetailActionIdlingResource
            )
        )
    }

    @BeforeTest
    fun setup() {
        onView(withId(CoreUiR.id.input)).perform(
            typeText(managedAccountIntentCreator.getUsername()),
            pressKey(KeyEvent.KEYCODE_ENTER)
        )
        onView(withId(com.passbolt.mobile.android.feature.authentication.R.id.authButton)).perform(click())
    }

    @Test
    fun asALoggedInMobileUserOnTheHomepageICanAccessTheResourceViewPage() {
        //    Given     that I am a mobile user with the application installed
        //    And       the Passbolt application is already opened
        //    And       I completed the login step
        //    And       I am on the homepage
        onView(withId(com.passbolt.mobile.android.feature.permissions.R.id.rootLayout)).check(matches(isDisplayed()))
        onView(withId(com.passbolt.mobile.android.feature.otp.R.id.searchEditText)).perform(click(), typeText("cake"))
        //    When      I click on a resource
        onView(withText("cakephp")).perform(click())
        //    Then      I see the resource display screen
        //    And       I see an arrow on the top left corner to go back to the previous page
        onView(withId(com.passbolt.mobile.android.feature.resources.R.id.backArrow))
            .check(matches(isDisplayed()))
            .check(matches(hasDrawable(id = CoreUiR.drawable.ic_arrow_left, tint = CoreUiR.color.icon_tint)))
        //    And       I see a “3 dots” icon on the top right corner
        onView(withId(com.passbolt.mobile.android.feature.resources.R.id.moreIcon))
            .check(matches(isDisplayed()))
            .check(matches(hasDrawable(id = CoreUiR.drawable.ic_more, tint = CoreUiR.color.icon_tint)))
        //    And       I see the resource favicon or a default icon
        onView(withId(R.id.icon)).check(matches(isDisplayed()))
        //    And       I see the resource name
        onView(allOf(withId(com.passbolt.mobile.android.feature.otp.R.id.name), withText("cakephp"))).check(
            matches(
                isDisplayed()
            )
        )
        //    And       I see the “Website URL” list item with title, value and a copy icon
        onView(withText(LocalizationR.string.resource_details_url_header)).check(matches(isDisplayed()))
        onView(withId(com.passbolt.mobile.android.feature.resources.R.id.urlItem)).check(matches(isDisplayed()))
        onView(
            allOf(
                isDescendantOfA(withId(com.passbolt.mobile.android.feature.resources.R.id.usernameItem)),
                withId(com.passbolt.mobile.android.core.ui.R.id.actionIcon)
            )
        )
            .check(matches(isDisplayed()))
            .check(matches(hasDrawable(id = CoreUiR.drawable.ic_copy, tint = CoreUiR.color.icon_tint)))
        //    And       I see the “Username” list item with title, value and a copy icon
        onView(withText(LocalizationR.string.resource_details_username_header)).check(matches(isDisplayed()))
        onView(withId(com.passbolt.mobile.android.feature.resources.R.id.usernameItem)).check(matches(isDisplayed()))
        onView(
            allOf(
                isDescendantOfA(withId(com.passbolt.mobile.android.feature.resources.R.id.usernameItem)),
                withId(com.passbolt.mobile.android.core.ui.R.id.actionIcon)
            )
        )
            .check(matches(isDisplayed()))
            .check(matches(hasDrawable(id = CoreUiR.drawable.ic_copy, tint = CoreUiR.color.icon_tint)))
        //    And       I see the “Password” list item with title, hidden value and a show icon
        onView(withText(LocalizationR.string.resource_details_password_header)).check(matches(isDisplayed()))
        onView(withId(com.passbolt.mobile.android.feature.resources.R.id.passwordItem)).check(matches(isDisplayed()))
        onView(
            allOf(
                isDescendantOfA(withId(com.passbolt.mobile.android.feature.resources.R.id.passwordItem)),
                withId(com.passbolt.mobile.android.core.ui.R.id.actionIcon)
            )
        )
            .check(matches(isDisplayed()))
            .check(matches(hasDrawable(id = CoreUiR.drawable.ic_eye_visible, tint = CoreUiR.color.icon_tint)))
        //    And       I see the “Description” list item with title, hidden value and a show icon
        onView(withText(LocalizationR.string.resource_details_description_header)).check(matches(isDisplayed()))
        onView(withId(com.passbolt.mobile.android.feature.resources.R.id.descriptionItem)).check(matches(isDisplayed()))
    }

    @Test
    // https://passbolt.testrail.io/index.php?/cases/view/2447
    fun asALoggedInMobileUserOnTheResourceDisplayICanShowOrHideResourceDescription() {
        //    Given     that I am a mobile user with the application installed
        //    And       the Passbolt application is already opened
        //    And       I completed the login step
        //    And       I am on a resource display screen
        onView(withId(com.passbolt.mobile.android.feature.otp.R.id.searchEditText)).perform(
            click(),
            typeText("TestResourceDesc")
        )
        onView(withText("TestResourceDescription")).perform(click())
        //    When      I click on the show icon in the “Description” item list
        onView(
            allOf(
                isDescendantOfA(withId(com.passbolt.mobile.android.feature.resources.R.id.descriptionItem)),
                withId(com.passbolt.mobile.android.core.ui.R.id.actionIcon)
            )
        )
            .perform(click())
        //    Then      I see the description
        onView(withText("Luxembourg")).check(matches(isDisplayed()))
    }

    @Test
    fun asALoggedInMobileUserOnTheResourceDisplayICanSeeTheRestOfALongDescriptionThatIsHigherThanTheScreen() {
        //    Given     that I am a mobile user with the application installed
        onView(withId(com.passbolt.mobile.android.feature.otp.R.id.searchEditText)).perform(click(), typeText("long d"))
        //    When      I click on a resource
        onView(withText("long desc")).perform(click())
        onView(
            allOf(
                isDescendantOfA(withId(com.passbolt.mobile.android.feature.resources.R.id.descriptionItem)),
                withId(com.passbolt.mobile.android.core.ui.R.id.actionIcon)
            )
        )
            .perform(click())
        //    And       the description is unhidden
        //    And       the description height is taller than the height of the page
        //    When      I scroll the page
        onView(withId(com.passbolt.mobile.android.feature.resources.R.id.root)).perform(swipeUp())
        //    Then      I see the rest of the description
    }

    @Test
    fun asALoggedInMobileUserOnTheResourceDisplayICanTriggerTheActionMenuAndCopyCredentialsToTheClipboard() {
        //    Given     that I am a mobile user with the application installed
        onView(withId(com.passbolt.mobile.android.feature.otp.R.id.searchEditText)).perform(click(), typeText("face"))
        //    When      I click on a resource
        onView(withText("facebook")).perform(click())
        //    When      I click on the “3 dots” icon
        onView(withId(com.passbolt.mobile.android.feature.resources.R.id.moreIcon)).perform(click())
        //    Then      I see the action menu drawer
        //    And       I see the name of the resource
        onView(withId(R.id.title)).check(matches(isDisplayed()))
        //    And       I see a cross button to go back to the resource display
        onView(withId(com.passbolt.mobile.android.feature.autofill.R.id.close)).check(matches(isDisplayed()))
        //    And       I see a { “Launch website”, "Copy url", "Copy password", "Copy username", "Copy description",
        //              "Add to favourite", "Share password", "Edit password", "Delete password" } item
        ResourcesDetailsItemModel.values().forEach { resourceItem ->
            onView(withId(resourceItem.resourceId))
                .check(matches(isDisplayed()))
                .check(matches(hasDrawable(id = resourceItem.resourceIconId, tint = resourceItem.resourceTintColorId)))
        }
    }
}
