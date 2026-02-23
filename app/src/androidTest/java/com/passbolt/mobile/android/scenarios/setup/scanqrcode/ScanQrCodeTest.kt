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

package com.passbolt.mobile.android.scenarios.setup.scanqrcode

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.rule.GrantPermissionRule
import com.passbolt.mobile.android.accountinit.AccountDataCleaner
import com.passbolt.mobile.android.feature.setup.SetUpActivity
import com.passbolt.mobile.android.helpers.getString
import com.passbolt.mobile.android.instrumentationTestsModule
import com.passbolt.mobile.android.rules.lazyActivityScenarioRule
import org.hamcrest.Matchers.allOf
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
class ScanQrCodeTest : KoinTest {
    @get:Rule
    val activityRule =
        lazyActivityScenarioRule<SetUpActivity>(
            koinOverrideModules = listOf(instrumentationTestsModule),
        )

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

    private val accountDataCleaner: AccountDataCleaner by inject()

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Before
    fun setup() {
        accountDataCleaner.clearAccountData()

        composeTestRule.apply {
            onNodeWithText(getString(LocalizationR.string.welcome_connect_to_existing_account)).performClick()
            onNodeWithText(getString(LocalizationR.string.transfer_details_scan_button)).performClick()
        }
    }

    @After
    fun tearDown() {
        accountDataCleaner.clearAccountData()
    }

    //    https://passbolt.testrail.io/index.php?/cases/view/2342
    @Test
    fun asAMobileUserICanGetHelpDuringTheQrCodeScanningProcess() {
        composeTestRule.apply {
            //        Given   the user is on the "Scanning QR codes" screen
            //        When    the user clicks on the "information" icon next to the progress bar
            onNode(hasContentDescription(getString(LocalizationR.string.help_button_description))).performClick()
            //        Then    a modal with help options is presented
            onNodeWithText(getString(LocalizationR.string.help_menu_help)).assertIsDisplayed()
            //        And     a "Why scanning QR codes?" button is available
            onNodeWithText(getString(LocalizationR.string.help_menu_why_scan_codes)).assertIsDisplayed()
            //        And     an "Enable debug logs" switch is available (on Android only)
            onNodeWithText(getString(LocalizationR.string.help_menu_enable_debug_logs)).assertIsDisplayed()
            //        And     an "Access the logs" button is available
            onNodeWithText(getString(LocalizationR.string.help_menu_access_logs)).assertIsDisplayed()
            //        And     a "Visit help site" button is available
            onNodeWithText(getString(LocalizationR.string.help_menu_visit_help_website)).assertIsDisplayed()
        }
    }

    //    https://passbolt.testrail.io/index.php?/cases/view/6192
    @Test
    fun asAMobileUserICanOpenHelpWebpageDuringTheQrCodeScanningProcess() {
        Intents.init()

        try {
            composeTestRule.apply {
                //        Given   the user is on the "Help" modal
                onNode(hasContentDescription(getString(LocalizationR.string.help_button_description))).performClick()
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

    //    https://passbolt.testrail.io/index.php?/cases/view/6193
    @Test
    fun asAMobileUserICanSeeAnExplanationWhyScanningQrCodes() {
        composeTestRule.apply {
            //      Given   the user is on the "Help" modal
            onNode(hasContentDescription(getString(LocalizationR.string.help_button_description))).performClick()
            //      When    the user clicks on the "Why scanning QR codes" button
            onNodeWithText(getString(LocalizationR.string.help_menu_why_scan_codes)).performClick()
            //      Then    a dialog explaining the process of scanning QR codes is presented
            onNodeWithText(getString(LocalizationR.string.scan_qr_exit_information_dialog_title)).assertIsDisplayed()
            //      And     the message says how the process would look like
            onNodeWithText(getString(LocalizationR.string.scan_qr_exit_information_dialog_message)).assertIsDisplayed()
            //      And     a "Got it" button to close the dialog is presented
            //      And     a "Got it" button is clickable
            onNodeWithText(getString(LocalizationR.string.got_it)).assertIsDisplayed()
        }
    }
}
