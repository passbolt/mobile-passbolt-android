/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021-2023 Passbolt SA
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

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.KoinTest
import org.koin.test.inject
import com.passbolt.mobile.android.core.localization.R as LocalizationR

// TODO update tests to adjust to compose
@RunWith(AndroidJUnit4::class)
@MediumTest
class SetupAutofillNotConfiguredTest : KoinTest {
    @get:Rule(order = 0)
    val startActivityRule =
        lazyActivityScenarioRule<StartUpActivity>(
            koinOverrideModules = listOf(instrumentationTestsModule, autofillNotConfiguredModuleTests),
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

    @get:Rule
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
//        onView(withId(CoreUiR.id.input)).perform(typeText(managedAccountIntentCreator.getPassphrase()), closeSoftKeyboard())
        // TODO rewrite to compose
//        onView(withId(AuthenticationR.id.authButton)).perform(scrollTo(), click())
    }

    @After
    fun tearDown() {
        accountDataCleaner.clearAccountData()
    }

    //    https://passbolt.testrail.io/index.php?/cases/view/2366
    @Test
    fun asAMobileUserIShouldBePromptedToEnableTheSettingsOfTheAutofillDuringTheSetupProcess() {
//        //    Given     Autofill is not configured for Passbolt
//        //    When      I skip or finish the biometric configuration
//        composeTestRule.onNodeWithText(getString(LocalizationR.string.common_maybe_later)).performClick()
//        composeTestRule.waitForIdle()
//        //    Then      I am on the page explaining the Autofill configuration
//        //    And       I see a "Go to settings" primary button
//        onView(withId(AutofillR.id.goToSettingsButton)).check(matches(isDisplayed()))
//        onView(withId(AutofillR.id.closeButton)).check(matches(isDisplayed()))
//        onView(withId(AutofillR.id.stepsView)).check(matches(isDisplayed()))
//        onView(withText(getString(LocalizationR.string.dialog_encourage_autofill_header))).check(matches(isDisplayed()))
//        //    And       I see a "Maybe later" button
//        onView(withId(AutofillR.id.maybeLaterButton)).check(matches(isDisplayed()))
    }

    //    https://passbolt.testrail.io/index.php?/cases/view/2364
    @Test
    fun asAMobileUserIShouldBeAbleToSetupPassboltAutofillDuringTheSetupProcessIfItIsNotAlreadyConfigured() {
//        Intents.init()
//
//        try {
//            //    Given     I am on the Autofill setup page
//            composeTestRule.onNodeWithText(getString(LocalizationR.string.common_maybe_later)).performClick()
//            composeTestRule.waitForIdle()
//            //    When      I click on the "Go to settings" button
//            onView(withId(AutofillR.id.goToSettingsButton)).perform(click())
//            //    Then      I am redirected to the settings of the page for Autofill or to the Settings where I can enable the autofill
//            Intents.intended(
//                allOf(
//                    IntentMatchers.hasAction(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE),
//                ),
//            )
//            //    And       I can go back to the application
//        } finally {
//            Intents.release()
//        }
    }

    //    https://passbolt.testrail.io/index.php?/cases/view/2367
    @Test
    fun asAMobileUserIShouldBeAbleToSkipTheAutofillConfigurationDuringTheSetupProcess() {
//        //    Given     I am on the Autofill setup page
//        composeTestRule.onNodeWithText(getString(LocalizationR.string.common_maybe_later)).performClick()
//        composeTestRule.waitForIdle()
//        //    When      I click on the "Maybe later" button
//        onView(withId(AutofillR.id.maybeLaterButton)).perform(click())
//        //    Then      I am redirected to the home page
//        composeTestRule.onNodeWithTag(Home.SCREEN).assertIsDisplayed()
    }
}
