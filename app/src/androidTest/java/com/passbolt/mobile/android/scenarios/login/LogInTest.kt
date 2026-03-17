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

package com.passbolt.mobile.android.scenarios.login

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.passbolt.mobile.android.core.idlingresource.ResourcesFullRefreshIdlingResource
import com.passbolt.mobile.android.core.idlingresource.SignInIdlingResource
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.feature.authentication.AuthenticationMainActivity
import com.passbolt.mobile.android.helpers.getString
import com.passbolt.mobile.android.instrumentationTestsModule
import com.passbolt.mobile.android.intents.ManagedAccountIntentCreator
import com.passbolt.mobile.android.rules.IdlingResourceRule
import com.passbolt.mobile.android.rules.lazyActivitySetupScenarioRule
import com.passbolt.mobile.android.testtags.composetags.Auth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.passbolt.mobile.android.core.localization.R as LocalizationR

@RunWith(AndroidJUnit4::class)
@LargeTest
class LogInTest : KoinComponent {
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

    @get:Rule
    val idlingResourceRule =
        let {
            val signInIdlingResource: SignInIdlingResource by inject()
            val resourcesFullRefreshIdlingResource: ResourcesFullRefreshIdlingResource by inject()
            IdlingResourceRule(arrayOf(signInIdlingResource, resourcesFullRefreshIdlingResource))
        }

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    //  https://passbolt.testrail.io/index.php?/cases/view/2368
    @Test
    fun asAMobileUserWithoutBiometricsConfiguredIAmPromptedToEnterMyPassphraseToLogin() {
        //  Given  that I am a mobile user with the application installed
        //  And    I have only one account configured
        //  And    I am logged out
        //  And    I didn't configured biometrics for my passphrase
        //  When   I open the application
        //  Then   I see the splash screen for a few seconds
        //  And    I see a login page prompting me to enter the passphrase
        composeTestRule.apply {
            //  And    I see an input field to enter my passphrase
            onNodeWithTag(Auth.PASSPHRASE_INPUT)
                .assertIsDisplayed()
            //  And    I see an eye button to show my passphrase
            onNodeWithTag("password_input_visibility_toggle")
                .assertIsDisplayed()
            //  And    I do not see a biometric provider button <<not automated>>
            //  And    I see a "Sign in" primary button
            onNodeWithTag(Auth.SIGN_IN_BUTTON)
                .assertIsDisplayed()
            //  And    I see a forgot my passphrase button
            onNodeWithText(getString(LocalizationR.string.auth_forgot_password_button))
                .assertIsDisplayed()
            //  And    I see a back arrow leading to the "List of accounts" welcome screen
            onNodeWithTag("BackNavigationIcon", useUnmergedTree = true)
                .assertIsDisplayed()
            //  And    I see my avatar
            onNodeWithTag(Auth.AVATAR)
                .assertIsDisplayed()
        }
    }
}
