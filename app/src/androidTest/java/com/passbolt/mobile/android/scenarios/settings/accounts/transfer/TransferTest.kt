/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2023-2024 Passbolt SA
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
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.passbolt.mobile.android.core.idlingresource.SignInIdlingResource
import com.passbolt.mobile.android.core.idlingresource.TransferAccountIdlingResource
import com.passbolt.mobile.android.core.localization.R.string.settings_accounts
import com.passbolt.mobile.android.core.localization.R.string.settings_accounts_transfer_account
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.feature.authentication.AuthenticationMainActivity
import com.passbolt.mobile.android.feature.settings.R.id.settingsNavCompose
import com.passbolt.mobile.android.helpers.getString
import com.passbolt.mobile.android.helpers.signIn
import com.passbolt.mobile.android.instrumentationTestsModule
import com.passbolt.mobile.android.intents.ManagedAccountIntentCreator
import com.passbolt.mobile.android.matchers.assertHasBitmapContent
import com.passbolt.mobile.android.rules.IdlingResourceRule
import com.passbolt.mobile.android.rules.lazyActivitySetupScenarioRule
import com.passbolt.mobile.android.scenarios.setup.configurebiometric.biometricSetupUnavailableModuleTests
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.component.inject
import org.koin.test.KoinTest
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR
import com.passbolt.mobile.android.feature.authentication.R as AuthenticationR

@RunWith(AndroidJUnit4::class)
@LargeTest
class TransferTest : KoinTest {
    @get:Rule
    val startUpActivityRule =
        lazyActivitySetupScenarioRule<AuthenticationMainActivity>(
            koinOverrideModules =
                listOf(
                    instrumentationTestsModule,
                    biometricSetupUnavailableModuleTests,
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

    private val managedAccountIntentCreator: ManagedAccountIntentCreator by inject()

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @get:Rule
    val idlingResourceRule =
        let {
            val signInIdlingResource: SignInIdlingResource by inject()
            val transferAccountIdlingResource: TransferAccountIdlingResource by inject()

            IdlingResourceRule(
                arrayOf(
                    signInIdlingResource,
                    transferAccountIdlingResource,
                ),
            )
        }

    /**
     * **#MOBILE_USER_ON_SETTINGS_PAGE:**
     * Given    I am a mobile user with the application installed
     * And      I am logged in
     * And      I am on Passbolt PRO/CE/Cloud
     */
    @Before
    fun setup() {
        composeTestRule.signIn(managedAccountIntentCreator.getPassphrase())
        onView(withId(settingsNavCompose)).perform(click())
        composeTestRule.apply {
            waitForIdle()
            onNodeWithText(getString(settings_accounts)).performClick()
            waitForIdle()
            onNodeWithText(getString(settings_accounts_transfer_account)).performClick()
            waitForIdle()
        }
    }

    /**
     * **I can see an explanation on how to transfer an existing account**
     *
     * TestRail: {@link https://passbolt.testrail.io/index.php?/cases/view/8147}
     *
     * Given    I’m logged in user on <page> screen (handled in setup)
     * Then     the “Transfer account details” explanation screen is presented with a corresponding title
     * And      a "Start transfer" primary action button is visible
     */
    @Test
    fun asAUserICanSeeAnExplanationOnHowToTransferAnExistingAccount() {
        composeTestRule.apply {
            waitForIdle()

            onNodeWithText(getString(LocalizationR.string.transfer_account_title))
                .assertExists()
                .assertIsDisplayed()

            onNodeWithTag("StartTransferButton")
                .assertExists()
                .assertIsDisplayed()
        }
    }

//    // https://passbolt.testrail.io/index.php?/cases/view/8150
//    @Test
//    @FlakyTest(detail = "On Huawei Mate 30 Pro it is not working when iterated in a group")
//    fun asAMobileUserIShouldSeeEnterYourPassphraseScreenWhenTransferStarted() {
//        //      Given   I’m on mobile without any biometry enabled for the Passbolt app
//        //      And     I’m logged in user on “Transfer account details” screen
//        //      When    I click “Start Transfer”
//        onView(withId(com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.R.id.startTransferButton)).perform(click())
//        //      Then    I see a "Enter your passphrase" page
//        onView(withText(LocalizationR.string.auth_enter_passphrase)).check(matches(isDisplayed()))
//        //      And     I see a back arrow button
//        onView(ViewMatchers.isAssignableFrom(Toolbar::class.java))
//            .check(CastedViewAssertion<Toolbar> { it.navigationIcon != null })
//        onView(withId(com.passbolt.mobile.android.feature.accountdetails.R.id.avatarImage)).check(matches(isDisplayed()))
//        //      And     I see my current user's name
//        onView(withId(com.passbolt.mobile.android.feature.settings.R.id.nameLabel)).check(matches(isDisplayed()))
//        val firstName = managedAccountIntentCreator.getFirstName()
//        val lastName = managedAccountIntentCreator.getLastName()
//        onView(withText(AccountModelMapper.defaultLabel(firstName, lastName))).check(matches(isDisplayed()))
//        //      And     I see my current user's email
//        onView(withId(com.passbolt.mobile.android.feature.accountdetails.R.id.emailLabel)).check(matches(isDisplayed()))
//        val email = managedAccountIntentCreator.getUsername()
//        onView(withText(email)).check(matches(isDisplayed()))
//        //      And     I see the url of the server
//        onView(withId(com.passbolt.mobile.android.feature.authentication.R.id.domainLabel)).check(matches(isDisplayed()))
//        val url = managedAccountIntentCreator.getDomain()
//        onView(withText(url)).check(matches(isDisplayed()))
//        //      And     I see a passphrase input field
//        onView(withId(CoreUiR.id.input)).check(matches(isDisplayed()))
//        //      And     I see an eye icon to toggle passphrase visibility
//        onView(withId(MaterialR.id.text_input_end_icon)).check(matches(isDisplayed()))
//        //      And     I see a “Confirm passphrase” primary action button
//        onView(withId(com.passbolt.mobile.android.feature.authentication.R.id.authButton)).check(matches(isDisplayed()))
//    }
//

    /**
     * **I should see "Transferring your account details" screen**
     *
     * TestRail: {@link https://passbolt.testrail.io/index.php?/cases/view/8151}
     *
     * Given    I’m on “Transfer account details” process
     * And      I am on the "Enter your passphrase" page
     * When     I click “Confirm passphrase” or provide valid biometric authentication
     * Then     I see a “Transferring your account details” page with corresponding title
     * And      I see a first QR code
     * And      I see a “Cancel transfer” primary action button
     */
    @Test
    fun asAUserIShouldSeeTransferringYourAccountDetailsScreen() {
        composeTestRule.onNodeWithTag("StartTransferButton").performClick()

        onView(withId(CoreUiR.id.input)).perform(typeText(managedAccountIntentCreator.getPassphrase()))
        onView(withId(AuthenticationR.id.authButton)).perform(scrollTo(), click())

        composeTestRule.apply {
            waitForIdle()

            onNodeWithText(getString(LocalizationR.string.transfer_account_title))
                .assertIsDisplayed()

            onNodeWithTag("QrCode")
                .assertHasBitmapContent()

            onNodeWithText(getString(LocalizationR.string.transfer_account_cancel_button))
                .assertIsDisplayed()
        }
    }

    /**
     * **I need to confirm to stop the QR code presentation**
     *
     * TestRail: {@link https://passbolt.testrail.io/index.php?/cases/view/8153}
     *
     * Given    I’m on a “Transferring your account details” page
     * When     I click “Cancel Transfer” button
     * Then     I see a confirmation dialog
     * And      I see some explanation
     * And      I see a “Cancel” and “Stop transfer” options
     */
    @Test
    fun asAUserINeedToConfirmToStopTheQrCodePresentation() {
        composeTestRule.onNodeWithTag("StartTransferButton").performClick()
        onView(withId(CoreUiR.id.input)).perform(typeText(managedAccountIntentCreator.getPassphrase()))
        onView(withId(AuthenticationR.id.authButton)).perform(scrollTo(), click())

        composeTestRule.apply {
            waitForIdle()

            onNodeWithText(getString(LocalizationR.string.transfer_account_cancel_button))
                .performClick()

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
     * Given    I’m on a “Transferring your account details” page
     * And      I see a prompt with “Cancel”
     * And      I see action buttons
     * When     I click on the “Stop transfer” button
     * Then     the prompt is dismissed
     * And      the process is stopped
     * And      I see “Failed feedback” screen
     * And      I see “Transfer cancelled” explanation
     */
    @Test
    fun asAUserICanStopTheQrCodePresentation() {
        openStopTransferPrompt()

        composeTestRule.apply {
            waitForIdle()

            onNodeWithText(getString(LocalizationR.string.transfer_account_stop_button))
                .performClick()

            onNodeWithText(getString(LocalizationR.string.transfer_account_stop_confirmation_dialog_message))
                .assertDoesNotExist()

            onNodeWithTag("QrCode")
                .assertDoesNotExist()

            onNodeWithTag("TransferAccountSummaryScreen")
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
     * Given    I’m on a “Transferring your account details” page
     * When     there is an error during the transfer process
     * Then     I see an unsuccessful “Something went wrong!” screen with a corresponding title
     * And      I see an unsuccessful illustration
     * And      I see an error message
     * And      I see a “Go back to my account”
     */
    @Test
    fun asAUserIShouldSeeAFailedFeedbackInCaseOfErrorDuringQrCodesSequence() {
        openStopTransferPrompt()

        composeTestRule.apply {
            waitForIdle()

            onNodeWithText(getString(LocalizationR.string.transfer_account_stop_button))
                .performClick()

            onNodeWithText(getString(LocalizationR.string.transfer_account_summary_cancelled))
                .assertIsDisplayed()

            onNodeWithTag(CoreUiR.drawable.ic_failed.toString())
                .assertIsDisplayed()

            // there is cancel process tested here now so there is no msg

            onNodeWithText(getString(LocalizationR.string.transfer_account_summary_cancelled))
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
     * Given    I’m on a “Transferring your account details” page
     * And      there was an error during the transfer process
     * And      I see an unsuccessful “Something went wrong!” screen
     * When     I click a “Go back to my account”
     * Then     I see the Account details page
     */
    @Test
    fun asAUserICouldGoBackFromAFailedFeedbackInCaseOfErrorDuringQrCodesSequence() {
        openStopTransferPrompt()

        composeTestRule.apply {
            waitForIdle()

            onNodeWithText(getString(LocalizationR.string.transfer_account_stop_button))
                .performClick()

            onNodeWithText(getString(LocalizationR.string.transfer_account_summary_cancelled))
                .assertIsDisplayed()

            onNodeWithText(getString(LocalizationR.string.transfer_account_summary_go_back))
                .performClick()

            onNodeWithText(getString(settings_accounts))
                .assertIsDisplayed()
        }
    }

    private fun openStopTransferPrompt() {
        composeTestRule.onNodeWithTag("StartTransferButton").performClick()
        onView(withId(CoreUiR.id.input)).perform(typeText(managedAccountIntentCreator.getPassphrase()))
        onView(withId(AuthenticationR.id.authButton)).perform(scrollTo(), click())
        composeTestRule.onNodeWithText(getString(LocalizationR.string.transfer_account_cancel_button)).performClick()
    }
}
