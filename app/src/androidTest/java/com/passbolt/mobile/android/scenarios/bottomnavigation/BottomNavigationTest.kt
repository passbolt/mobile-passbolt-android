/*
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2025 Passbolt SA
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

package com.passbolt.mobile.android.scenarios.bottomnavigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.passbolt.mobile.android.core.idlingresource.ResourcesFullRefreshIdlingResource
import com.passbolt.mobile.android.core.idlingresource.SignInIdlingResource
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.feature.authentication.AuthenticationMainActivity
import com.passbolt.mobile.android.helpers.signIn
import com.passbolt.mobile.android.instrumentationTestsModule
import com.passbolt.mobile.android.intents.ManagedAccountIntentCreator
import com.passbolt.mobile.android.rules.IdlingResourceRule
import com.passbolt.mobile.android.rules.lazyActivitySetupScenarioRule
import com.passbolt.mobile.android.testtags.composetags.BottomNav
import com.passbolt.mobile.android.testtags.composetags.Home
import com.passbolt.mobile.android.testtags.composetags.Otp
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.component.inject
import org.koin.test.KoinTest

@RunWith(AndroidJUnit4::class)
@LargeTest
class BottomNavigationTest : KoinTest {
    @get:Rule(order = 0)
    val startUpActivityRule =
        lazyActivitySetupScenarioRule<AuthenticationMainActivity>(
            koinOverrideModules =
                listOf(
                    instrumentationTestsModule,
                ),
            intentSupplier = {
                ActivityIntents.authentication(
                    getInstrumentation().targetContext,
                    ActivityIntents.AuthConfig.Startup,
                    AppContext.APP,
                    managedAccountIntentCreator.getUserLocalId(),
                )
            },
        )

    @get:Rule
    val idlingResourceRule =
        let {
            val signInIdlingResource: SignInIdlingResource by inject()
            val resourcesFullRefreshIdlingResource: ResourcesFullRefreshIdlingResource by inject()
            IdlingResourceRule(arrayOf(signInIdlingResource, resourcesFullRefreshIdlingResource))
        }

    private val managedAccountIntentCreator: ManagedAccountIntentCreator by inject()

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Before
    fun setup() {
        composeTestRule.signIn(managedAccountIntentCreator.getPassphrase())
    }

    /**
     * **I can go to the settings workspace using the bottom navigation**
     *
     * TestRail: {@link https://passbolt.testrail.io/index.php?/cases/view/2388}
     *
     * Given    that I am a logged in mobile user on the homepage
     * When     I click on settings in the navigation
     * Then     I see the settings page for this account
     * And      I see Settings button highlighted
     */
    @Test
    fun iCanGoToTheSettingsWorkspaceUsingTheBottomNavigation() {
        composeTestRule.apply {
            onNodeWithTag(BottomNav.SETTINGS_TAB)
                .assertIsDisplayed()
                .assertIsNotSelected()
            onNodeWithTag(BottomNav.SETTINGS_TAB).performClick()
            onNodeWithTag(BottomNav.SETTINGS_TAB)
                .assertIsSelected()
        }
    }

    /**
     * **I can go to TOTP list using bottom navigation**
     *
     * TestRail: {@link https://passbolt.testrail.io/index.php?/cases/view/16001}
     *
     * Given    that I am a logged in user
     * And      I have enabled at least one TOTP resource type on my server instance
     * When     I click on the TOTP button in the navigation
     * Then     I am on the TOTP page
     * And      TOTP button is highlighted
     */
    @Test
    fun iCanGoToTotpListUsingBottomNavigation() {
        composeTestRule.apply {
            onNodeWithTag(BottomNav.OTP_TAB)
                .assertIsDisplayed()
                .assertIsNotSelected()
            onNodeWithTag(BottomNav.OTP_TAB).performClick()
            onNodeWithTag(Otp.SCREEN)
                .assertIsDisplayed()
            onNodeWithTag(BottomNav.OTP_TAB)
                .assertIsSelected()
        }
    }

    /**
     * **I can go to the home using bottom navigation**
     *
     * TestRail: {@link https://passbolt.testrail.io/index.php?/cases/view/2389}
     *
     * Given    that I am a logged in mobile user on the settings
     * When     I click on home button in the navigation
     * Then     I see the home page
     * And      The Home button is highlighted
     */
    @Test
    fun iCanGoToTheHomeUsingBottomNavigation() {
        composeTestRule.apply {
            // navigate to settings first
            onNodeWithTag(BottomNav.SETTINGS_TAB).performClick()
            // navigate back to home
            onNodeWithTag(BottomNav.HOME_TAB)
                .assertIsDisplayed()
                .assertIsNotSelected()
            onNodeWithTag(BottomNav.HOME_TAB).performClick()
            onNodeWithTag(Home.SCREEN).assertIsDisplayed()
            onNodeWithTag(BottomNav.HOME_TAB)
                .assertIsSelected()
            onNodeWithTag(BottomNav.OTP_TAB)
                .assertIsNotSelected()
            onNodeWithTag(BottomNav.SETTINGS_TAB)
                .assertIsNotSelected()
        }
    }
}
