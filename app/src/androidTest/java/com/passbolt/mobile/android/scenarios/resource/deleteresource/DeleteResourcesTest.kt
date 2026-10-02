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
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.IdlingRegistry
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.passbolt.mobile.android.core.idlingresource.CreateMenuModelIdlingResource
import com.passbolt.mobile.android.core.idlingresource.CreateResourceIdlingResource
import com.passbolt.mobile.android.core.idlingresource.DeleteResourceIdlingResource
import com.passbolt.mobile.android.core.idlingresource.ResourcesFullRefreshIdlingResource
import com.passbolt.mobile.android.core.idlingresource.SearchIdlingResource
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
import com.passbolt.mobile.android.instrumentationTestsModule
import com.passbolt.mobile.android.intents.ManagedAccountIntentCreator
import com.passbolt.mobile.android.rules.IdlingResourceRule
import com.passbolt.mobile.android.rules.lazyActivitySetupScenarioRule
import com.passbolt.mobile.android.testtags.composetags.Home
import org.junit.Before
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
            val searchIdlingResource: SearchIdlingResource by inject()
            IdlingResourceRule(
                arrayOf(
                    signInIdlingResource,
                    resourcesFullRefreshIdlingResource,
                    deleteIdlingResource,
                    createResourceIdlingResource,
                    createMenuModelIdlingResource,
                    searchIdlingResource,
                ),
            )
        }

    private companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Resource name: {0}")
        fun resourceNames() =
            listOf(
                "To be deleted - Default resource",
            )
    }

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Before
    fun setup() {
        composeTestRule.signIn(managedAccountIntentCreator.getPassphrase())
    }

    /**  [On the password removal popup, I can delete resource when v5 resources are enabled](https://passbolt.testrail.io/index.php?/cases/view/13121)
     *
     *     Given that I am on removal popup of the <resource>
     *     When I click 'Delete' button in @blue
     *     Then I am back on the homepage
     *     And I don't see deleted resource on the list
     *
     *     Examples:
     *     TODO: enable remaining rows once creation of other resource types is supported in the app
     *     | resource                       |
     *     | Simple password                |
     *     | Password with description      |
     *     | Password description totp      |
     *     | Simple Password (Deprecated)   |
     *     | Default resource type          |
     *     | Default resource type with TOTP|
     *
     */
    @Test
    fun onThePasswordRemovalPopupICanDeleteResourceWhenV5ResourcesAreEnabled() {
        val randomizedName = "$testedResource ${System.currentTimeMillis()}"
        composeTestRule.apply {
            chooseFilter(filters_menu_all_items)
            createNewPasswordFromHomeScreen(randomizedName)

            searchAndClickMoreOfFirstResource(randomizedName)
            onNodeWithText(getString(more_delete)).performClick()
            onNodeWithText(getString(delete)).performClick()

            val deletedRowMatcher =
                hasTestTag(Home.RESOURCE_ROW).and(
                    hasAnyDescendant(hasText(randomizedName, substring = true, ignoreCase = true)),
                )
            waitUntil(timeoutMillis = 20_000, conditionDescription = "Resource removed") {
                onAllNodes(deletedRowMatcher, useUnmergedTree = true).fetchSemanticsNodes().isEmpty()
            }

            val noPasswordsMatcher = hasText(getString(LocalizationR.string.no_passwords))
            waitUntil(timeoutMillis = 15_000, conditionDescription = "No passwords empty state shown") {
                onAllNodes(noPasswordsMatcher).fetchSemanticsNodes().isNotEmpty()
            }
            onNode(noPasswordsMatcher).assertIsDisplayed()
        }
    }

    /**  [After deletion I can see confirmation snackbar when v5 resources are enabled](https://passbolt.testrail.io/index.php?/cases/view/13122)
     *
     *     Given that I am on removal popup of the <resource>
     *     When I click ‘Delete’ button in @blue
     *     Then I see a snackbar "<password name> resource was deleted." in @green
     *
     *     TODO: enable remaining rows once creation of other resource types is supported in the app
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
    @Test
    fun afterDeletionICanSeeConfirmationPopUpWhenV5ResourcesAreEnabled() {
        val randomizedName = "$testedResource ${System.currentTimeMillis()}"
        composeTestRule.apply {
            createNewPasswordFromHomeScreen(randomizedName)

            // Unregister refresh idling resource so Espresso doesn't block on the
            // post-delete refresh and miss the transient snackbar.
            IdlingRegistry.getInstance().unregister(resourcesFullRefreshIdlingResource)

            searchAndClickMoreOfFirstResource(randomizedName)
            onNodeWithText(getString(more_delete)).performClick()
            onNodeWithText(getString(delete)).performClick()

            val snackbarText =
                getString(LocalizationR.string.common_message_resource_deleted, randomizedName)
            val snackbarMatcher = hasText(snackbarText, substring = true, ignoreCase = true)
            waitUntil(timeoutMillis = 15_000, conditionDescription = "Snackbar shown") {
                onAllNodes(snackbarMatcher).fetchSemanticsNodes().isNotEmpty()
            }
            onNode(snackbarMatcher).assertIsDisplayed()
        }
    }
}
