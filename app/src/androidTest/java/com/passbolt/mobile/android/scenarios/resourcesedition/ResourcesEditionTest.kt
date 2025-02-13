/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2023-2025 Passbolt SA
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

package com.passbolt.mobile.android.scenarios.resourcesedition

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasTextColor
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.textfield.TextInputLayout
import com.passbolt.mobile.android.core.idlingresource.CreateResourceIdlingResource
import com.passbolt.mobile.android.core.idlingresource.ResourcesFullRefreshIdlingResource
import com.passbolt.mobile.android.core.idlingresource.SignInIdlingResource
import com.passbolt.mobile.android.core.idlingresource.UpdateResourceIdlingResource
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.feature.authentication.AuthenticationMainActivity
import com.passbolt.mobile.android.helpers.createNewPasswordFromHomeScreen
import com.passbolt.mobile.android.helpers.pickFirstResourceWithName
import com.passbolt.mobile.android.helpers.signIn
import com.passbolt.mobile.android.instrumentationTestsModule
import com.passbolt.mobile.android.intents.ManagedAccountIntentCreator
import com.passbolt.mobile.android.matchers.first
import com.passbolt.mobile.android.matchers.hasDrawable
import com.passbolt.mobile.android.matchers.withHint
import com.passbolt.mobile.android.matchers.withTextInputStrokeColorOf
import com.passbolt.mobile.android.rules.IdlingResourceRule
import com.passbolt.mobile.android.rules.lazyActivitySetupScenarioRule
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.equalTo
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
import com.passbolt.mobile.android.feature.otp.R as OtpR
import com.passbolt.mobile.android.feature.resourcemoremenu.R.id as ResourcemoremenuId
import com.passbolt.mobile.android.feature.resources.R.id as ResourcesID


@RunWith(AndroidJUnit4::class)
@MediumTest
class ResourcesEditionTest : KoinTest {

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
        val updateResourceIdlingResource: UpdateResourceIdlingResource by inject()
        val createResourceIdlingResource: CreateResourceIdlingResource by inject()
        IdlingResourceRule(
            arrayOf(
                signInIdlingResource,
                resourcesFullRefreshIdlingResource,
                updateResourceIdlingResource,
                createResourceIdlingResource
            )
        )
    }

    @BeforeTest
    fun setup() {
        signIn(managedAccountIntentCreator.getPassphrase())
        onView(withId(MaterialR.id.text_input_start_icon)).perform(click())
        onView(withId(com.passbolt.mobile.android.feature.home.R.id.allItems)).perform(click())
        createNewPasswordFromHomeScreen("ResourcesEditionTest")
    }

    @Test
//    https://passbolt.testrail.io/index.php?/cases/view/8134
    fun onTheResourcesActionMenuDrawerICanClickEditPassword() {
        //    Given     that I am on the action menu drawer
        pickFirstResourceWithName("ResourcesEditionTest")
        //    And       I see ‘Edit password’ element enabled
        onView(first(withId(ResourcesID.moreIcon))).perform(click())
        onView(withId(ResourcemoremenuId.editPassword))
            .check(matches(isDisplayed()))
            .check(matches(hasDrawable(id = CoreUiR.drawable.ic_edit, tint = CoreUiR.color.icon_tint)))
        //    When      I click ‘Edit password’
        onView(withId(ResourcemoremenuId.editPassword)).perform(click())
        //    Then      I am on `Edit password` screen
        onView(withText(LocalizationR.string.resource_update_edit_password_title)).check(matches(isDisplayed()))
    }

    @Test
//    https://passbolt.testrail.io/index.php?/cases/view/8135
    fun onTheEditPasswordPageICanEditElements() {
        //    Given     that I am on `Edit password` screen
        enterEditPasswordScreen()
        //    And       I see Edit password workspace
        onView(withText(LocalizationR.string.resource_update_edit_password_title)).check(matches(isDisplayed()))
        //    And       <placeholder> is filled
        EditableFieldInput.entries.forEach { editableInputField ->
            onViewInputWithHintName(editableInputField.hintName)
                .check(matches(isDisplayed()))
        }
        //    When      I change <placeholder>
        EditableFieldInput.entries.forEach { editableInputField ->
            onViewInputWithHintName(editableInputField.hintName)
                .perform(replaceText(editableInputField.textToReplace))
        }
        //    Then      <placeholder> is changed
        EditableFieldInput.entries.forEach { editableInputField ->
            onViewInputWithHintName(editableInputField.hintName)
                .check(matches(withText(editableInputField.textToReplace)))
        }
        //    Examples:
        //    | placeholder |
        //    | Enter a name |
        //    | Enter URL |
        //    | Enter username |
        //    | Enter a password |
        //    | Enter description |
    }


    @Test
//    https://passbolt.testrail.io/index.php?/cases/view/8136
    fun onTheEditPasswordPageICanSaveChangedResources() {
        //    Given     that I am on `Edit password` screen
        enterEditPasswordScreen()
        //    And       I see Edit password workspace
        onView(withText(LocalizationR.string.resource_update_edit_password_title)).check(matches(isDisplayed()))
        //    And       <placeholder> was changed
        EditableFieldInput.entries.forEach { editableInputField ->
            onViewInputWithHintName(editableInputField.hintName)
                .perform(replaceText(editableInputField.textToReplace))
        }
        //    When      I click ‘Save’ button
        onView(withId(ResourcesID.updateButton)).perform(scrollTo(), click())
        //    Examples:
        //    | placeholder |
        //    | Enter a name |
        //    | Enter URL |
        //    | Enter username |
        //    | Enter a password |
        //    | Enter description |
    }

    @Test
//    https://passbolt.testrail.io/index.php?/cases/view/8137
    fun onTheEditPasswordPageIShouldSeeAnErrorMessageAfterDeletingTheMandatoryTextField() {
        //    Given     that I am on `Edit password` screen
        enterEditPasswordScreen()
        //    And       all placeholders are filled
        EditableFieldInput.entries.forEach { editableInputField ->
            onViewInputWithHintName(editableInputField.hintName)
                .check(matches(isDisplayed()))
        }
        //    When      I delete <placeholder> text field
        onViewInputWithHintName(EditableFieldInput.ENTER_NAME.hintName)
            .perform(replaceText(""))
        onViewInputWithHintName(EditableFieldInput.ENTER_PASSWORD.hintName)
            .perform(replaceText(""))
        //    Then      I click Save button
        onView(withId(ResourcesID.updateButton)).perform(scrollTo(), click())
        //    And       I see <placeholder> label in @red
        onView(withText("Resource name *")).check(matches(hasTextColor(CoreUiR.color.red)))
        onView(withText("Password *")).check(matches(hasTextColor(CoreUiR.color.red)))
        //    And       I see <placeholder> frame in @red
        onViewTextInputLayoutWithHintName(EditableFieldInput.ENTER_NAME.hintName)
            .check(matches(withTextInputStrokeColorOf(CoreUiR.color.red)))
        onViewTextInputLayoutWithHintName(EditableFieldInput.ENTER_PASSWORD.hintName)
            .check(matches(withTextInputStrokeColorOf(CoreUiR.color.red)))
        //    And       see exclamation mark
        //    And       I see information: "The <placeholder> cannot be empty"
        onView(withText("The name cannot be empty"))
            .check(matches(isDisplayed()))
            .check(matches(hasTextColor(CoreUiR.color.red)))
        onView(withText("The password cannot be empty"))
            .check(matches(isDisplayed()))
            .check(matches(hasTextColor(CoreUiR.color.red)))
        //    Examples:
        //    | placeholder |
        //    | Enter a name |
        //    | Enter a password |
    }

    @Test
//    https://passbolt.testrail.io/index.php?/cases/view/8138
    fun onTheEditPasswordPageICanDeleteTheOptionalInputTextField() {
        //    Given     that I am on `Edit password` screen
        enterEditPasswordScreen()
        //    And       all placeholders are filled
        EditableFieldInput.entries.forEach { editableInputField ->
            onViewInputWithHintName(editableInputField.hintName)
                .check(matches(isDisplayed()))
        }
        //    When      I delete <placeholder> text field
        onViewInputWithHintName(EditableFieldInput.ENTER_URL.hintName)
            .perform(replaceText(""))
        onViewInputWithHintName(EditableFieldInput.ENTER_USERNAME.hintName)
            .perform(replaceText(""))
        onViewInputWithHintName(EditableFieldInput.ENTER_DESCRIPTION.hintName)
            .perform(replaceText(""))
        //    Then      I delete <placeholder> text field
        onViewInputWithHintName(EditableFieldInput.ENTER_URL.hintName)
            .check(matches(withText("")))
        onViewInputWithHintName(EditableFieldInput.ENTER_USERNAME.hintName)
            .check(matches(withText("")))
        onViewInputWithHintName(EditableFieldInput.ENTER_DESCRIPTION.hintName)
            .check(matches(withText("")))
        //    Examples:
        //    | placeholder |
        //    | Enter URL |
        //    | Enter username |
        //    | Enter description |
    }

    @Test
    //  https://passbolt.testrail.io/index.php?/cases/view/8139
    fun onTheEditPasswordPageISeeConfirmationPopupWhenSavingAChangedResource() {
        // unregister refresh idling resource after first refresh not to block the snackbar checks
        // (second refresh is during snackbar is showing)
        IdlingRegistry.getInstance().unregister(resourcesFullRefreshIdlingResource)
        enterEditPasswordScreen()
        //    And       all placeholders are filled
        EditableFieldInput.entries.forEach { editableInputField ->
            onViewInputWithHintName(editableInputField.hintName)
                .check(matches(isDisplayed()))
        }
        //    And       I delete <placeholder> text field
        onViewInputWithHintName(EditableFieldInput.ENTER_NAME.hintName)
            .perform(replaceText(EditableFieldInput.ENTER_NAME.textToReplace))
        onViewInputWithHintName(EditableFieldInput.ENTER_URL.hintName)
            .perform(replaceText(""))
        onViewInputWithHintName(EditableFieldInput.ENTER_USERNAME.hintName)
            .perform(replaceText(""))
        onViewInputWithHintName(EditableFieldInput.ENTER_DESCRIPTION.hintName)
            .perform(replaceText(""))
        //    When      I click "Save" button
        onView(withId(ResourcesID.updateButton)).perform(scrollTo(), click())
        //    Then      I see a popup "{password name} password was successfully edited." in @green
        onView(withId(com.passbolt.mobile.android.feature.permissions.R.id.rootLayout)).check(matches(isDisplayed()))
        onView(withId(MaterialR.id.snackbar_text))
            .check(matches(withText(CoreMatchers.endsWith("password was successfully edited."))))
        //    Examples:
        //    | placeholder |
        //    | Enter URL |
        //    | Enter username |
        //    | Enter description |
    }

    private fun onViewInputWithHintName(hintName: String): ViewInteraction =
        onView(allOf(isDescendantOfA(withHint(equalTo(hintName))), withId(CoreUiR.id.input)))

    private fun onViewTextInputLayoutWithHintName(hintName: String): ViewInteraction = onView(
        allOf(
            isDescendantOfA(withHint(hasToString(hintName))),
            isAssignableFrom(TextInputLayout::class.java)
        )
    )

    private fun enterEditPasswordScreen() {
        onView(withId(OtpR.id.searchEditText)).perform(typeText("ResourcesEditionTest"))
        onView(first(withId(OtpR.id.more))).perform(click())
        onView(withId(ResourcemoremenuId.editPassword)).perform(click())
    }
}
