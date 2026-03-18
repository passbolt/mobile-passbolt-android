/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2023-2025 Passbolt SA
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

package com.passbolt.mobile.android.scenarios.settings.accounts.transfer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.passbolt.mobile.android.core.idlingresource.ResourcesFullRefreshIdlingResource
import com.passbolt.mobile.android.core.idlingresource.SignInIdlingResource
import com.passbolt.mobile.android.core.idlingresource.TransferAccountIdlingResource
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
import com.passbolt.mobile.android.testtags.composetags.Auth
import com.passbolt.mobile.android.testtags.composetags.BottomNav
import com.passbolt.mobile.android.testtags.composetags.TransferAccount
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.component.inject
import org.koin.test.KoinTest
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@RunWith(AndroidJUnit4::class)
@LargeTest
class TransferTest : KoinTest {
    @get:Rule(order = 0)
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

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val managedAccountIntentCreator: ManagedAccountIntentCreator by inject()

    @get:Rule
    val idlingResourceRule =
        let {
            val signInIdlingResource: SignInIdlingResource by inject()
            val transferAccountIdlingResource: TransferAccountIdlingResource by inject()
            val resourcesFullRefreshIdlingResource: ResourcesFullRefreshIdlingResource by inject()

            IdlingResourceRule(
                arrayOf(
                    signInIdlingResource,
                    transferAccountIdlingResource,
                    resourcesFullRefreshIdlingResource,
                ),
            )
        }

    /**
     * **#MOBILE_USER_ON_TRANSFER_ONBOARDING:**
     * Given    I am a mobile user with the application installed
     * And      I am logged in
     * And      I am on the Transfer account onboarding screen
     */
    @Before
    fun setup() {
        composeTestRule.apply {
            signIn(managedAccountIntentCreator.getPassphrase())
            onNodeWithTag(BottomNav.SETTINGS_TAB).performClick()
            onNodeWithText(getString(LocalizationR.string.settings_accounts)).performClick()
            onNodeWithText(getString(LocalizationR.string.settings_accounts_transfer_account)).performClick()
        }
    }

    /**
     * **I can see an explanation on how to transfer an existing account**
     *
     * TestRail: {@link https://passbolt.testrail.io/index.php?/cases/view/8147}
     *
     * Given    I'm logged in user on transfer onboarding screen (handled in setup)
     * Then     the "Transfer account details" explanation screen is presented with a corresponding title
     * And      a "Start transfer" primary action button is visible
     */
    @Test
    fun asAUserICanSeeAnExplanationOnHowToTransferAnExistingAccount() {
        composeTestRule.apply {
            onNodeWithText(getString(LocalizationR.string.transfer_account_title))
                .assertIsDisplayed()

            onNodeWithTag(TransferAccount.START_TRANSFER_BUTTON)
                .assertIsDisplayed()
        }
    }

    /**
     * **I should see "Transferring your account details" screen**
     *
     * TestRail: {@link https://passbolt.testrail.io/index.php?/cases/view/8151}
     *
     * Given    I'm on "Transfer account details" onboarding screen
     * When     I click "Start transfer"
     * Then     I see a "Transferring your account details" page with corresponding title
     * And      I see a first QR code
     * And      I see a "Cancel transfer" primary action button
     */
    @Test
    fun asAUserIShouldSeeTransferringYourAccountDetailsScreen() {
        startTransfer()
        composeTestRule.apply {
            onNodeWithText(getString(LocalizationR.string.transfer_account_title))
                .assertIsDisplayed()

            onNodeWithTag(TransferAccount.QR_CODE)
                .assertIsDisplayed()

            onNodeWithText(getString(LocalizationR.string.transfer_account_cancel_button))
                .assertIsDisplayed()
        }
    }

    /**
     * **I need to confirm to stop the QR code presentation**
     *
     * TestRail: {@link https://passbolt.testrail.io/index.php?/cases/view/8153}
     *
     * Given    I'm on a "Transferring your account details" page
     * When     I click "Cancel Transfer" button
     * Then     I see a confirmation dialog
     * And      I see some explanation
     * And      I see a "Cancel" and "Stop transfer" options
     */
    @Test
    fun asAUserINeedToConfirmToStopTheQrCodePresentation() {
        openStopTransferPrompt()

        composeTestRule.apply {
            onNodeWithText(getString(LocalizationR.string.are_you_sure))
                .assertIsDisplayed()

            onNodeWithText(getString(LocalizationR.string.transfer_account_stop_confirmation_dialog_message))
                .assertIsDisplayed()

            onNodeWithText(getString(LocalizationR.string.cancel))
                .assertIsDisplayed()

            onNodeWithText(getString(LocalizationR.string.transfer_account_stop_button))
                .assertIsDisplayed()
        }
    }

    /**
     * **I can stop the QR code presentation**
     *
     * TestRail: {@link https://passbolt.testrail.io/index.php?/cases/view/8154}
     *
     * Given    I'm on a "Transferring your account details" page
     * And      I see a prompt with "Cancel"
     * And      I see action buttons
     * When     I click on the "Stop transfer" button
     * Then     the prompt is dismissed
     * And      the process is stopped
     * And      I see "Failed feedback" screen
     * And      I see "Transfer cancelled" explanation
     */
    @Test
    fun asAUserICanStopTheQrCodePresentation() {
        openStopTransferPrompt()

        composeTestRule.apply {
            onNodeWithText(getString(LocalizationR.string.transfer_account_stop_button))
                .performClick()

            onNodeWithText(getString(LocalizationR.string.transfer_account_stop_confirmation_dialog_message))
                .assertDoesNotExist()

            onNodeWithTag(TransferAccount.QR_CODE)
                .assertDoesNotExist()

            onNodeWithTag(TransferAccount.SUMMARY_SCREEN)
                .assertIsDisplayed()

            onNodeWithText(getString(LocalizationR.string.transfer_account_summary_cancelled))
                .assertIsDisplayed()
        }
    }

    /**
     * **I should see a failed feedback in case of error during QR codes sequence**
     *
     * TestRail: {@link https://passbolt.testrail.io/index.php?/cases/view/8156}
     *
     * Given    I'm on a "Transferring your account details" page
     * When     there is an error during the transfer process
     * Then     I see an unsuccessful screen with a corresponding title
     * And      I see an unsuccessful illustration
     * And      I see a "Go back to my account"
     */
    @Test
    fun asAUserIShouldSeeAFailedFeedbackInCaseOfErrorDuringQrCodesSequence() {
        openStopTransferPrompt()

        composeTestRule.apply {
            onNodeWithText(getString(LocalizationR.string.transfer_account_stop_button))
                .performClick()

            onNodeWithText(getString(LocalizationR.string.transfer_account_summary_cancelled))
                .assertIsDisplayed()

            onNodeWithTag(CoreUiR.drawable.ic_failed.toString())
                .assertIsDisplayed()

            onNodeWithText(getString(LocalizationR.string.transfer_account_summary_go_back))
                .assertIsDisplayed()
        }
    }

    /**
     * **I could go back from a failed feedback in case of error during QR codes sequence**
     *
     * TestRail: {@link https://passbolt.testrail.io/index.php?/cases/view/C8157}
     *
     * Given    I'm on a "Transferring your account details" page
     * And      there was an error during the transfer process
     * And      I see an unsuccessful screen
     * When     I click a "Go back to my account"
     * Then     I see the Account details page
     */
    @Test
    fun asAUserICouldGoBackFromAFailedFeedbackInCaseOfErrorDuringQrCodesSequence() {
        openStopTransferPrompt()

        composeTestRule.apply {
            onNodeWithText(getString(LocalizationR.string.transfer_account_stop_button))
                .performClick()

            onNodeWithText(getString(LocalizationR.string.transfer_account_summary_cancelled))
                .assertIsDisplayed()

            onNodeWithText(getString(LocalizationR.string.transfer_account_summary_go_back))
                .performClick()

            onNodeWithText(getString(LocalizationR.string.settings_accounts))
                .assertIsDisplayed()
        }
    }

    private fun startTransfer() {
        composeTestRule.apply {
            onNodeWithTag(TransferAccount.START_TRANSFER_BUTTON).performClick()
            // re-authentication before start transfer
            onNodeWithTag(Auth.PASSPHRASE_INPUT).performTextReplacement(managedAccountIntentCreator.getPassphrase())
            onNodeWithTag(Auth.SIGN_IN_BUTTON).performClick()
        }
    }

    private fun openStopTransferPrompt() {
        startTransfer()
        composeTestRule.onNodeWithText(getString(LocalizationR.string.transfer_account_cancel_button)).performClick()
    }
}
