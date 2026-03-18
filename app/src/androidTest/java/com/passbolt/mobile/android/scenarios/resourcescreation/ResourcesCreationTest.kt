/**
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

package com.passbolt.mobile.android.scenarios.resourcescreation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.espresso.IdlingRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.passbolt.mobile.android.core.idlingresource.CreateResourceIdlingResource
import com.passbolt.mobile.android.core.idlingresource.ResourcesFullRefreshIdlingResource
import com.passbolt.mobile.android.core.idlingresource.SignInIdlingResource
import com.passbolt.mobile.android.core.localization.R.string.filters_menu_all_items
import com.passbolt.mobile.android.core.localization.R.string.filters_menu_favourites
import com.passbolt.mobile.android.core.localization.R.string.filters_menu_folders
import com.passbolt.mobile.android.core.localization.R.string.filters_menu_groups
import com.passbolt.mobile.android.core.localization.R.string.filters_menu_owned_by_me
import com.passbolt.mobile.android.core.localization.R.string.filters_menu_recently_modified
import com.passbolt.mobile.android.core.localization.R.string.filters_menu_shared_with_me
import com.passbolt.mobile.android.core.localization.R.string.filters_menu_tags
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.feature.authentication.AuthenticationMainActivity
import com.passbolt.mobile.android.helpers.chooseFilter
import com.passbolt.mobile.android.helpers.getString
import com.passbolt.mobile.android.helpers.signIn
import com.passbolt.mobile.android.instrumentationTestsModule
import com.passbolt.mobile.android.intents.ManagedAccountIntentCreator
import com.passbolt.mobile.android.rules.IdlingResourceRule
import com.passbolt.mobile.android.rules.lazyActivitySetupScenarioRule
import com.passbolt.mobile.android.testtags.composetags.BackNavigation.ICON
import com.passbolt.mobile.android.testtags.composetags.Home
import com.passbolt.mobile.android.testtags.composetags.PasswordField
import com.passbolt.mobile.android.testtags.composetags.ResourceForm
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.component.inject
import org.koin.test.KoinTest
import com.passbolt.mobile.android.core.localization.R as LocalizationR

@RunWith(AndroidJUnit4::class)
@MediumTest
class ResourcesCreationTest : KoinTest {
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
            val createResourceIdlingResource: CreateResourceIdlingResource by inject()
            IdlingResourceRule(
                arrayOf(
                    signInIdlingResource,
                    resourcesFullRefreshIdlingResource,
                    createResourceIdlingResource,
                ),
            )
        }

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Before
    fun setup() {
        composeTestRule.apply {
            signIn(managedAccountIntentCreator.getPassphrase())
            chooseFilter(filters_menu_all_items)
        }
    }

    /**  [As a logged in mobile user on the password workspace I should see a create button](https://passbolt.testrail.io/index.php?/cases/view/6348)
     *
     *      Given   that I am a logged in mobile user
     *      When    I am on the password workspace
     *      And     I am on a filter that supports creation
     *      Then    I see a create button
     *      And     on filters that don't support creation I don't see a create button
     */
    @Test
    fun asALoggedInMobileUserOnThePasswordWorkspaceIShouldSeeACreateButton() {
        composeTestRule.apply {
            val filtersWithFab =
                listOf(
                    filters_menu_all_items,
                    filters_menu_favourites,
                    filters_menu_recently_modified,
                    filters_menu_shared_with_me,
                    filters_menu_owned_by_me,
                    filters_menu_folders,
                )
            filtersWithFab.forEach { filter ->
                chooseFilter(filter)
                onNodeWithTag(Home.FAB).assertExists()
            }

            val filtersWithoutFab =
                listOf(
                    filters_menu_tags,
                    filters_menu_groups,
                )
            filtersWithoutFab.forEach { filter ->
                chooseFilter(filter)
                onNodeWithTag(Home.FAB).assertDoesNotExist()
            }
        }
    }

    /**  [As a logged in mobile user on the password workspace I should see the new password page](https://passbolt.testrail.io/index.php?/cases/view/8128)
     *
     *      Given   I am a logged in mobile user
     *      And     I see the create button
     *      When    I click on the create button and choose Password
     *      Then    I see the New password page with all fields
     */
    @Test
    fun asALoggedInMobileUserOnThePasswordWorkspaceIShouldSeeTheNewPasswordPage() {
        composeTestRule.apply {
            onNodeWithTag(Home.FAB).performClick()
            onNodeWithText(getString(LocalizationR.string.create_resource_menu_create_password)).performClick()

            onNodeWithText(getString(LocalizationR.string.resource_form_create_password)).assertIsDisplayed()
            onNode(hasTestTag(ICON), useUnmergedTree = true).assertExists() // Back icon
            onNodeWithTag(ResourceForm.NAME_INPUT).assertIsDisplayed()
            onNodeWithTag(ResourceForm.URI_INPUT).assertIsDisplayed()
            onNodeWithTag(ResourceForm.USERNAME_INPUT).assertIsDisplayed()
            onNodeWithTag(ResourceForm.PASSWORD_INPUT).assertExists()
            onNodeWithTag(PasswordField.VISIBILITY_TOGGLE, useUnmergedTree = true).assertExists()
            onNodeWithTag(ResourceForm.SAVE_BUTTON).assertIsDisplayed()
        }
    }

    /**  [As a logged in mobile user on the new password page I should see a toast message after creation](https://passbolt.testrail.io/index.php?/cases/view/8130)
     *
     *      Given   I am on the "new password" page
     *      And     I filled out all mandatory fields
     *      When    I click on the create button
     *      Then    I am redirected to the password workspace
     */
    @Test
    fun asALoggedInMobileUserOnTheNewPasswordPageIShouldSeeAToastMessageAfterCreationAPassword() {
        // unregister refresh idling resource after first refresh not to block the snackbar checks
        // (second refresh is during snackbar is showing)
        IdlingRegistry.getInstance().unregister(resourcesFullRefreshIdlingResource)
        composeTestRule.apply {
            onNodeWithTag(Home.FAB).performClick()
            onNodeWithText(getString(LocalizationR.string.create_resource_menu_create_password)).performClick()

            onNodeWithTag(ResourceForm.NAME_INPUT).performTextReplacement("PasswordNameTest")
            onNodeWithTag(ResourceForm.PASSWORD_INPUT).performTextReplacement("TestPassword123!")
            onNodeWithTag(ResourceForm.SAVE_BUTTON).performClick()
            onNodeWithTag(Home.SCREEN).assertIsDisplayed()
        }
    }

    /**  [As a logged in mobile user on the new password page I can generate a random password](https://passbolt.testrail.io/index.php?/cases/view/8132)
     *
     *      Given   I am a logged in mobile user on the new password page
     *      When    I click on the "Generate" button
     *      Then    I see the password field is automatically filled in
     */
    @Test
    fun asALoggedInMobileUserOnTheNewPasswordPageICanGenerateARandomPassword() {
        composeTestRule.apply {
            onNodeWithTag(Home.FAB).performClick()
            onNodeWithText(getString(LocalizationR.string.create_resource_menu_create_password)).performClick()

            onNodeWithTag(ResourceForm.GENERATE_PASSWORD_BUTTON).performClick()
            // after generation the password field should exist and be filled
            onNodeWithTag(ResourceForm.PASSWORD_INPUT).assertExists()
        }
    }

    /**  [As a logged in mobile user on the new password page I can switch the visibility of the password](https://passbolt.testrail.io/index.php?/cases/view/8133)
     *
     *      Given   I am a logged in mobile user on the new password page
     *      And     the password field is not empty
     *      When    I click on the "show/hide" button
     *      Then    I can see the password in plain text
     */
    @Test
    fun asALoggedInMobileUserOnTheNewPasswordPageICanSwitchTheVisibilityOfThePassword() {
        composeTestRule.apply {
            onNodeWithTag(Home.FAB).performClick()
            onNodeWithText(getString(LocalizationR.string.create_resource_menu_create_password)).performClick()

            onNodeWithTag(ResourceForm.PASSWORD_INPUT).performTextReplacement("TestPassword")
            onNodeWithTag(PasswordField.VISIBILITY_TOGGLE, useUnmergedTree = true).performClick()
            onNodeWithTag(ResourceForm.PASSWORD_INPUT).assertTextContains("TestPassword")
        }
    }
}
