/*
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

package com.passbolt.mobile.android.scenarios.home.filters.folders.creation

import android.view.KeyEvent
import androidx.appcompat.widget.Toolbar
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.pressKey
import androidx.test.espresso.action.ViewActions.replaceText
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
import com.leinardi.android.speeddial.SpeedDialView
import com.passbolt.mobile.android.commontest.viewassertions.CastedViewAssertion
import com.passbolt.mobile.android.core.idlingresource.CreateFolderIdlingResource
import com.passbolt.mobile.android.core.idlingresource.ResourcesFullRefreshIdlingResource
import com.passbolt.mobile.android.core.idlingresource.SignInIdlingResource
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.feature.authentication.AuthenticationMainActivity
import com.passbolt.mobile.android.feature.setup.R
import com.passbolt.mobile.android.helpers.getString
import com.passbolt.mobile.android.helpers.signIn
import com.passbolt.mobile.android.instrumentationTestsModule
import com.passbolt.mobile.android.intents.ManagedAccountIntentCreator
import com.passbolt.mobile.android.matchers.hasDrawable
import com.passbolt.mobile.android.matchers.withIndex
import com.passbolt.mobile.android.matchers.withSpeedDialViewOpenState
import com.passbolt.mobile.android.matchers.withTextInputStrokeColorOf
import com.passbolt.mobile.android.rules.IdlingResourceRule
import com.passbolt.mobile.android.rules.lazyActivitySetupScenarioRule
import org.hamcrest.Matchers.allOf
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
class FolderCreationTest : KoinTest {
    @get:Rule
    val startUpActivityRule =
        lazyActivitySetupScenarioRule<AuthenticationMainActivity>(
            koinOverrideModules = listOf(instrumentationTestsModule),
            intentSupplier = {
                ActivityIntents.authentication(
                    InstrumentationRegistry.getInstrumentation().targetContext,
                    ActivityIntents.AuthConfig.Startup,
                    AppContext.APP,
                    managedAccountIntentCreator.getUserLocalId(),
                )
            },
        )

    private val managedAccountIntentCreator: ManagedAccountIntentCreator by inject()

    private val resourcesFullRefreshIdlingResource: ResourcesFullRefreshIdlingResource by inject()

    @get:Rule
    val idlingResourceRule =
        let {
            val signInIdlingResource: SignInIdlingResource by inject()
            val createFolderIdlingResource: CreateFolderIdlingResource by inject()
            IdlingResourceRule(
                arrayOf(
                    signInIdlingResource,
                    createFolderIdlingResource,
                    resourcesFullRefreshIdlingResource,
                ),
            )
        }

    @BeforeTest
    fun setup() {
        //    Background context
        //    #PRO_FOLDER_CREATION_WITH_PERMISSION ●
        //    Given I already have account admin_automated
        //    And I am logged in mobile app
        //    And I am on Passbolt PRO
        //    And I want to create new folder
        //    And I am on the folders filter view
        //    And I have the permission to create a folder in my current location
        signIn(managedAccountIntentCreator.getPassphrase())
        onView(withId(MaterialR.id.text_input_start_icon)).perform(click())
        //    Given     that I am on the folders workspace
        onView(withId(com.passbolt.mobile.android.feature.home.R.id.folders)).perform(click())
    }

    //    https://passbolt.testrail.io/index.php?/cases/view/8160
    @Test
    fun onTheFoldersWorkspaceICanClickCreateButton() {
        //    Given     that I am on #PRO_FOLDER_CREATION_WITH_PERMISSION
        //    And       I see a create button with an icon in @blue
        onView(withId(com.passbolt.mobile.android.feature.home.R.id.homeSpeedDialViewId))
            .check(matches(isDisplayed()))
            .check(matches(hasDrawable(id = CoreUiR.drawable.ic_plus, tint = CoreUiR.color.icon_tint)))
        //    When      I click create button
        onView(withId(com.passbolt.mobile.android.feature.home.R.id.homeSpeedDialViewId)).perform(click())
        //    Then      I see ‘Add folder’ with folder icon
        onView(
            allOf(
                isDescendantOfA(withId(com.passbolt.mobile.android.feature.home.R.id.homeSpeedDialViewAddFolderId)),
                withId(
                    com.leinardi.android.speeddial.R.id.sd_fab,
                ),
            ),
        ).check(matches(isDisplayed()))
            .check(matches(hasDrawable(id = CoreUiR.drawable.ic_folder, tint = CoreUiR.color.icon_tint)))
        onView(
            allOf(
                isDescendantOfA(withId(com.passbolt.mobile.android.feature.home.R.id.homeSpeedDialViewAddFolderId)),
                withId(
                    com.leinardi.android.speeddial.R.id.sd_label,
                ),
            ),
        ).check(matches(isDisplayed()))
            .check(matches(withText(LocalizationR.string.home_speed_dial_add_folder)))
        //    And       I see ‘Add password’ with key icon
        onView(
            allOf(
                isDescendantOfA(withId(com.passbolt.mobile.android.feature.home.R.id.homeSpeedDialViewAddPasswordId)),
                withId(
                    com.leinardi.android.speeddial.R.id.sd_fab,
                ),
            ),
        ).check(matches(isDisplayed()))
            .check(matches(hasDrawable(id = CoreUiR.drawable.ic_key, tint = CoreUiR.color.icon_tint)))
        onView(
            allOf(
                isDescendantOfA(withId(com.passbolt.mobile.android.feature.home.R.id.homeSpeedDialViewAddPasswordId)),
                withId(
                    com.leinardi.android.speeddial.R.id.sd_label,
                ),
            ),
        ).check(matches(isDisplayed()))
            .check(matches(withText(LocalizationR.string.home_speed_dial_add_password)))
        //    And       And I see ‘X’ close button (Note: this is actually the "plus" icon but rotated 45 degrees)
        onView(withId(com.passbolt.mobile.android.feature.home.R.id.homeSpeedDialViewId))
            .check(matches(isDisplayed()))
            .check(matches(hasDrawable(id = CoreUiR.drawable.ic_plus, tint = CoreUiR.color.icon_tint)))
    }

    //    https://passbolt.testrail.io/index.php?/cases/view/8161
    @Test
    fun onTheFoldersWorkspaceICanClickAddPasswordAndOpenNewPasswordWorkspace() {
        //    Given     that I am on #PRO_FOLDER_CREATION_WITH_PERMISSION
        onView(withId(com.passbolt.mobile.android.feature.home.R.id.homeSpeedDialViewId))
            .check(matches(isDisplayed()))
            .check(matches(hasDrawable(id = CoreUiR.drawable.ic_plus, tint = CoreUiR.color.icon_tint)))
        //    When      I click create button
        onView(withId(com.passbolt.mobile.android.feature.home.R.id.homeSpeedDialViewId)).perform(click())
        //    When      When I click ‘Add password’
        onView(withIndex(1, withId(com.leinardi.android.speeddial.R.id.sd_fab))).perform(click())
        //    Then      Then I see ‘New password’ workspace
        onView(
            allOf(
                isDescendantOfA(withId(R.id.toolbar)),
                withText(LocalizationR.string.resource_update_password_title),
            ),
        ).check(matches(isDisplayed()))
    }

    //    https://passbolt.testrail.io/index.php?/cases/view/8162
    @Test
    fun onTheFolderWorkspaceICanCancelCreationProcess() {
        //    Given     that I am on #PRO_FOLDER_CREATION_WITH_PERMISSION
        //    And       I see a create button with an icon in @blue
        onView(withId(com.passbolt.mobile.android.feature.home.R.id.homeSpeedDialViewId))
            .check(matches(isDisplayed()))
            .check(matches(hasDrawable(id = CoreUiR.drawable.ic_plus, tint = CoreUiR.color.icon_tint)))
        //    When      I click create button
        onView(withId(com.passbolt.mobile.android.feature.home.R.id.homeSpeedDialViewId)).perform(click())
        //    When      I click ‘X’ button
        //    Then      I am on folders workspace
        onView(
            allOf(
                withId(com.passbolt.mobile.android.feature.home.R.id.homeSpeedDialViewId),
                isAssignableFrom(SpeedDialView::class.java),
            ),
        ).check(matches(withSpeedDialViewOpenState(isOpen = true)))
    }

    //    https://passbolt.testrail.io/index.php?/cases/view/8163
    @Test
    fun onTheFolderWorkspaceICanClickAddFolderAndOpenCreateFolderWorkspace() {
        //    Given     that I am on #PRO_FOLDER_CREATION_WITH_PERMISSION
        onView(withId(com.passbolt.mobile.android.feature.home.R.id.homeSpeedDialViewId))
            .check(matches(isDisplayed()))
            .check(matches(hasDrawable(id = CoreUiR.drawable.ic_plus, tint = CoreUiR.color.icon_tint)))
        //    When      I click create button
        onView(withId(com.passbolt.mobile.android.feature.home.R.id.homeSpeedDialViewId)).perform(click())
        //    When      I click ‘Add folder’ button
        onView(withIndex(0, withId(com.leinardi.android.speeddial.R.id.sd_label_container))).perform(click())
        //    Then      I see ‘Create folder’ workspace
        onView(
            allOf(
                isDescendantOfA(withId(R.id.toolbar)),
                withText(LocalizationR.string.create_folder_title),
            ),
        ).check(matches(isDisplayed()))
        //    And       I see a back arrow to go back to the previous page
        onView(isAssignableFrom(Toolbar::class.java)).check(CastedViewAssertion<Toolbar> { it.navigationIcon != null })
        //    And       I see a mandatory input text field with a ‘Name’ label
        onView(withText("Name *")).check(matches(isDisplayed()))
        //    And       I see ‘Location’ label with ‘Root’ information
        onView(withText("Location")).check(matches(isDisplayed()))
        onView(withText("Root")).check(matches(isDisplayed()))
        //    And       I see user's avatar under ‘Share with’ label
        onView(withText("Shared with")).check(matches(isDisplayed()))
        //    And       I see ‘Save’ button in @blue
        onView(withText(LocalizationR.string.save)).check(matches(isDisplayed()))
    }

    //    https://passbolt.testrail.io/index.php?/cases/view/8164
    @Test
    fun onTheRootFolderWorkspaceIWillSeeAnErrorWhenSavingFolderWithoutItsName() {
        //    Given     that I am on #PRO_FOLDER_CREATION_WITH_PERMISSION
        onView(withId(com.passbolt.mobile.android.feature.home.R.id.homeSpeedDialViewId))
            .check(matches(isDisplayed()))
            .check(matches(hasDrawable(id = CoreUiR.drawable.ic_plus, tint = CoreUiR.color.icon_tint)))
        //    When      I click create button
        onView(withId(com.passbolt.mobile.android.feature.home.R.id.homeSpeedDialViewId)).perform(click())
        //    When      I click ‘Add folder’ button
        onView(withIndex(0, withId(com.leinardi.android.speeddial.R.id.sd_label_container))).perform(click())
        //    Given     that I am on ‘Create folder’ workspace
        //    When      I click ‘Save’ button
        onView(withText(LocalizationR.string.save)).perform(click())
        //    Then      Then I see the label of the ‘Name’ field in @red
        onView(withText("Name *")).check(matches(hasTextColor(CoreUiR.color.red)))
        //    And       I see stroke of the ‘Name’ field in @red
        onView(withId(CoreUiR.id.textLayout))
            .check(matches(withTextInputStrokeColorOf(CoreUiR.color.red)))
        //    And       I see exclamation mark in @red
        onView(withId(MaterialR.id.text_input_error_icon)).check(matches(isDisplayed()))
        //    And       I see error ‘Length should be between 1 and 256’ below the field in @red
        onView(withText("Length should be between 1 and 256"))
            .check(matches(isDisplayed()))
            .check(matches(hasTextColor(CoreUiR.color.red)))
    }

    //    https://passbolt.testrail.io/index.php?/cases/view/8165
    @Test
    fun onTheRootFolderWorkspaceICanSaveNewFolder() {
        // unregister refresh idling resource after first refresh not to block the snackbar checks
        // (second refresh is during snackbar is showing)
        IdlingRegistry.getInstance().unregister(resourcesFullRefreshIdlingResource)

        //    Given     that I am on #PRO_FOLDER_CREATION_WITH_PERMISSION
        onView(withId(com.passbolt.mobile.android.feature.home.R.id.homeSpeedDialViewId))
            .check(matches(isDisplayed()))
            .check(matches(hasDrawable(id = CoreUiR.drawable.ic_plus, tint = CoreUiR.color.icon_tint)))
        //    When      I click create button
        onView(withId(com.passbolt.mobile.android.feature.home.R.id.homeSpeedDialViewId)).perform(click())
        //    When      I click ‘Add folder’ button
        onView(withIndex(0, withId(com.leinardi.android.speeddial.R.id.sd_label_container))).perform(click())
        //    Given     that I am on ‘Create folder’ workspace
        //    And       And I filled out mandatory ‘Name’ field
        onView(withId(CoreUiR.id.input)).perform(replaceText(NEW_TEST_FOLDER_NAME))
        //    When      I click ‘Save’ button
        onView(withText(LocalizationR.string.save)).perform(click())
        //    Then      I see ‘New folder {folder’s name} has been created!’ in @green
        //    And       I am redirected to the folders workspace
        onView(withId(com.passbolt.mobile.android.feature.permissions.R.id.rootLayout)).check(matches(isDisplayed()))
        onView(withId(MaterialR.id.snackbar_text))
            .check(
                matches(
                    withText(
                        getString(
                            LocalizationR.string.common_message_folder_created,
                            NEW_TEST_FOLDER_NAME,
                        ),
                    ),
                ),
            )
    }

    //    https://passbolt.testrail.io/index.php?/cases/view/8166
    @Test
    fun onTheFolderWorkspaceICanOpenFolder() {
        createFolderForOpening()

        //    Given     that I am on #PRO_FOLDER_CREATION_WITH_PERMISSION
        //    And       And I have at least the "can update" permission in the current context
        onView(withId(com.passbolt.mobile.android.feature.otp.R.id.searchEditText)).perform(
            replaceText(NEW_TEST_FOLDER_NAME),
            pressKey(KeyEvent.KEYCODE_ENTER),
        )
        //    When      I can click a folder which I created before
        onView(withIndex(0, withId(com.passbolt.mobile.android.feature.home.R.id.itemFolder))).perform(click())
        //    Then      I see a back arrow to go back to the previous page
        onView(withId(com.passbolt.mobile.android.feature.autofill.R.id.backButton)).check(matches(isDisplayed()))
        //    And       I see folder icon
        onView(withId(com.passbolt.mobile.android.feature.home.R.id.titleDrawable)).check(matches(isDisplayed()))
        //    And       I see folder name
        onView(withText(NEW_TEST_FOLDER_NAME)).check(matches(isDisplayed()))
        //    And       I see ‘3 dots’
        onView(withId(com.passbolt.mobile.android.feature.home.R.id.moreButton)).check(matches(isDisplayed()))
        //    And       I see filters icon
        onView(withId(MaterialR.id.text_input_start_icon)).check(matches(isDisplayed()))
        //    And       I see search bar
        onView(withId(com.passbolt.mobile.android.feature.otp.R.id.searchEditText)).check(matches(isDisplayed()))
        //    And       I see user’s current avatar
        onView(withId(MaterialR.id.text_input_end_icon)).check(matches(isDisplayed()))
        //    And       I see ‘There are no passwords’ description with picture
        onView(withText(LocalizationR.string.no_passwords)).check(matches(isDisplayed()))
        //    And       I see create button in @blue
        onView(withId(com.passbolt.mobile.android.feature.home.R.id.homeSpeedDialViewId))
            .check(matches(isDisplayed()))
            .check(matches(hasDrawable(id = CoreUiR.drawable.ic_plus, tint = CoreUiR.color.icon_tint)))
    }

    private fun createFolderForOpening() {
        onView(withId(com.passbolt.mobile.android.feature.home.R.id.homeSpeedDialViewId)).perform(click())
        onView(withIndex(0, withId(com.leinardi.android.speeddial.R.id.sd_label_container))).perform(click())
        onView(withId(CoreUiR.id.input)).perform(replaceText(NEW_TEST_FOLDER_NAME))
        onView(withText(LocalizationR.string.save)).perform(click())
    }

    private companion object {
        private const val NEW_TEST_FOLDER_NAME = "Automated tests folder"
    }
}
