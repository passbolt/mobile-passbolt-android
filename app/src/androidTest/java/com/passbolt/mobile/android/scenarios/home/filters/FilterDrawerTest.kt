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

package com.passbolt.mobile.android.scenarios.home.filters

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.passbolt.mobile.android.core.idlingresource.ResourcesFullRefreshIdlingResource
import com.passbolt.mobile.android.core.idlingresource.SignInIdlingResource
import com.passbolt.mobile.android.core.localization.R.string.filters_menu_title
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.feature.authentication.AuthenticationMainActivity
import com.passbolt.mobile.android.helpers.getString
import com.passbolt.mobile.android.helpers.signIn
import com.passbolt.mobile.android.instrumentationTestsModule
import com.passbolt.mobile.android.intents.ManagedAccountIntentCreator
import com.passbolt.mobile.android.rules.IdlingResourceRule
import com.passbolt.mobile.android.rules.lazyActivitySetupScenarioRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.component.inject
import org.koin.test.KoinTest
import kotlin.test.BeforeTest

@RunWith(AndroidJUnit4::class)
@LargeTest
class FilterDrawerTest : KoinTest {
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

    @get:Rule
    val idlingResourceRule =
        let {
            val signInIdlingResource: SignInIdlingResource by inject()
            val resourcesFullRefreshIdlingResource: ResourcesFullRefreshIdlingResource by inject()
            IdlingResourceRule(arrayOf(signInIdlingResource, resourcesFullRefreshIdlingResource))
        }

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @BeforeTest
    fun setup() {
        composeTestRule.signIn(managedAccountIntentCreator.getPassphrase())
    }

    // https://passbolt.testrail.io/index.php?/cases/view/2616
    @Test
    fun asALoggedInMobileUserOnTheHomepageICanSeeAFilterIconInTheSearchBar() {
        //    Given     that I am a logged in mobile user
        //    When      I am on the homepage
        //    And       the search bar is not focused
        //    Then      I see an icon filter in the left side of the search bar
        composeTestRule.onNodeWithTag("home_search_filter").assertExists()
    }

    // https://passbolt.testrail.io/index.php?/cases/view/2617
    @Test
    fun asALoggedInMobileUserOnTheHomepageICanSeeTheFilterDrawer() {
        //    Given     that I am a logged in mobile user on the homepage
        //    When      I click on the filter icon
        composeTestRule.onNodeWithTag("home_search_filter").performClick()
        //    Then      I see the “filter” drawer
        composeTestRule.onNodeWithTag("filters_menu_sheet").assertIsDisplayed()
        //    And       I see the homepage is greyed out in the background
        //    And       I see a “Filter view by” title
        composeTestRule.onNodeWithText(getString(filters_menu_title)).assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_sheet_icon_close").assertIsDisplayed()
        //    And       I see <filter> list item with their corresponding icon
        ResourceFilterModel.entries.forEach { model ->
            composeTestRule
                .onNode(
                    hasTestTag("filters_menu_sheet").and(
                        hasAnyDescendant(hasText(getString(model.filterNameId))),
                    ),
                ).assertExists()
        }
    }
}
