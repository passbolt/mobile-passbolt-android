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

package com.passbolt.mobile.android.scenarios.welcome

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.passbolt.mobile.android.core.ui.compose.topbar.BackNavigationIcon
import com.passbolt.mobile.android.feature.setup.SetUpActivity
import com.passbolt.mobile.android.helpers.getString
import com.passbolt.mobile.android.instrumentationTestsModule
import com.passbolt.mobile.android.rules.lazyActivityScenarioRule
import com.passbolt.mobile.android.testtags.composetags.Setup.APPS_IMAGE
import com.passbolt.mobile.android.testtags.composetags.Setup.HELP_BUTTON
import com.passbolt.mobile.android.testtags.composetags.Setup.LOGO_IMAGE
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.KoinTest
import com.passbolt.mobile.android.core.localization.R as LocalizationR

@RunWith(AndroidJUnit4::class)
@MediumTest
class WelcomeScreenTest : KoinTest {
    @get:Rule
    val activityRule =
        lazyActivityScenarioRule<SetUpActivity>(
            koinOverrideModules = listOf(instrumentationTestsModule),
        )

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    //    https://passbolt.testrail.io/index.php?/cases/view/2332
    @Test
    fun asAMobileUserICanSeeTheWelcomeScreenWhenIOpenTheApplicationAndNoAccountIsSetup() {
        //    Given     that the application is not configured for any users
        //    When      I launch the application
        //    Then      I see a welcome screen
        composeTestRule.apply {
            //    And       I see a Passbolt logo
            onNodeWithTag(LOGO_IMAGE).assertIsDisplayed()
            //    And       I see a welcome illustration
            onNodeWithTag(APPS_IMAGE).assertIsDisplayed()
            //    And       I see a welcome message
            onNodeWithText(getString(LocalizationR.string.welcome_title)).assertIsDisplayed()
            onNodeWithText(getString(LocalizationR.string.welcome_body)).assertIsDisplayed()
            //    And       I see a "connect to an existing account" primary action
            onNodeWithText(getString(LocalizationR.string.welcome_connect_to_existing_account)).assertIsDisplayed()
            //    And       I see an "I don't have an account" secondary action
            onNodeWithText(getString(LocalizationR.string.welcome_no_account)).assertIsDisplayed()
            //    And       I see a "Help" side action
            onNodeWithTag(HELP_BUTTON).assertIsDisplayed()
        }
    }

    //    https://passbolt.testrail.io/index.php?/cases/view/2333
    @Test
    fun asAMobileUserICanSeeAnExplanationWhyICannotCreateAnAccountOnTheMobileApp() {
        composeTestRule.apply {
            //      Given   the welcome screen is displayed
            //      When    the user click on the "I don't have an account" button
            onNodeWithText(getString(LocalizationR.string.welcome_no_account)).performClick()
            //      Then    a dialog explaining why I can't create an account is presented
            onNodeWithText(getString(LocalizationR.string.welcome_create_account_dialog_title)).assertIsDisplayed()
            //      And     the message says "I need to create an account first using the web"
            onNodeWithText(getString(LocalizationR.string.welcome_create_account_dialog_message)).assertIsDisplayed()
            //      And     a "Got it" button to close the dialog is presented
            //      And     a "Got it" button is clickable
            onNodeWithText(getString(LocalizationR.string.got_it)).assertIsDisplayed()
        }
    }

    //    https://passbolt.testrail.io/index.php?/cases/view/6190
    @Test
    fun asAMobileUserICanGetHelpBeforeTheQrCodeScanningProcess() {
        composeTestRule.apply {
            //    Given   the user is on the "Welcome" screen
            //    When    the user clicks on the "information" icon on the top
            onNodeWithTag(HELP_BUTTON).performClick()
            //    Then    a modal with help options is presented
            onNodeWithText(getString(LocalizationR.string.help_menu_help)).assertIsDisplayed()
            //    And     an "Enable debug logs" switch is available (on Android only)
            onNodeWithText(getString(LocalizationR.string.help_menu_enable_debug_logs)).assertIsDisplayed()
            //    And     an "Access the logs" button is available
            onNodeWithText(getString(LocalizationR.string.help_menu_access_logs)).assertIsDisplayed()
            //    And     a "Visit help site" button is available
            onNodeWithText(getString(LocalizationR.string.help_menu_visit_help_website)).assertIsDisplayed()
        }
    }

    //    https://passbolt.testrail.io/index.php?/cases/view/6191
    //    NOTE: setup Chrome first (accept dialogs, pop ups, etc)
    @Test
    fun asAMobileUserICanOpenHelpWebpageBeforeTheQrCodeScanningProcess() {
        Intents.init()

        try {
            composeTestRule.apply {
                //        Given   the user is on the "Help" modal
                onNodeWithTag(HELP_BUTTON).performClick()
                //        When    the user clicks on the "Visit help site" button
                onNodeWithText(getString(LocalizationR.string.help_menu_visit_help_website)).performClick()
                //        Then    a webpage with help is presented
                intended(
                    allOf(
                        hasAction(Intent.ACTION_VIEW),
                        hasData(getString(LocalizationR.string.help_website)),
                    ),
                )
            }
        } finally {
            Intents.release()
        }
    }

    //    https://passbolt.testrail.io/index.php?/cases/view/2334
    @Test
    fun asAMobileUserICanSeeAnExplanationOnHowToConnectAnExistingAccount() {
        composeTestRule.apply {
            //        Given   the welcome screen is displayed
            //        When    the user clicks on "connect to an existing account"
            onNodeWithText(getString(LocalizationR.string.welcome_connect_to_existing_account)).performClick()
            //        Then    the "Transfer account details" explanation screen is presented
            onNodeWithText(getString(LocalizationR.string.transfer_account_title)).assertIsDisplayed()
            //        And     the screen has an arrow button on the top left to go back to the welcome screen
            onNodeWithTag(BackNavigationIcon.TestTags.ICON, useUnmergedTree = true).assertIsDisplayed()
            //        And     it has an explanation of the different steps of the setup process
            onNodeWithText(getString(LocalizationR.string.transfer_details_header)).assertIsDisplayed()
            //        And     it has a "Scan QR codes" primary action button
            onNodeWithText(getString(LocalizationR.string.transfer_details_scan_button)).assertIsDisplayed()
        }
    }
}
