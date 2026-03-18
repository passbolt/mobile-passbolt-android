/*
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021-2026 Passbolt SA
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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.passbolt.mobile.android.core.idlingresource.CreateFolderIdlingResource
import com.passbolt.mobile.android.core.idlingresource.ResourcesFullRefreshIdlingResource
import com.passbolt.mobile.android.core.idlingresource.SignInIdlingResource
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.core.ui.topbar.BackNavigationIcon.TestTags.ICON
import com.passbolt.mobile.android.feature.authentication.AuthenticationMainActivity
import com.passbolt.mobile.android.helpers.chooseFilter
import com.passbolt.mobile.android.helpers.getString
import com.passbolt.mobile.android.helpers.signIn
import com.passbolt.mobile.android.instrumentationTestsModule
import com.passbolt.mobile.android.intents.ManagedAccountIntentCreator
import com.passbolt.mobile.android.rules.IdlingResourceRule
import com.passbolt.mobile.android.rules.lazyActivitySetupScenarioRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.component.inject
import org.koin.test.KoinTest
import com.passbolt.mobile.android.core.localization.R as LocalisationR

@RunWith(AndroidJUnit4::class)
@MediumTest
class FolderCreationTest : KoinTest {
    @get:Rule(order = 0)
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

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Before
    fun setup() {
        composeTestRule.signIn(managedAccountIntentCreator.getPassphrase())
        composeTestRule.chooseFilter(LocalisationR.string.filters_menu_folders)
    }

    /**  [On the folders workspace I can click ‘Create Button’ when v5 resources are enabled and default](https://passbolt.testrail.io/index.php?/cases/view/17611)
     *
     *      Given   The <resource type> is allowed
     *      When    I click on “Create a resource” button
     *      Then    I see a drawer named “Create a resource” with a list
     *      And     I see ‘X’ close button
     *      And I see a <resource icon> list item
     *      And I see a <resource name> list item
     *
     *      | resource type | resource icon | resource name |
     *      |---------------|---------------|---------------|
     *      | password v5   | key         | Password      |
     *      | TOTP  v5     | lock        | TOTP          |
     *      | Note   | notebook        | Note          |
     *      | folders v5    | folder        | Folder          |
     *
     */
    @Test
    fun onTheFoldersWorkspaceICanClickCreateButtonWhenV5ResourcesAreEnabledAndDefault() {
        composeTestRule.apply {
            onNodeWithTag("home_fab").performClick()
            onNodeWithText(getString(LocalisationR.string.create_resource_menu_create_a_resource))
                .assertIsDisplayed()
            onNodeWithTag("bottom_sheet_icon_close").assertIsDisplayed()
            onNodeWithText(getString(LocalisationR.string.create_resource_menu_create_password))
                .assertIsDisplayed()
            // "TOTP" text appears both in the bottom nav and in the create menu
            onAllNodesWithText(getString(LocalisationR.string.create_resource_menu_create_totp))[0]
                .assertIsDisplayed()
            onNodeWithText(getString(LocalisationR.string.create_resource_menu_create_note))
                .assertIsDisplayed()
            onNodeWithText(getString(LocalisationR.string.create_resource_menu_create_folder))
                .assertIsDisplayed()
        }
    }

    /**  [On the folder workspace I can cancel creation process](https://passbolt.testrail.io/index.php?/cases/view/8162)
     *
     *      Given  I clicked on the create button
     *      When    I click ‘X’ button
     *      Then    I am back on folders workspace
     */
    @Test
    fun onTheFolderWorkspaceICanCancelCreationProcess() {
        composeTestRule.apply {
            onNodeWithTag("home_fab").performClick()
            onNodeWithTag("bottom_sheet_icon_close").performClick()
            onNodeWithTag("home_screen").assertIsDisplayed()
        }
    }

    /**  [I can enter `Create folder` screen](https://passbolt.testrail.io/index.php?/cases/view/8163)
     *
     *      Given  I clicked on the create button
     *      When   I click ‘Folder’ button
     *      Then   I see ‘Create folder’ workspace
     *      And I see a back arrow to go back to the previous page
     *      And I see a mandatory input text field with a ‘Name’ label
     *      And I see ‘Location’ label with ‘Root’ information
     *      And I see an icon of a user who creates a folder
     *      And I see ‘Save’ button in @blue
     */
    @Test
    fun iCanEnterCreateFolderScreen() {
        composeTestRule.apply {
            onNodeWithTag("home_fab").performClick()
            onNodeWithText(getString(LocalisationR.string.create_resource_menu_create_folder)).performClick()
            onNodeWithText(getString(LocalisationR.string.create_folder_title)).assertIsDisplayed()
            onNode(hasTestTag(ICON), useUnmergedTree = true).assertExists()
            onNode(
                hasText(getString(LocalisationR.string.enter_folder_name), substring = true, ignoreCase = true),
                useUnmergedTree = true,
            ).assertExists()
            onNode(
                hasText(getString(LocalisationR.string.location), substring = true, ignoreCase = true),
                useUnmergedTree = true,
            ).assertExists()
            onNode(
                hasText(getString(LocalisationR.string.folder_root), substring = true, ignoreCase = true),
                useUnmergedTree = true,
            ).assertExists()
            onNode(
                hasText(getString(LocalisationR.string.shared_with), substring = true, ignoreCase = true),
                useUnmergedTree = true,
            ).assertExists()
            onNodeWithText(getString(LocalisationR.string.save)).assertExists()
        }
    }

    /**  [On the root folder workspace I will see an error when saving folder without its name](https://passbolt.testrail.io/index.php?/cases/view/8164)
     *
     *      Given I clicked on the create button
     *      And I clicked on the Folder button
     *      When I click ‘Save’ button
     *      Then I see the label of the ‘Name’ field in @red
     *      And I see the "Name" field have a @red border
     *      And I see exclamation mark in @red
     *      And I see error ‘Length should be between 1 and 256’ below the field in @red
     */
    @Test
    fun onTheRootFolderWorkspaceIWillSeeAnErrorWhenSavingFolderWithoutItsName() {
        val expectedError =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext
                .getString(LocalisationR.string.validation_required_with_max_length, 256)
        composeTestRule.apply {
            onNodeWithTag("home_fab").performClick()
            onNodeWithText(getString(LocalisationR.string.create_resource_menu_create_folder)).performClick()
            onNodeWithText(getString(LocalisationR.string.save)).performClick()
            onNodeWithText(expectedError).assertIsDisplayed()
        }
    }

    /**  [On the root folder workspace I can save new folder](https://passbolt.testrail.io/index.php?/cases/view/8165)
     *
     *      Given I clicked on the create button
     *      And I clicked on the create folder button
     *      And I filled out mandatory ‘Name’ field
     *      When I click ‘Save’ button
     *      Then I see ‘New folder {folder’s name} has been created!’ in @green
     *      And I am redirected to the folders workspace
     */
    @Test
    fun onTheRootFolderWorkspaceICanSaveNewFolder() {
        composeTestRule.apply {
            waitUntil(timeoutMillis = 4_000) {
                onAllNodesWithTag("home_fab").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithTag("home_fab").performClick()
            onNodeWithText(getString(LocalisationR.string.create_resource_menu_create_folder)).performClick()
            onNodeWithTag("create_folder_name_input", useUnmergedTree = true)
                .performClick()
                .performTextReplacement(NEW_TEST_FOLDER_NAME)
            onNodeWithText(getString(LocalisationR.string.save)).performClick()
            onNodeWithTag("home_screen").assertIsDisplayed()
        }
    }

    private companion object {
        private const val NEW_TEST_FOLDER_NAME = "Automated Android folder"
    }
}
