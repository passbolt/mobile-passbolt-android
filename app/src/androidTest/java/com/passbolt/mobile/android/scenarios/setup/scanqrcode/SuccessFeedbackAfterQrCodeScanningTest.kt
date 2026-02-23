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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.passbolt.mobile.android.accountinit.AccountDataCleaner
import com.passbolt.mobile.android.feature.setup.SetUpActivity
import com.passbolt.mobile.android.helpers.getString
import com.passbolt.mobile.android.instrumentationTestsModule
import com.passbolt.mobile.android.intents.ManagedAccountIntentCreator
import com.passbolt.mobile.android.rules.lazyActivityScenarioRule
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
class SuccessFeedbackAfterQrCodeScanningTest : KoinTest {
    @get:Rule
    val startActivityRule =
        lazyActivityScenarioRule<SetUpActivity>(
            koinOverrideModules = listOf(instrumentationTestsModule),
            intentSupplier = {
                managedAccountIntentCreator.createIntent(
                    InstrumentationRegistry.getInstrumentation().targetContext,
                )
            },
        )

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val managedAccountIntentCreator: ManagedAccountIntentCreator by inject()
    private val accountDataCleaner: AccountDataCleaner by inject()

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

    //    https://passbolt.testrail.io/index.php?/cases/view/2346
    @Test
    fun asAMobileUserIShouldSeeASuccessFeedbackAtTheEndOfTheQrCodeScanning() {
        // Given    the user is on the "Scanning QR codes" screen
        // When     the user scans the last QR code
        // Then     a successful feedback illustration and message appears
        composeTestRule.apply {
            waitForIdle()
            onNodeWithText(getString(LocalizationR.string.scan_qr_summary_success_title)).assertIsDisplayed()
            // And      a "Continue" button is available
            onNodeWithText(getString(LocalizationR.string.continue_label)).assertIsDisplayed()
        }
    }
}
