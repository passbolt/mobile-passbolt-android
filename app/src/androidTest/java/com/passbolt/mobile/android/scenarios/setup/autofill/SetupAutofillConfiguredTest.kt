/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021-2023,2025 Passbolt SA
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

package com.passbolt.mobile.android.scenarios.setup.autofill

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.passbolt.mobile.android.accountinit.AccountDataCleaner
import com.passbolt.mobile.android.accountinit.AccountInitializer
import com.passbolt.mobile.android.core.idlingresource.ResourcesFullRefreshIdlingResource
import com.passbolt.mobile.android.core.idlingresource.SignInIdlingResource
import com.passbolt.mobile.android.feature.startup.StartUpActivity
import com.passbolt.mobile.android.helpers.getString
import com.passbolt.mobile.android.instrumentationTestsModule
import com.passbolt.mobile.android.intents.ManagedAccountIntentCreator
import com.passbolt.mobile.android.rules.IdlingResourceRule
import com.passbolt.mobile.android.rules.lazyActivityScenarioRule
import com.passbolt.mobile.android.testtags.composetags.Auth
import com.passbolt.mobile.android.testtags.composetags.Home
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.KoinTest
import org.koin.test.inject
import com.passbolt.mobile.android.core.localization.R as LocalizationR

@RunWith(AndroidJUnit4::class)
@MediumTest
class SetupAutofillConfiguredTest : KoinTest {
    @get:Rule(order = 1)
    val startActivityRule =
        lazyActivityScenarioRule<StartUpActivity>(
            koinOverrideModules = listOf(instrumentationTestsModule, autofillConfiguredModuleTests),
            intentSupplier = {
                managedAccountIntentCreator.createIntent(
                    InstrumentationRegistry.getInstrumentation().targetContext,
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

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

    @get:Rule(order = 0)
    val composeTestRule = createEmptyComposeRule()

    private val managedAccountIntentCreator: ManagedAccountIntentCreator by inject()
    private val accountDataCleaner: AccountDataCleaner by inject()
    private val accountDataInitializer: AccountInitializer by inject()

    @Before
    fun setup() {
        accountDataCleaner.clearAccountData()
        composeTestRule.apply {
            onNodeWithText(getString(LocalizationR.string.welcome_connect_to_existing_account)).performClick()
            onNodeWithText(getString(LocalizationR.string.transfer_details_scan_button)).performClick()
            onNodeWithText(getString(LocalizationR.string.continue_label)).performClick()
        }
        accountDataInitializer.initializeAccount()
        composeTestRule.apply {
            onNodeWithTag(Auth.PASSPHRASE_INPUT).performTextReplacement(managedAccountIntentCreator.getPassphrase())
            onNodeWithTag(Auth.SIGN_IN_BUTTON).performClick()
        }
    }

    @After
    fun tearDown() {
        accountDataCleaner.clearAccountData()
    }

    //    https://passbolt.testrail.io/index.php?/cases/view/2365
    @Test
    fun asAMobileUserIShouldNotSeeAPromptToEnableAutofillForPassboltIfItIsAlreadyConfigured() {
        composeTestRule.apply {
            //    Given     Autofill is configured for Passbolt
            //    When      I skip or finish the biometric configuration
            onNodeWithText(getString(LocalizationR.string.common_maybe_later)).performClick()
            //    Then      I do not see the page explaining the autofill configuration
            //    And       I see the home page
            onNodeWithTag(Home.SCREEN).assertIsDisplayed()
        }
    }
}
