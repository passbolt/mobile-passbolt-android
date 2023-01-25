package com.passbolt.mobile.android.scenarios.resourcesedition

import android.view.KeyEvent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.pressKey
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
import com.jakewharton.espresso.OkHttp3IdlingResource
import com.passbolt.mobile.android.core.idlingresource.ResourcesFullRefreshIdlingResource
import com.passbolt.mobile.android.core.idlingresource.SignInIdlingResource
import com.passbolt.mobile.android.core.idlingresource.UpdateResourceIdlingResource
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.feature.authentication.AuthenticationMainActivity
import com.passbolt.mobile.android.feature.setup.R
import com.passbolt.mobile.android.first
import com.passbolt.mobile.android.hasDrawable
import com.passbolt.mobile.android.instrumentationTestsModule
import com.passbolt.mobile.android.intents.ManagedAccountIntentCreator
import com.passbolt.mobile.android.rules.IdlingResourceRule
import com.passbolt.mobile.android.rules.lazyActivitySetupScenarioRule
import com.passbolt.mobile.android.withHint
import com.passbolt.mobile.android.withTextInputStrokeColorOf
import org.hamcrest.CoreMatchers.equalTo
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
 * Copyright (c) 2023 Passbolt SA
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

    @get:Rule
    val idlingResourceRule = let {
        val signInIdlingResource: SignInIdlingResource by inject()
        val resourcesFullRefreshIdlingResource: ResourcesFullRefreshIdlingResource by inject()
        val okHttpIdlingResource: OkHttp3IdlingResource by inject()
        val updateResourceIdlingResource: UpdateResourceIdlingResource by inject()
        IdlingResourceRule(
            arrayOf(
                signInIdlingResource,
                resourcesFullRefreshIdlingResource,
                okHttpIdlingResource,
                updateResourceIdlingResource
            )
        )
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
        //    Given     that I am on the action menu drawer
        onView(withId(R.id.searchEditText)).perform(typeText("ResourcesEditionTestPK"))
        onView(first(withId(R.id.more))).perform(click())
    }

    @Test
//    https://passbolt.testrail.io/index.php?/cases/view/8134
    fun onTheResourcesActionMenuDrawerICanClickEditPassword() {
        //    Given     that I am on the action menu drawer
        //    And       I see ‘Edit password’ element enabled
        onView(withId(R.id.edit))
            .check(matches(isDisplayed()))
            .check(matches(hasDrawable(id = R.drawable.ic_edit, tint = R.color.icon_tint)))
        //    When      I click ‘Edit password’
        onView(withId(R.id.edit)).perform(click())
        //    Then      I am on `Edit password` screen
        onView(withText(R.string.resource_update_edit_password_title)).check(matches(isDisplayed()))
    }

    @Test
//    https://passbolt.testrail.io/index.php?/cases/view/8135
    fun onTheEditPasswordPageICanEditElements() {
        //    Given     that I am on `Edit password` screen
        onView(withId(R.id.edit)).perform(click())
        //    And       I see Edit password workspace
        onView(withText(R.string.resource_update_edit_password_title)).check(matches(isDisplayed()))
        //    And       <placeholder> is filled
        EditableFieldInput.values().forEach { editableInputField ->
            onViewInputWithHintName(editableInputField.hintName)
                .check(matches(isDisplayed()))
        }
        //    When      I change <placeholder>
        EditableFieldInput.values().forEach { editableInputField ->
            onViewInputWithHintName(editableInputField.hintName)
                .perform(replaceText(editableInputField.textToReplace))
        }
        //    Then      <placeholder> is changed
        EditableFieldInput.values().forEach { editableInputField ->
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
        onView(withId(R.id.edit)).perform(click())
        //    And       I see Edit password workspace
        onView(withText(R.string.resource_update_edit_password_title)).check(matches(isDisplayed()))
        //    And       <placeholder> was changed
        EditableFieldInput.values().forEach { editableInputField ->
            onViewInputWithHintName(editableInputField.hintName)
                .perform(replaceText(editableInputField.textToReplace))
        }
        //    When      I click ‘Save’ button
        onView(withId(R.id.updateButton)).perform(scrollTo(), click())
        //    Then      I see a popup "{password name} password was successfully edited." in @green
        //        TODO idling not working
        //        onView(withText(R.string.common_message_resource_edited))
        //            .inRoot(hasToast())
        //            .check(matches(isDisplayed()))
        //        createNewPassword()
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
        onView(withId(R.id.edit)).perform(click())
        //    And       all placeholders are filled
        EditableFieldInput.values().forEach { editableInputField ->
            onViewInputWithHintName(editableInputField.hintName)
                .check(matches(isDisplayed()))
        }
        //    When      I delete <placeholder> text field
        onViewInputWithHintName(EditableFieldInput.ENTER_NAME.hintName)
            .perform(replaceText(""))
        onViewInputWithHintName(EditableFieldInput.ENTER_PASSWORD.hintName)
            .perform(replaceText(""))
        //    Then      I click Save button
        onView(withId(R.id.updateButton)).perform(scrollTo(), click())
        //    And       I see <placeholder> label in @red
        onView(withText("Name *")).check(matches(hasTextColor(R.color.red)))
        onView(withText("Password *")).check(matches(hasTextColor(R.color.red)))
        //    And       I see <placeholder> frame in @red
        onViewTextInputLayoutWithHintName(EditableFieldInput.ENTER_NAME.hintName)
            .check(matches(withTextInputStrokeColorOf(R.color.red)))
        onViewTextInputLayoutWithHintName(EditableFieldInput.ENTER_PASSWORD.hintName)
            .check(matches(withTextInputStrokeColorOf(R.color.red)))
        //    And       see exclamation mark
        //    And       I see information: "The <placeholder> cannot be empty"
        onView(withText("The name cannot be empty"))
            .check(matches(isDisplayed()))
            .check(matches(hasTextColor(R.color.red)))
        onView(withText("The password cannot be empty"))
            .check(matches(isDisplayed()))
            .check(matches(hasTextColor(R.color.red)))
        //    Examples:
        //    | placeholder |
        //    | Enter a name |
        //    | Enter a password |
    }

    @Test
//    https://passbolt.testrail.io/index.php?/cases/view/8138
    fun onTheEditPasswordPageICanDeleteTheOptionalInputTextField() {
        //    Given     that I am on `Edit password` screen
        onView(withId(R.id.edit)).perform(click())
        //    And       all placeholders are filled
        EditableFieldInput.values().forEach { editableInputField ->
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
//    https://passbolt.testrail.io/index.php?/cases/view/8139
    fun onTheEditPasswordPageISeeConfirmationPopupWhenSavingAChangedResource() {
        //    Given     that I am on `Edit password` screen
        onView(withId(R.id.edit)).perform(click())
        //    And       all placeholders are filled
        EditableFieldInput.values().forEach { editableInputField ->
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
        onView(withId(R.id.updateButton)).perform(scrollTo(), click())
        //    Then      I see a popup "{password name} password was successfully edited." in @green
        //        TODO idling not working
        //        onView(withText(R.string.common_message_resource_edited))
        //            .inRoot(hasToast())
        //            .check(matches(isDisplayed()))
        //        createNewPassword()
        //    Examples:
        //    | placeholder |
        //    | Enter URL |
        //    | Enter username |
        //    | Enter description |
    }

    private fun onViewInputWithHintName(hintName: String): ViewInteraction =
        onView(allOf(isDescendantOfA(withHint(equalTo(hintName))), withId(R.id.input)))

    private fun onViewTextInputLayoutWithHintName(hintName: String): ViewInteraction = onView(
        allOf(
            isDescendantOfA(withHint(hasToString(hintName))),
            isAssignableFrom(TextInputLayout::class.java)
        )
    )

    private fun createNewPassword() {
        onView(withId(R.id.speedDialViewId)).perform(click())
        onView(allOf(isDescendantOfA(withHint(hasToString("Enter Name"))), withId(R.id.input)))
            .perform(typeText("ResourcesEditionTestPK"), pressKey(KeyEvent.KEYCODE_BACK))
        onView(allOf(isDescendantOfA(withHint(hasToString("Enter URL"))), withId(R.id.input)))
            .perform(typeText("TestURL"), pressKey(KeyEvent.KEYCODE_BACK))
        onView(allOf(isDescendantOfA(withHint(hasToString("Enter Username"))), withId(R.id.input)))
            .perform(typeText("TestUsername"), pressKey(KeyEvent.KEYCODE_BACK))
        onView(withId(R.id.generatePasswordLayout)).perform(click())
        onView(allOf(isDescendantOfA(withHint(hasToString("Enter Description"))), withId(R.id.input)))
            .perform(typeText("TestDescription"), pressKey(KeyEvent.KEYCODE_BACK))
        onView(withId(R.id.updateButton)).perform(scrollTo(), click())
    }
}
