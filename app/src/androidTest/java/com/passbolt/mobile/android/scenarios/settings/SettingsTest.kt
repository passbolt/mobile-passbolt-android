/**
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

package com.passbolt.mobile.android.scenarios.settings

import android.app.Instrumentation.ActivityResult
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.passbolt.mobile.android.core.idlingresource.ResourcesFullRefreshIdlingResource
import com.passbolt.mobile.android.core.idlingresource.SignInIdlingResource
import com.passbolt.mobile.android.core.idlingresource.SignOutIdlingResource
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.feature.authentication.AuthenticationMainActivity
import com.passbolt.mobile.android.helpers.getString
import com.passbolt.mobile.android.helpers.signIn
import com.passbolt.mobile.android.instrumentationTestsModule
import com.passbolt.mobile.android.intents.ManagedAccountIntentCreator
import com.passbolt.mobile.android.rules.IdlingResourceRule
import com.passbolt.mobile.android.rules.lazyActivitySetupScenarioRule
import com.passbolt.mobile.android.scenarios.setup.autofill.autofillConfiguredModuleTests
import com.passbolt.mobile.android.scenarios.setup.configurebiometric.biometricSetupUnavailableModuleTests
import com.passbolt.mobile.android.testtags.composetags.BackNavigation
import com.passbolt.mobile.android.testtags.composetags.BottomNav
import com.passbolt.mobile.android.testtags.composetags.OpenableSetting
import com.passbolt.mobile.android.testtags.composetags.SwitchWithDescription
import com.passbolt.mobile.android.testtags.composetags.SwitchableSetting
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.component.inject
import org.koin.test.KoinTest
import com.passbolt.mobile.android.core.localization.R as LocalizationR

@RunWith(AndroidJUnit4::class)
@LargeTest
class SettingsTest : KoinTest {
    @get:Rule(order = 1)
    val startUpActivityRule =
        lazyActivitySetupScenarioRule<AuthenticationMainActivity>(
            koinOverrideModules =
                listOf(
                    instrumentationTestsModule,
                    biometricSetupUnavailableModuleTests,
                    autofillConfiguredModuleTests,
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

    @get:Rule(order = 0)
    val composeTestRule = createEmptyComposeRule()

    private val managedAccountIntentCreator: ManagedAccountIntentCreator by inject()

    @get:Rule
    val idlingResourceRule =
        let {
            val signInIdlingResource: SignInIdlingResource by inject()
            val signOutIdlingResource: SignOutIdlingResource by inject()
            val resourcesFullRefreshIdlingResource: ResourcesFullRefreshIdlingResource by inject()
            IdlingResourceRule(arrayOf(signInIdlingResource, resourcesFullRefreshIdlingResource, signOutIdlingResource))
        }

    @Before
    fun setup() {
        //    #MOBILE_USER_ON_SETTINGS_PAGE:
        //    Given	I am a mobile user with the application installed
        //    And	I am logged in
        //    And 	I am on Passbolt PRO/CE/Cloud
        composeTestRule.apply {
            signIn(managedAccountIntentCreator.getPassphrase())
            onNodeWithTag(BottomNav.SETTINGS_TAB).performClick()
        }
    }

    //    https://passbolt.testrail.io/index.php?/cases/view/2438
    @Test
    fun asAMobileUserOnTheMainSettingsPageICanSeeTheListOfSettingsIHaveAccessTo() {
        //    Given     that I am #MOBILE_USER_ON_SETTINGS_PAGE
        //    When      I'm staying on the Settings page
        composeTestRule.apply {
            //    Then      I see the "Settings" title
            onAllNodesWithText(getString(LocalizationR.string.settings_title))[0]
                .assertIsDisplayed()

            //    And       I see a "App settings" with an settings icon and a caret on the right
            //    And       I see a "Accounts" with an personas icon and a caret on the right
            //    And       I see a "Terms & licences" with an info icon and a caret on the right
            //    And       I see a "Debug, logs" with an bug icon and a caret on the right
            //    And       I see a "Sign out" with an exit icon
            SettingsMenuItemModel.entries.forEach { item ->
                var matcher =
                    hasTestTag(item.testTag)
                        .and(hasAnyDescendant(hasText(getString(item.settingsItemTextId))))
                        .and(hasAnyDescendant(hasContentDescription(getString(item.settingsItemTextId))))

                if (item.hasOpenableIcon) {
                    matcher = matcher.and(hasAnyDescendant(hasTestTag(OpenableSetting.ARROW)))
                }

                composeTestRule
                    .onNode(matcher, useUnmergedTree = true)
                    .assertExists()
            }
        }
    }

    @Test
    fun asAnAndroidUserICanSeeAppSettings() {
        //    Given     that I am #MOBILE_USER_ON_SETTINGS_PAGE
        composeTestRule.apply {
            //    When 	    I click on the "App settings" button
            onNodeWithText(getString(LocalizationR.string.settings_app_settings)).performClick()
            //    Then      I see the "App settings" title
            onNodeWithText(getString(LocalizationR.string.settings_app_settings))
                .assertIsDisplayed()

            //    And	    I see a back button to go to the main settings page
            onNode(
                hasTestTag(BackNavigation.ICON),
                useUnmergedTree = true,
            ).assertIsDisplayed()

            //    And 	    I see a Fingerprint with a fingerprint icon and a switch on the right
            //    And 	    I see a Autofill with a key icon and a caret on the right
            //    And 	    I see a Default filter with a filter icon and a caret on the right
            //    And 	    I see a Expert settings with a gear icon and a caret on the right
            AppSettingsItemModel.entries.forEach { item ->
                var matcher =
                    hasTestTag(item.testTag)
                        .and(hasAnyDescendant(hasText(getString(item.settingsItemTextId))))
                        .and(hasAnyDescendant(hasContentDescription(getString(item.settingsItemTextId))))

                if (item.hasOpenableIcon) {
                    matcher = matcher.and(hasAnyDescendant(hasTestTag(OpenableSetting.ARROW)))
                }

                composeTestRule
                    .onNode(matcher, useUnmergedTree = true)
                    .assertExists()
            }
        }
    }

    @Test
    fun asAnAndroidUserICanSeeExpertSettings() {
        //    Given     that I am #MOBILE_USER_ON_SETTINGS_PAGE
        composeTestRule.apply {
            //    And	    I'm on the "App settings" screen
            onNodeWithText(getString(LocalizationR.string.settings_app_settings)).performClick()
            //    When 	    I click on the "Expert settings" button
            onNodeWithText(getString(LocalizationR.string.settings_app_settings_expert_settings)).performClick()
            //    Then      I see the "Expert settings" title
            onNodeWithText(getString(LocalizationR.string.settings_app_settings_expert_settings))
                .assertIsDisplayed()
            //    And 	    I see the back button to go to the main settings page
            onNode(
                hasTestTag(BackNavigation.ICON),
                useUnmergedTree = true,
            ).assertIsDisplayed()
            //    And 	    I see a Developer mode with an nodes icon and a switch on the right
            //    And 	    I see a Hide "device is rooted" dialog with an hash icon and a switch on the right

            ExpertSettingsItemModel.entries.forEach { item ->
                val matcher =
                    hasTestTag(item.testTag)
                        .and(hasAnyDescendant(hasText(getString(item.settingsItemTextId))))
                        .and(hasAnyDescendant(hasContentDescription(getString(item.settingsItemTextId))))

                composeTestRule
                    .onNode(matcher, useUnmergedTree = true)
                    .assertExists()
            }
        }
    }

    @Test
    fun asAnAndroidUserICanEnableDeveloperMode() {
        //    Given     that I am #MOBILE_USER_ON_SETTINGS_PAGE
        composeTestRule.apply {
            //    And	    I'm on the "Expert settings" screen
            onNodeWithText(getString(LocalizationR.string.settings_app_settings)).performClick()
            onNodeWithText(getString(LocalizationR.string.settings_app_settings_expert_settings)).performClick()
            //    When 	    I enable the "Developer mode" switch
            onAllNodesWithTag(SwitchableSetting.SWITCH)[0].performClick()
            //    Then      I see that switch is enabled
            onAllNodesWithTag(SwitchableSetting.SWITCH)[0].assertIsOn()
            //    And 	    I see that "Hide "device is rooted" dialog" switch is available
            onAllNodesWithTag(SwitchableSetting.SWITCH)[1].assertIsEnabled()
        }
    }

    @Test
    fun asAnAndroidUserICanDisableDeveloperMode() {
        //    Given     that I am #MOBILE_USER_ON_SETTINGS_PAGE
        composeTestRule.apply {
            //    And	    I'm on the "Expert settings" screen
            onNodeWithText(getString(LocalizationR.string.settings_app_settings)).performClick()
            onNodeWithText(getString(LocalizationR.string.settings_app_settings_expert_settings)).performClick()
            //    When 	    I disable the "Developer mode" switch
            onAllNodesWithTag(SwitchableSetting.SWITCH)[0].performClick()
            onAllNodesWithTag(SwitchableSetting.SWITCH)[0].performClick()
            //    And 	    I see that every subsequent position is unavailable
            onAllNodesWithTag(SwitchableSetting.SWITCH)[0].assertIsOff()
            onAllNodesWithTag(SwitchableSetting.SWITCH)[1].assertIsNotEnabled()
        }
    }

    @Test
    fun asAnAndroidUserICanHideDeviceIsRootedDialog() {
        //    Given     that I am #MOBILE_USER_ON_SETTINGS_PAGE
        composeTestRule.apply {
            //    And	    I'm on the "Expert settings" screen
            onNodeWithText(getString(LocalizationR.string.settings_app_settings)).performClick()
            onNodeWithText(getString(LocalizationR.string.settings_app_settings_expert_settings)).performClick()
            //    When 	    I enable the "Hide "device is rooted" dialog" switch
            onAllNodesWithTag(SwitchableSetting.SWITCH)[0].performClick()
            onAllNodesWithTag(SwitchableSetting.SWITCH)[1].performClick()
            onAllNodesWithTag(SwitchableSetting.SWITCH)[1].assertIsOn()
            onAllNodesWithTag(SwitchableSetting.SWITCH)[1].assertIsOn()
        }
    }

    @Test
    fun asAMobileUserICanSeeAccounts() {
        //    Given     that I am #MOBILE_USER_ON_SETTINGS_PAGE
        //    When 	    I click on the "Accounts" button
        //    Then      I see the "Accounts" title
        //    And 	    I see the back button to go to the main settings page
        composeTestRule.apply {
            onNodeWithText(getString(LocalizationR.string.settings_accounts)).performClick()
            //    And 	    I see a Manage accounts with a person icon and a caret on the right
            val manageAccountMatcher =
                hasTestTag(OpenableSetting.ITEM)
                    .and(hasAnyDescendant(hasText(getString(LocalizationR.string.settings_accounts_transfer_account))))
                    .and(hasAnyDescendant(hasContentDescription(getString(LocalizationR.string.settings_accounts_transfer_account))))
                    .and(hasAnyDescendant(hasTestTag(OpenableSetting.ARROW)))

            composeTestRule
                .onNode(manageAccountMatcher, useUnmergedTree = true)
                .assertExists()
            //    And 	    I see a Transfer account to another device with an lorry icon and a caret on the right
            val transferAccountMatcher =
                hasTestTag(OpenableSetting.ITEM)
                    .and(hasAnyDescendant(hasText(getString(LocalizationR.string.settings_accounts_transfer_account))))
                    .and(hasAnyDescendant(hasContentDescription(getString(LocalizationR.string.settings_accounts_transfer_account))))
                    .and(hasAnyDescendant(hasTestTag(OpenableSetting.ARROW)))

            composeTestRule
                .onNode(transferAccountMatcher, useUnmergedTree = true)
                .assertExists()
        }
    }

    @Test
    fun asALoggedInMobileUserOnTheSettingsPageAndBiometricIsDisabledOnMyDeviceICanEnableBiometrics() {
        //    Given     that I am a mobile user with the application installed
        //    And       I completed the login step
        //    And       Biometric is disabled on my device
        //    And       I am on the settings page
        composeTestRule.apply {
            onNodeWithText(getString(LocalizationR.string.settings_app_settings)).performClick()
            //    When      I switch on the biometrics toggle button
            onAllNodesWithTag(SwitchableSetting.SWITCH)[0].performClick()
            //    Then      I am prompted to configure biometrics in the device settings
            onNodeWithText(getString(LocalizationR.string.settings_add_first_fingerprint_title)).assertIsDisplayed()
            //    And       I see a "Cancel" button to go back to the previous state
            onNodeWithText(getString(LocalizationR.string.cancel)).assertIsDisplayed()
            //    And       I see a "Go to settings" button to configure biometrics
            onNodeWithText(getString(LocalizationR.string.dialog_encourage_autofill_go_to_settings)).assertIsDisplayed()
        }
    }

    @Test
    fun asALoggedInAndroidUserOnTheSettingsPageICanEnableAutofill() {
        //    Given     that I am a mobile user with the application installed
        //    And       the Passbolt application is already opened
        //    And       I completed the login step
        //    And       Autofill is enabled
        //    And       I am on the settings page
        composeTestRule.apply {
            onNodeWithText(getString(LocalizationR.string.settings_app_settings)).performClick()
            //    When      I click on the "Autofill" list item
            onNodeWithText(getString(LocalizationR.string.settings_app_settings_autofill)).performClick()
            waitForIdle()
            onAllNodesWithTag(SwitchWithDescription.SWITCH, useUnmergedTree = true)[0].performClick()
            waitForIdle()
            //    Then      I see the "Passbolt Autofill enabled!" screen
            onNodeWithText(getString(LocalizationR.string.dialog_autofill_enabled_title)).assertIsDisplayed()
        }
    }

    @Test
    fun asAnAndroidUserICanSeeTermsAndLicences() {
        Intents.init()

        //    Given     that I am #MOBILE_USER_ON_SETTINGS_PAGE
        //    Then      I see the "Terms & licences" title
        composeTestRule.apply {
            //    When 	    I click on the "Terms & licences" button
            onNodeWithText(getString(LocalizationR.string.settings_terms_and_licenses)).performClick()
            onNodeWithText(getString(LocalizationR.string.settings_terms_and_licenses_terms)).performClick()
            //    And 	    I see the back button to go to the main settings page
            //    And 	    I see a Terms & Conditions with an info icon and a caret on the right
            //    And 	    I see a Privacy policy with an lock icon and a caret on the right
            //    And 	    I see a Open Source Licences with an feather icon and a caret on the right
            val expectedIntent: Matcher<Intent> = allOf(hasAction(Intent.ACTION_VIEW), hasData("passbolt.com/terms"))
            intending(expectedIntent).respondWith(ActivityResult(0, null))

            Intents.release()
        }
    }

    @Test
    fun asALoggedInMobileUserOnTheSettingsPageICanOpenThePrivacyPolicePage() {
        Intents.init()

        //    Given     that I am a mobile user with the application installed
        //    And       the Passbolt application is already opened
        //    And       I completed the login step
        //    And       I am on the settings page
        composeTestRule.apply {
            onNodeWithText(getString(LocalizationR.string.settings_terms_and_licenses)).performClick()
            //    When      I click on "Privacy policy" list item
            onNodeWithText(getString(LocalizationR.string.settings_terms_and_licenses_privacy_policy)).performClick()
            //    Then      i see a "Privacy policy" page (as a web page)
            val expectedIntent: Matcher<Intent> = allOf(hasAction(Intent.ACTION_VIEW), hasData("passbolt.com/privacy"))
            intending(expectedIntent).respondWith(ActivityResult(0, null))

            Intents.release()
        }
    }

    @Test
    fun asAnAndroidUserISeeDebugLogs() {
        //    Given     that I am #MOBILE_USER_ON_SETTINGS_PAGE
        composeTestRule.apply {
            //    When 	    I click on the "Debug, logs" button
            onNodeWithText(getString(LocalizationR.string.settings_debug_logs)).performClick()
            //    Then      I see the "Debug, logs" title
            onNodeWithText(getString(LocalizationR.string.settings_debug_logs))
                .assertIsDisplayed()
            //    And 	    I see the back button to go to the main settings page
            onNode(
                hasTestTag(BackNavigation.ICON),
                useUnmergedTree = true,
            ).assertIsDisplayed()
            //    And 	    I see a Enable debug logs with an bug icon and a switch on the right
            //    And 	    I see a Access the logs with an sheet icon and a caret on the right
            //    And 	    I see a Visit help site with an chain icon and a caret on the right
            DebugLogsItemModel.entries.forEach { item ->
                var matcher =
                    hasTestTag(item.testTag)
                        .and(hasAnyDescendant(hasText(getString(item.settingsItemTextId))))
                        .and(hasAnyDescendant(hasContentDescription(getString(item.settingsItemTextId))))

                if (item.hasOpenableIcon) {
                    matcher = matcher.and(hasAnyDescendant(hasTestTag(OpenableSetting.ARROW)))
                }

                composeTestRule
                    .onNode(matcher, useUnmergedTree = true)
                    .assertExists()
            }
        }
    }

    @Test
    fun asALoggedInMobileUserOnTheSettingsPageICanSignOut() {
        //    Given     that I am a mobile user with the application installed
        //    And       the Passbolt application is already opened
        //    And       I completed the login step
        //    And       I am on the settings page
        composeTestRule.apply {
            //    When      I click on the "Sign out" list item
            onNodeWithText(getString(LocalizationR.string.settings_sign_out)).performClick()
            //    Then      I see a confirmation modal
            onNodeWithText(getString(LocalizationR.string.are_you_sure)).assertIsDisplayed()
            onNodeWithText(getString(LocalizationR.string.logout_dialog_message)).assertIsDisplayed()
            //    And       I see a sign out button
            onAllNodesWithText(getString(LocalizationR.string.common_sign_out))[1].assertIsDisplayed()
            //    And       I see a cancel button
            onNodeWithText(getString(LocalizationR.string.cancel)).assertIsDisplayed()
            //    When      I click on the "Sign out" button
            onAllNodesWithText(getString(LocalizationR.string.common_sign_out))[1].performClick()
            //    Then      I see the "Sign in - List of accounts" welcome screen
            onNodeWithText(getString(LocalizationR.string.accounts_list_title)).assertIsDisplayed()
        }
    }

    @Test
    fun asALoggedInMobileUserOnTheSettingsPageINeedToConfirmSignOut() {
        //    Given     that I am a mobile user with the application installed
        //    And       I am on the settings page
        composeTestRule.apply {
            //    When      I click on the "Sign out" list item
            onNodeWithText(getString(LocalizationR.string.settings_sign_out)).performClick()
            //    Then      I see a confirmation modal
            onNodeWithText(getString(LocalizationR.string.are_you_sure)).assertIsDisplayed()
            onNodeWithText(getString(LocalizationR.string.logout_dialog_message)).assertIsDisplayed()
            //    And       I see a sign out button
            onAllNodesWithText(getString(LocalizationR.string.common_sign_out))[1].assertIsDisplayed()
            //    And       I see a cancel button
            onNodeWithText(getString(LocalizationR.string.cancel)).assertIsDisplayed()
            //    When      I click "Cancel" button
            onNodeWithText(getString(LocalizationR.string.cancel)).performClick()
            onAllNodesWithText(getString(LocalizationR.string.settings_title))[0]
                .assertIsDisplayed()
        }
    }
}
