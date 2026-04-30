/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2023-2026 Passbolt SA
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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
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
import com.passbolt.mobile.android.core.idlingresource.UpdateResourceIdlingResource
import com.passbolt.mobile.android.core.localization.R.string.filters_menu_all_items
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.feature.authentication.AuthenticationMainActivity
import com.passbolt.mobile.android.helpers.chooseFilter
import com.passbolt.mobile.android.helpers.createNewPasswordFromHomeScreen
import com.passbolt.mobile.android.helpers.getString
import com.passbolt.mobile.android.helpers.searchAndClickMoreOfFirstResource
import com.passbolt.mobile.android.helpers.signIn
import com.passbolt.mobile.android.helpers.waitForResourceForm
import com.passbolt.mobile.android.instrumentationTestsModule
import com.passbolt.mobile.android.intents.ManagedAccountIntentCreator
import com.passbolt.mobile.android.rules.IdlingResourceRule
import com.passbolt.mobile.android.rules.lazyActivitySetupScenarioRule
import com.passbolt.mobile.android.testtags.composetags.Home
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
class ResourcesEditionTest : KoinTest {
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
            val updateResourceIdlingResource: UpdateResourceIdlingResource by inject()
            val createResourceIdlingResource: CreateResourceIdlingResource by inject()
            IdlingResourceRule(
                arrayOf(
                    signInIdlingResource,
                    resourcesFullRefreshIdlingResource,
                    updateResourceIdlingResource,
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
            createNewPasswordFromHomeScreen(RESOURCE_NAME)
        }
    }

    /**  [On the resources action menu drawer I can click edit password](https://passbolt.testrail.io/index.php?/cases/view/8134)
     *
     *      Given   that I am on the action menu drawer
     *      And     I see 'Edit password' element enabled
     *      When    I click 'Edit password'
     *      Then    I am on the Edit password screen
     */
    @Test
    fun onTheResourcesActionMenuDrawerICanClickEditPassword() {
        composeTestRule.apply {
            searchAndClickMoreOfFirstResource(RESOURCE_NAME)
            onNodeWithText(getString(LocalizationR.string.more_edit)).assertIsDisplayed()
            onNodeWithText(getString(LocalizationR.string.more_edit)).performClick()
            onNodeWithText(
                InstrumentationRegistry
                    .getInstrumentation()
                    .targetContext
                    .getString(LocalizationR.string.resource_form_edit_resource, RESOURCE_NAME),
            ).assertIsDisplayed()
        }
    }

    /**  [On the edit password page I can edit elements](https://passbolt.testrail.io/index.php?/cases/view/8135)
     *
     *      Given   that I am on the Edit password screen
     *      And     fields are filled
     *      When    I change the fields
     *      Then    the fields are changed
     */
    @Test
    fun onTheEditPasswordPageICanEditElements() {
        enterEditPasswordScreen()
        composeTestRule.apply {
            onNodeWithTag(ResourceForm.NAME_INPUT).assertIsDisplayed()
            onNodeWithTag(ResourceForm.URI_INPUT).assertIsDisplayed()
            onNodeWithTag(ResourceForm.USERNAME_INPUT).assertIsDisplayed()

            onNodeWithTag(ResourceForm.NAME_INPUT).performTextReplacement("EditedName")
            onNodeWithTag(ResourceForm.URI_INPUT).performTextReplacement("EditedURL")
            onNodeWithTag(ResourceForm.USERNAME_INPUT).performTextReplacement("EditedUsername")

            onNodeWithTag(ResourceForm.NAME_INPUT).assertTextContains("EditedName")
            onNodeWithTag(ResourceForm.URI_INPUT).assertTextContains("EditedURL")
            onNodeWithTag(ResourceForm.USERNAME_INPUT).assertTextContains("EditedUsername")
        }
    }

    /**  [On the edit password page I can save changed resources](https://passbolt.testrail.io/index.php?/cases/view/8136)
     *
     *      Given   that I am on the Edit password screen
     *      And     I changed a field
     *      When    I click Save button
     *      Then    I am back on the home screen
     */
    @Test
    fun onTheEditPasswordPageICanSaveChangedResources() {
        enterEditPasswordScreen()
        composeTestRule.apply {
            onNodeWithTag(ResourceForm.NAME_INPUT).performTextReplacement("EditedName")
            onNodeWithTag(ResourceForm.SAVE_BUTTON).performClick()
            onNodeWithTag(Home.SCREEN).assertIsDisplayed()
        }
    }

    /**  [On the edit password page I can delete the optional input text field](https://passbolt.testrail.io/index.php?/cases/view/8138)
     *
     *      Given   that I am on the Edit password screen
     *      And     all placeholders are filled
     *      When    I delete optional text fields
     *      Then    the fields are empty
     */
    @Test
    fun onTheEditPasswordPageICanDeleteTheOptionalInputTextField() {
        enterEditPasswordScreen()
        composeTestRule.apply {
            onNodeWithTag(ResourceForm.URI_INPUT).performTextReplacement("")
            onNodeWithTag(ResourceForm.USERNAME_INPUT).performTextReplacement("")

            onNodeWithTag(ResourceForm.URI_INPUT).assertTextContains("")
            onNodeWithTag(ResourceForm.USERNAME_INPUT).assertTextContains("")
        }
    }

    /**  [On the edit password page I see confirmation popup when saving a changed resource](https://passbolt.testrail.io/index.php?/cases/view/8139)
     *
     *      Given   that I am on the Edit password screen
     *      And     I edited the resource
     *      When    I click Save button
     *      Then    I am back on the home screen
     */
    @Test
    fun onTheEditPasswordPageISeeConfirmationPopupWhenSavingAChangedResource() {
        // unregister refresh idling resource after first refresh not to block the snackbar checks
        // (second refresh is during snackbar is showing)
        IdlingRegistry.getInstance().unregister(resourcesFullRefreshIdlingResource)
        enterEditPasswordScreen()
        composeTestRule.apply {
            onNodeWithTag(ResourceForm.NAME_INPUT).performTextReplacement("EditedName")
            onNodeWithTag(ResourceForm.URI_INPUT).performTextReplacement("")
            onNodeWithTag(ResourceForm.USERNAME_INPUT).performTextReplacement("")
            onNodeWithTag(ResourceForm.SAVE_BUTTON).performClick()
            onNodeWithTag(Home.SCREEN).assertIsDisplayed()
        }
    }

    private fun enterEditPasswordScreen() {
        composeTestRule.apply {
            searchAndClickMoreOfFirstResource(RESOURCE_NAME)
            onNodeWithText(getString(LocalizationR.string.more_edit)).performClick()
            waitForResourceForm()
        }
    }

    private companion object {
        private const val RESOURCE_NAME = "ResourcesEditionTest"
    }
}
