/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021-2023 Passbolt SA
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

package com.passbolt.mobile.android.scenarios.resourcescreation

import android.widget.ProgressBar
import androidx.appcompat.widget.Toolbar
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.hasTextColor
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.textfield.TextInputLayout
import com.passbolt.mobile.android.commontest.viewassertions.CastedViewAssertion
import com.passbolt.mobile.android.core.idlingresource.CreateResourceIdlingResource
import com.passbolt.mobile.android.core.idlingresource.ResourcesFullRefreshIdlingResource
import com.passbolt.mobile.android.core.idlingresource.SignInIdlingResource
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.core.ui.textinputfield.PasswordGenerateInputView
import com.passbolt.mobile.android.core.ui.textinputfield.PasswordGenerateInputView.PasswordStrength.VeryStrong
import com.passbolt.mobile.android.feature.authentication.AuthenticationMainActivity
import com.passbolt.mobile.android.feature.setup.R
import com.passbolt.mobile.android.hasDrawable
import com.passbolt.mobile.android.instrumentationTestsModule
import com.passbolt.mobile.android.intents.ManagedAccountIntentCreator
import com.passbolt.mobile.android.isTextHidden
import com.passbolt.mobile.android.rules.IdlingResourceRule
import com.passbolt.mobile.android.rules.lazyActivitySetupScenarioRule
import com.passbolt.mobile.android.scenarios.actions.clickOnPasswordToggle
import com.passbolt.mobile.android.scenarios.helpers.getString
import com.passbolt.mobile.android.scenarios.helpers.signIn
import com.passbolt.mobile.android.withHint
import com.passbolt.mobile.android.withProgressBarOfMinimumProgress
import com.passbolt.mobile.android.withTextInputStrokeColorOf
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.hasToString
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

    private val resourcesFullRefreshIdlingResource: ResourcesFullRefreshIdlingResource by inject()

    @get:Rule
    val idlingResourceRule = let {
        val signInIdlingResource: SignInIdlingResource by inject()
        val createResourceIdlingResource: CreateResourceIdlingResource by inject()
        IdlingResourceRule(
            arrayOf(
                signInIdlingResource,
                resourcesFullRefreshIdlingResource,
                createResourceIdlingResource
            )
        )
    }

    @BeforeTest
    fun setup() {
        signIn(managedAccountIntentCreator.getPassphrase())
        onView(withId(MaterialR.id.text_input_start_icon)).perform(click())
        onView(withId(com.passbolt.mobile.android.feature.home.R.id.allItems)).perform(click())
    }

    @Test
//  https://passbolt.testrail.io/index.php?/cases/view/6348
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
        VisibleCreateButton.entries.forEach { visibleCreateButton ->
            onView(withId(MaterialR.id.text_input_start_icon)).perform(click())
            onView(withId(visibleCreateButton.filterId)).perform(click())
            onView(withId(com.passbolt.mobile.android.feature.home.R.id.homeSpeedDialViewId))
                .check(matches(isDisplayed()))
                .check(matches(hasDrawable(id = CoreUiR.drawable.ic_plus, tint = CoreUiR.color.icon_tint)))
        }
        //    And       I am on the <filter> filter
        //    And       I do not have any Passbolt version
        //    Then      I do not see a create button with an icon on <Position> in @blue
        //        Examples:
        //           | filter              |
        //           | “Tags”              |
        //           | “Groups”            |
        HiddenCreateButton.entries.forEach { hiddenCreateButton ->
            onView(withId(MaterialR.id.text_input_start_icon)).perform(click())
            onView(withId(hiddenCreateButton.filterId)).perform(click())
            onView(withId(com.passbolt.mobile.android.feature.home.R.id.homeSpeedDialViewId))
                .check(matches(not(isDisplayed())))
        }
    }

    @Test
//  https://passbolt.testrail.io/index.php?/cases/view/8128
    fun asALoggedInMobileUserOnThePasswordWorkspaceIShouldSeeTheNewPasswordPage() {
        //    Given     I am a logged in mobile user
        //    And       I have at least the "can update" permission in the current context
        //    And       I see the create button
        //    When      I click on the create button
        //    Then      I see the New password page with "New password" title
        onView(withId(com.passbolt.mobile.android.feature.home.R.id.homeSpeedDialViewId)).perform(click())
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
        onView(
            allOf(
                isDescendantOfA(withHint(hasToString("Enter password"))),
                withId(MaterialR.id.text_input_end_icon)
            )
        )
            .check(matches(isDisplayed()))
            .check(matches(withContentDescription(MaterialR.string.password_toggle_content_description)))
        //    And       I see a "Random" button on the right of the password field
        onView(withId(CoreUiR.id.generatePasswordLayout)).check(matches(isDisplayed()))
        //    And       I see a "Strength" bar under the password field
        onView(withId(CoreUiR.id.progressBar)).check(matches(isDisplayed()))
        //    And       I see a "Password strength" indicator under the "Strength" bar
        onView(withId(CoreUiR.id.strengthDescription)).check(matches(isDisplayed()))
        //    And       I see a "Lock" button above the description field
        onView(allOf(hasSibling(withText("Description")), withId(R.id.icon)))
            .check(matches(isDisplayed()))
        //    And       I see a "Create" primary button
        onView(withId(com.passbolt.mobile.android.feature.resources.R.id.updateButton)).check(matches(isDisplayed()))
    }

    @Test
    //  https://passbolt.testrail.io/index.php?/cases/view/8130
    fun asALoggedInMobileUserOnTheNewPasswordPageIShouldSeeAToastMessageAfterCreationAPassword() {
        // unregister refresh idling resource after first refresh not to block the snackbar checks
        // (second refresh is during snackbar is showing)
        IdlingRegistry.getInstance().unregister(resourcesFullRefreshIdlingResource)

        //    Given     I am a logged in mobile user on the new password page
        onView(withId(com.passbolt.mobile.android.feature.home.R.id.homeSpeedDialViewId)).perform(click())
        //    And       I filled out all mandatory fields
        onView(allOf(isDescendantOfA(withHint(hasToString("Enter Name"))), withId(CoreUiR.id.input)))
            .perform(
                typeText("PasswordNameTest"),
                closeSoftKeyboard(),
            )
        onView(withId(CoreUiR.id.generatePasswordLayout)).perform(click())
        //    When      I click on the create button
        onView(withId(com.passbolt.mobile.android.feature.resources.R.id.updateButton))
            .perform(scrollTo(), click())
        //    Then      I see a "Loading" box
        //    And       I am redirected to the password workspace
        onView(withId(com.passbolt.mobile.android.feature.permissions.R.id.rootLayout)).check(matches(isDisplayed()))
        //    And       I see a toaster message with "New Password created"
        onView(withId(MaterialR.id.snackbar_text))
            .check(matches(withText(LocalizationR.string.resource_update_create_success)))
    }

    @Test
//  https://passbolt.testrail.io/index.php?/cases/view/8131
    fun asALoggedInMobileUserOnTheNewPasswordPageIShouldSeeAnErrorMessageAfterClickingTheCreateButtonWithAnEmptyMandatoryField() {
        //    Given     I am a logged in mobile user on the new password page
        onView(withId(com.passbolt.mobile.android.feature.home.R.id.homeSpeedDialViewId)).perform(click())
        //    And       I didn't filled out the Name and Password fields
        //    When      I click on the create button
        onView(withId(com.passbolt.mobile.android.feature.resources.R.id.updateButton)).perform(click())
        //    Then      I see the label of the Name and Password fields in @red
        onView(withText("Name *")).check(matches(hasTextColor(CoreUiR.color.red)))
        onView(withText("Password *")).check(matches(hasTextColor(CoreUiR.color.red)))
        //    And       I see the stroke of the Name and Password fields in @red
        onView(
            allOf(
                isDescendantOfA(withHint(hasToString("Enter Name"))),
                isAssignableFrom(TextInputLayout::class.java)
            )
        )
            .check(matches(withTextInputStrokeColorOf(CoreUiR.color.red)))
        onView(
            allOf(
                isDescendantOfA(withHint(hasToString("Enter password"))),
                isAssignableFrom(TextInputLayout::class.java)
            )
        )
            .check(matches(withTextInputStrokeColorOf(CoreUiR.color.red)))
        //    And       I see a error <Error> below the field in @red
        onView(withText("The name cannot be empty"))
            .check(matches(isDisplayed()))
            .check(matches(hasTextColor(CoreUiR.color.red)))
        onView(withText("The password cannot be empty"))
            .check(matches(isDisplayed()))
            .check(matches(hasTextColor(CoreUiR.color.red)))
        //    | Field       | Error                            |
        //    | Name        | "The name cannot be empty"       |
        //    | Password    | "The password cannot be empty"  |
    }

    @Test
    //  https://passbolt.testrail.io/index.php?/cases/view/8132
    fun asALoggedInMobileUserOnTheNewPasswordPageICanGenerateARandomPassword() {
        //    Given     I am a logged in mobile user on the new password page
        onView(withId(com.passbolt.mobile.android.feature.home.R.id.homeSpeedDialViewId)).perform(click())
        //    When      I click on the "Random" button
        onView(withId(CoreUiR.id.generatePasswordLayout)).perform(click())
        //    Then      I see the "Password" field is automatically filled in
        //    And       I see the password is obfuscated
        onView(allOf(isDescendantOfA(withHint(hasToString("Enter password"))), withId(CoreUiR.id.input)))
            .check(matches(isTextHidden()))
        //    And       I see the "Strength" bar is green
        onView(
            allOf(
                isDescendantOfA(isAssignableFrom(PasswordGenerateInputView::class.java)),
                isAssignableFrom(ProgressBar::class.java)
            )
        )
            .check(matches(isDisplayed()))
            .check(matches(withProgressBarOfMinimumProgress(VeryStrong.progress)))
        //    And       I see the "password strength" text is "Very strong"
        onView(withId(CoreUiR.id.strengthDescription))
            .check(matches(withText(VeryStrong.text)))
    }

    @Test
    //  https://passbolt.testrail.io/index.php?/cases/view/8133
    fun asALoggedInMobileUserOnTheNewPasswordPageICanSwitchTheVisibilityOfThePassword() {
        val password = "password"

        //    Given     I am a logged in mobile user on the new password page
        onView(withId(com.passbolt.mobile.android.feature.home.R.id.homeSpeedDialViewId))
            .perform(click())
        //    And       the password field is not empty
        onPasswordInputView()
            .perform(typeText(password))
        //    And       the "show/hide" button is in a <State> state
        onPasswordInputView()
            .check(matches(isTextHidden()))
        //    When      I click on the "show/hide" button
        clickOnPasswordToggle()
        //    Then      I can see the password <Visibility>
        onPasswordInputView()
            .check(matches(withText(password)))
        //    | State         | Visibility      |
        //    | Visible       | in plain text   |
        //    | Hidden        | obfuscated      |
    }

    private fun onPasswordInputView() =
        onView(
            allOf(
                isDescendantOfA(withHint(equalTo(getString(LocalizationR.string.resource_update_password_hint)))),
                withId(CoreUiR.id.input)
            )
        )
}
