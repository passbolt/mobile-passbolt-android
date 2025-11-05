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
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isNotSelected
import androidx.test.espresso.matcher.ViewMatchers.isSelected
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.passbolt.mobile.android.core.idlingresource.ResourcesFullRefreshIdlingResource
import com.passbolt.mobile.android.core.idlingresource.SignInIdlingResource
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.feature.authentication.AuthenticationMainActivity
import com.passbolt.mobile.android.feature.home.R.id.homeNav
import com.passbolt.mobile.android.feature.otp.R.id.otpNav
import com.passbolt.mobile.android.feature.settings.R.id.settingsNavCompose
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
import com.passbolt.mobile.android.core.localization.R as LocalizationR

@RunWith(AndroidJUnit4::class)
@LargeTest
class BottomNavigationTest : KoinTest {
    @get:Rule
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
    val composeTestRule = createEmptyComposeRule()

    private val managedAccountIntentCreator: ManagedAccountIntentCreator by inject()

    @get:Rule
    val idlingResourceRule =
        let {
            val signInIdlingResource: SignInIdlingResource by inject()
            val resourcesFullRefreshIdlingResource: ResourcesFullRefreshIdlingResource by inject()
            IdlingResourceRule(arrayOf(signInIdlingResource, resourcesFullRefreshIdlingResource))
        }

    @BeforeTest
    fun setup() {
        composeTestRule
            .signIn(managedAccountIntentCreator.getPassphrase())
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
        onView(withId(settingsNavCompose))
            .check(matches(isDisplayed()))
            .check(matches(isNotSelected()))
        onView(withId(settingsNavCompose)).perform(click())
        composeTestRule.apply {
            waitForIdle()
            onNodeWithText(getString(LocalizationR.string.settings_title))
                .assertIsDisplayed()
        }
        onView(withId(settingsNavCompose))
            .check(matches(isSelected()))
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
        onView(withId(otpNav))
            .check(matches(isDisplayed()))
            .check(matches(isNotSelected()))
        onView(withId(otpNav)).perform(click())
        composeTestRule.apply {
            onNodeWithTag("otp_screen")
                .assertIsDisplayed()
        }
        onView(withId(otpNav))
            .check(matches(isSelected()))
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
        onView(withId(settingsNavCompose)).perform(click())
        onView(withId(homeNav))
            .check(matches(isDisplayed()))
            .check(matches(isNotSelected()))
        onView(withId(homeNav)).perform(click())
        composeTestRule.apply {
            onNodeWithTag("home_screen").assertIsDisplayed()
        }
        onView(withId(homeNav))
            .check(matches(isSelected()))
        onView(withId(otpNav))
            .check(matches(isNotSelected()))
        onView(withId(settingsNavCompose))
            .check(matches(isNotSelected()))
    }
}
