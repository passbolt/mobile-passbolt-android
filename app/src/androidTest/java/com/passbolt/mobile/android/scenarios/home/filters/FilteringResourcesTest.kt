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
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.passbolt.mobile.android.core.idlingresource.ResourcesFullRefreshIdlingResource
import com.passbolt.mobile.android.core.idlingresource.SignInIdlingResource
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
class FilteringResourcesTest : KoinTest {
    @get:Rule
    val startUpActivityRule =
        lazyActivitySetupScenarioRule<AuthenticationMainActivity>(
            koinOverrideModules =
                listOf(
                    instrumentationTestsModule,
                ),
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

    /**
     *  [As a logged in mobile user on the homepage I can change the current active filter](https://passbolt.testrail.io/index.php?/cases/view/11224)
     *
     * Given       that I am a logged in mobile user
     * And         the active filter is   not   <filter>
     * When        I open the filter drawer
     * And         I click on the <filter> list item
     * Then        I do not see the filter drawer
     * And         I see the homepage
     * And         I see the title is <filter> with its corresponding icon //TODO: how to reliably check the icon is correctly placed near corresponding filter element
     * And         I see the list of resources contains <filter> elements //TODO: how to reliably check items corresponding to filter name
     *
     * Examples:
     *
     * | filter              |
     *
     * | “All items”         |
     * | “Favourites”        |
     * | “Recently modified” |
     * | “Shared with me”    |
     * | “Owned by me”       |
     * | “Expiry”            |
     * | “Folders”           |
     * | “Tags”              |
     * | “Groups”            |
     */
    @Test
    @FlakyTest(detail = "It is currently failing nondeterministic on Android 12 - reason unknown")
    fun asALoggedInMobileUserOnTheHomepageICanChangeTheCurrentActiveFilter() {
        ResourceFilterModel.entries.forEach { model ->
            composeTestRule.onNodeWithTag("home_search_filter").performClick()
            composeTestRule
                .onNode(
                    hasClickAction().and(
                        hasAnyDescendant(hasText(getString(model.filterNameId))),
                    ),
                    useUnmergedTree = true,
                ).performClick()
            composeTestRule.onNodeWithTag("home_screen").assertIsDisplayed()
        }
    }
}
