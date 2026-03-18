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

package com.passbolt.mobile.android.scenarios.home.filters.folders.list

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.passbolt.mobile.android.core.idlingresource.CreateFolderIdlingResource
import com.passbolt.mobile.android.core.idlingresource.ResourcesFullRefreshIdlingResource
import com.passbolt.mobile.android.core.idlingresource.SignInIdlingResource
import com.passbolt.mobile.android.core.localization.R.string.filters_menu_folders
import com.passbolt.mobile.android.core.localization.R.string.no_passwords
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.core.ui.scaffold.HomeScaffoldTestTags.APP_BAR_ICON
import com.passbolt.mobile.android.core.ui.topbar.BackNavigationIcon.TestTags.ICON
import com.passbolt.mobile.android.feature.authentication.AuthenticationMainActivity
import com.passbolt.mobile.android.helpers.chooseFilter
import com.passbolt.mobile.android.helpers.getString
import com.passbolt.mobile.android.helpers.searchAndOpenFirstFolderByName
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

@RunWith(AndroidJUnit4::class)
@MediumTest
class FoldersListTest : KoinTest {
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
        composeTestRule.chooseFilter(filters_menu_folders)
    }

    /**  [On the folder workspace I can open folder](https://passbolt.testrail.io/index.php?/cases/view/8166)
     *
     *      Given I am on the folders filter view
     *      When I click a folder
     *      Then I see a back arrow to go back to the previous page
     *      And I see folder icon
     *      And I see folder name
     *      And I see ‘3 dots’
     *      And I see filters icon
     *      And I see search bar
     *      And I see user’s current avatar
     *      And I see ‘There are no passwords’ description with picture
     *      And I see create button in @blue
     */
    @Test
    fun onTheFolderWorkspaceICanOpenFolder() {
        composeTestRule.apply {
            searchAndOpenFirstFolderByName(NEW_TEST_FOLDER_NAME)
            onNodeWithText(NEW_TEST_FOLDER_NAME).assertIsDisplayed()
            onNode(hasTestTag(ICON), useUnmergedTree = true).assertExists() // Back icon
            onNode(hasTestTag(APP_BAR_ICON), useUnmergedTree = true).assertExists() // Folder icon
            onNodeWithTag("home_search_input_field").assertExists()
            onNodeWithTag("home_search_filter").assertExists()
            onNodeWithText(getString(no_passwords)).assertIsDisplayed()
            onNodeWithTag("home_fab").assertExists()
        }
    }

    private companion object {
        private const val NEW_TEST_FOLDER_NAME = "Automated Android folder"
    }
}
