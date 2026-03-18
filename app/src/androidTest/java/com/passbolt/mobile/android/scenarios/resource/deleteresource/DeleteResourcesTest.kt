/*
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

package com.passbolt.mobile.android.scenarios.resource.deleteresource

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.IdlingRegistry
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.passbolt.mobile.android.core.idlingresource.CreateMenuModelIdlingResource
import com.passbolt.mobile.android.core.idlingresource.CreateResourceIdlingResource
import com.passbolt.mobile.android.core.idlingresource.DeleteResourceIdlingResource
import com.passbolt.mobile.android.core.idlingresource.ResourcesFullRefreshIdlingResource
import com.passbolt.mobile.android.core.idlingresource.SignInIdlingResource
import com.passbolt.mobile.android.core.localization.R.string.delete
import com.passbolt.mobile.android.core.localization.R.string.filters_menu_all_items
import com.passbolt.mobile.android.core.localization.R.string.more_delete
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.feature.authentication.AuthenticationMainActivity
import com.passbolt.mobile.android.helpers.chooseFilter
import com.passbolt.mobile.android.helpers.createNewPasswordFromHomeScreen
import com.passbolt.mobile.android.helpers.getString
import com.passbolt.mobile.android.helpers.searchAndClickMoreOfFirstResource
import com.passbolt.mobile.android.helpers.signIn
import com.passbolt.mobile.android.helpers.waitForHomeScreen
import com.passbolt.mobile.android.instrumentationTestsModule
import com.passbolt.mobile.android.intents.ManagedAccountIntentCreator
import com.passbolt.mobile.android.rules.IdlingResourceRule
import com.passbolt.mobile.android.rules.lazyActivitySetupScenarioRule
import com.passbolt.mobile.android.testtags.composetags.Home
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.koin.test.KoinTest
import org.koin.test.inject
import com.passbolt.mobile.android.core.localization.R as LocalizationR

@RunWith(Parameterized::class)
@MediumTest
class DeleteResourcesTest(
    private val testedResource: String,
) : KoinTest {
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
            val deleteIdlingResource: DeleteResourceIdlingResource by inject()
            val createResourceIdlingResource: CreateResourceIdlingResource by inject()
            val createMenuModelIdlingResource: CreateMenuModelIdlingResource by inject()
            IdlingResourceRule(
                arrayOf(
                    signInIdlingResource,
                    resourcesFullRefreshIdlingResource,
                    deleteIdlingResource,
                    createResourceIdlingResource,
                    createMenuModelIdlingResource,
                ),
            )
        }

    private companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Resource name: {0}")
        fun resourceNames() =
            listOf(
                "To be deleted - Password with description",
            )
    }

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Before
    fun setup() {
        composeTestRule.signIn(managedAccountIntentCreator.getPassphrase())
    }

    /**  [On the password removal popup, I can click the delete button](https://passbolt.testrail.io/index.php?/cases/view/8142)
     *
     *     Given that I am on removal popup of the <resource>
     *     When I click 'Delete' button in @blue
     *     Then I am back on the homepage
     *     And I see a popup "<password name> resource was deleted." in @green
     *
     */
    @Deprecated(message = "With the introduction of v5 resource types this test will be removed")
    @Test
    fun onThePasswordRemovalPopupICanClickTheDeleteButton() {
        val randomizedName = "$testedResource ${System.currentTimeMillis()}"
        composeTestRule.apply {
            createNewPasswordFromHomeScreen(randomizedName)
            chooseFilter(filters_menu_all_items)

            // unregister refresh idling resource after first refresh not to block the snackbar checks
            IdlingRegistry.getInstance().unregister(resourcesFullRefreshIdlingResource)

            searchAndClickMoreOfFirstResource(randomizedName)
            onNodeWithText(getString(more_delete)).performClick()
            onNodeWithText(getString(delete)).performClick()

            waitForHomeScreen()
            onNodeWithTag(Home.SCREEN).assertIsDisplayed()
        }
    }

    /**  [On the password removal popup, I can delete resource when v5 resources are enabled](https://passbolt.testrail.io/index.php?/cases/view/13121)
     *
     *     Given that I am on removal popup of the <resource>
     *     When I click 'Delete' button in @blue
     *     Then I am back on the homepage
     *     And I don't see deleted resource on the list
     *
     *     Examples:
     *     | resource                       |
     *     | Simple password                |
     *     | Password with description      |
     *     | Password description totp      |
     *     | Simple Password (Deprecated)   |
     *     | Default resource type          |
     *     | Default resource type with TOTP|
     *
     */
    @Ignore("This test will be enabled after enabling V5 on cloud's `Betty` user")
    @Test
    fun onThePasswordRemovalPopupICanDeleteTheResourceAndItDisappearsFromTheList() {
        val randomizedName = "$testedResource ${System.currentTimeMillis()}"
        composeTestRule.apply {
            createNewPasswordFromHomeScreen(randomizedName)
            chooseFilter(filters_menu_all_items)

            searchAndClickMoreOfFirstResource(randomizedName)
            onNodeWithText(getString(more_delete)).performClick()
            onNodeWithText(getString(delete)).performClick()

            waitForHomeScreen()
            onNodeWithTag(Home.SCREEN).assertIsDisplayed()
            onNodeWithText(getString(LocalizationR.string.no_passwords)).assertIsDisplayed()
        }
    }

    /**  [After deletion I can see confirmation snackbar when v5 resources are enabled](https://passbolt.testrail.io/index.php?/cases/view/13122)
     *
     *     Given that I am on removal popup of the <resource>
     *     When I click 'Delete' button in @blue
     *     Then I see a snackbar "<password name> password was deleted." in @green
     *
     *     Examples:
     *     | resource                       |
     *     | Simple password                |
     *     | Password with description      |
     *     | Password description totp      |
     *     | Simple Password (Deprecated)   |
     *     | Default resource type          |
     *     | Default resource type with TOTP|
     *
     */
    @Ignore("This test will be enabled after enabling V5 on cloud's `Betty` user")
    @Test
    fun afterDeletionICanSeeConfirmationPopUpWhenV5ResourcesAreEnabled() {
        composeTestRule.apply {
            // unregister refresh idling resource after first refresh not to block the snackbar checks
            IdlingRegistry.getInstance().unregister(resourcesFullRefreshIdlingResource)

            searchAndClickMoreOfFirstResource(testedResource)
            onNodeWithText(getString(more_delete)).performClick()
            onNodeWithText(getString(delete)).performClick()

            waitForHomeScreen()
            onNodeWithTag(Home.SCREEN).assertIsDisplayed()
        }
    }
}
