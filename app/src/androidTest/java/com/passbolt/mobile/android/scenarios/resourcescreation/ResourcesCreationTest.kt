package com.passbolt.mobile.android.scenarios.resourcescreation

import android.view.KeyEvent
import androidx.appcompat.widget.Toolbar
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.pressKey
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasTextColor
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.jakewharton.espresso.OkHttp3IdlingResource
import com.passbolt.mobile.android.commontest.viewassertions.CastedViewAssertion
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
import com.passbolt.mobile.android.withHint
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Matchers
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.hasToString
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
class ResourcesCreationTest : KoinTest {

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
        onView(withId(R.id.input)).perform(
            typeText(managedAccountIntentCreator.getUsername()),
            pressKey(KeyEvent.KEYCODE_ENTER)
        )
        onView(withId(R.id.authButton)).perform(click())
        onView(withId(R.id.rootLayout)).check(matches(isDisplayed()))
        onView(withId(R.id.text_input_start_icon)).perform(click())
        onView(withId(R.id.allItems)).perform(click())
    }

    @Test
    fun asALoggedInMobileUserOnThePasswordWorkspaceIShouldSeeACreateButton() {
        //    Given     that I am a logged in mobile user
        //    When      I am on the password workspace
        //    And       I am on the <filter> filter
        //    And       I have any Passbolt version
        //    Then      I see a create button with an icon on <Position> in @blue
        //        Examples:
        //           | filter              |
        //           | “All items”         |
        //           | “Favourites”        |
        //           | “Recently modified” |
        //           | “Shared with me”    |
        //           | “Owned by me”       |
        //           | “Folders”           |
        VisibleCreateButton.values().forEach { visibleCreateButton ->
            onView(withId(R.id.text_input_start_icon)).perform(click())
            onView(withId(visibleCreateButton.filterId)).perform(click())
            onView(withId(R.id.speedDialViewId))
                .check(matches(isDisplayed()))
                .check(matches(hasDrawable(id = R.drawable.ic_plus, tint = R.color.icon_tint)))
        }
        //    And       I am on the <filter> filter
        //    And       I do not have any Passbolt version
        //    Then      I do not see a create button with an icon on <Position> in @blue
        //        Examples:
        //           | filter              |
        //           | “Tags”              |
        //           | “Groups”            |
        HiddenCreateButton.values().forEach { hiddenCreateButton ->
            onView(withId(R.id.text_input_start_icon)).perform(click())
            onView(withId(hiddenCreateButton.filterId)).perform(click())
            onView(withId(R.id.speedDialViewId))
                .check(matches(not(isDisplayed())))
        }
    }

    @Test
    fun asALoggedInMobileUserOnThePasswordWorkspaceIShouldSeeTheNewPasswordPage() {
        //    Given     I am a logged in mobile user
        //    And       I have at least the "can update" permission in the current context
        //    And       I see the create button
        //    When      I click on the create button
        //    Then      I see the New password page with "New password" title
        onView(withId(R.id.speedDialViewId)).perform(click())
        onView(withId(R.id.toolbar)).check(matches(isDisplayed()))
        //    And       I see a back arrow to go back to the previous page
        onView(isAssignableFrom(Toolbar::class.java))
            .check(CastedViewAssertion<Toolbar> { it.navigationIcon != null })
        //    And       I see a mandatory input text field with a "Name" and "Enter Name"
        onView(withText("Name *")).check(matches(isDisplayed()))
        onView(withHint(hasToString("Enter Name"))).check(matches(isDisplayed()))
        //    And       I see a optional input text field with a "URL" and "Enter URL"
        onView(withText("URL")).check(matches(isDisplayed()))
        onView(withHint(hasToString("Enter URL"))).check(matches(isDisplayed()))
        //    And       I see a optional input text field with a "Username" and "Enter Username"
        onView(withText("Username")).check(matches(isDisplayed()))
        onView(withHint(hasToString("Enter Username"))).check(matches(isDisplayed()))
        //    And       I see a mandatory input text field with a "Password" and "Enter password"
        onView(withText("Password *")).check(matches(isDisplayed()))
        onView(withHint(hasToString("Enter password"))).check(matches(isDisplayed()))
        //    And       I see a optional input text field with a "Description" and "Enter Description"
        onView(withText("Description")).check(matches(isDisplayed()))
        onView(withHint(hasToString("Enter Description"))).check(matches(isDisplayed()))
        //    And       I see a mandatory input text field with a "Show/Hide" button inside the field
        // TODO please add matcher to find visible icon - below line not working
//      onView(allOf(withHint(hasToString("Enter password")), withId(R.id.text_input_end_icon))).check(matches(isDisplayed()))
        //    And       I see a "Random" button on the right of the password field
        onView(withId(R.id.generatePasswordLayout)).check(matches(isDisplayed()))
        //    And       I see a "Strength" bar under the password field
        onView(withId(R.id.progressBar)).check(matches(isDisplayed()))
        //    And       I see a "Password strength" indicator under the "Strength" bar
        onView(withId(R.id.strengthDescription)).check(matches(isDisplayed()))
        //    And       I see a "Lock" button above the description field
        // TODO MOB-870 please add matcher to find lock icon - below lines not working
//        onView(allOf(withText("Description"), withId(R.id.icon))).check(matches(isDisplayed()))
//        onView(allOf(withHint(hasToString("Enter Description")), withId(R.id.icon))).check(matches(isDisplayed()))
        //    And       I see a "Create" primary button
        onView(withId(R.id.updateButton)).check(matches(isDisplayed()))
    }

    @Test
    fun asALoggedInMobileUserOnTheNewPasswordPageIShouldSeeAToastMessageAfterCreationAPassword() {
//    Given     I am a logged in mobile user on the new password page
        onView(withId(R.id.speedDialViewId)).perform(click())
//    And       I filled out all mandatory fields
        // TODO MOB-870 please add matcher to be able to type text in input - below lines not working
//        onView(withHint(hasToString("Enter Name"))).perform(typeText("PasswordNameTest"))
//        onView(allOf(withHint(hasToString("Enter Name")), withId(R.id.input))).perform(typeText("PasswordNameTest"))
        onView(withId(R.id.generatePasswordLayout)).perform(click())
//    When      I click on the create button
        onView(withId(R.id.updateButton)).perform(click())
//    Then      I see a "Loading" box
//    And       I am redirected to the password workspace
//    And       I see a toaster message with "New Password created"
    }

    @Test
    fun asALoggedInMobileUserOnTheNewPasswordPageIShouldSeeAnErrorMessageAfterClickingTheCreateButtonWithAnEmptyMandatoryField() {
//    Given     I am a logged in mobile user on the new password page
        onView(withId(R.id.speedDialViewId)).perform(click())
//    And       I didn't filled out the Name and Password fields
//    When      I click on the create button
        onView(withId(R.id.updateButton)).perform(click())
//    Then      I see the label of the Name and Password fields in @red
        onView(withText("Name *")).check(matches(hasTextColor(R.color.red)))
        onView(withText("Password *")).check(matches(hasTextColor(R.color.red)))
//    And       I see the stroke of the Name and Password fields in @red
        // TODO MOB-870 please add matcher to be able to find the stroke color
//    And       I see a error <Error> below the field in @red
        onView(withText("The name cannot be empty")).check(matches(isDisplayed()))
        // TODO MOB-870 please add matcher to be able to find color - below line not working
//        onView(withText("The name cannot be empty")).check(matches(hasTextColor(R.color.tint_red)))
        onView(withText("The password cannot be empty")).check(matches(isDisplayed()))
        // TODO MOB-870 please add matcher to be able to find color - below line not working
//        onView(withText("The password cannot be empty")).check(matches(hasTextColor(R.color.tint_red)))
//    | Field       | Error                            |
//    | Name        | "The name cannot be empty"       |
//    | Password    | "The password cannot be empty"  |
    }

    @Test
    fun asALoggedInMobileUserOnTheNewPasswordPageICanGenerateARandomPassword() {
//    Given     I am a logged in mobile user on the new password page
        onView(withId(R.id.speedDialViewId)).perform(click())
//    When      I click on the "Random" button
        onView(withId(R.id.generatePasswordLayout)).perform(click())
//    Then      I see the "Password" field is automatically filled in
//    And       I see the password is obfuscated
        // TODO MOB-870 please add matcher to be able to find input in component - below line not working
//        onView(allOf(withHint(hasToString("Enter password")), withId(R.id.input))).check(matches(isTextHidden()))
//    And       I see the "Strength" bar is green
        onView(withId(R.id.progressBar)).check(matches(isDisplayed()))
        // TODO MOB-870 please add matcher to be able to find color for progress bar - below line not working
//        onView(withId(R.id.progressBar)).check(matches(hasTextColor(R.color.green)))
//    And       I see the "password strength" text is "Very strong"
        onView(withId(R.id.strengthDescription)).check(matches(withText("Very strong")))
    }

    @Test
    fun asALoggedInMobileUserOnTheNewPasswordPageICanSwitchTheVisibilityOfThePassword() {
//    Given     I am a logged in mobile user on the new password page
        onView(withId(R.id.speedDialViewId)).perform(click())
//    And       the password field is not empty
        onView(withId(R.id.generatePasswordLayout)).perform(click())
//    And       the "show/hide" button is in a <State> state
        // TODO MOB-870 please add matcher to be able to find state icon and drawable to find color - below line not working
//        onView(withId(R.id.text_input_end_icon))
//            .check(matches(hasDrawable(id = R.drawable.ic_eye_visible, tint = R.color.icon_tint)))
//    When      I click on the "show/hide" button
        // TODO MOB-870 please add matcher to be able to click state icon - below line not working
//        onView(allOf(withHint(hasToString("Enter password")), withId(R.id.input))).perform(click())
//    Then      I can see the password <Visibility>

//    | State         | Visibility      |
//    | Visible       | in plain text   |
//    | Hidden        | obfuscated      |
    }
}
