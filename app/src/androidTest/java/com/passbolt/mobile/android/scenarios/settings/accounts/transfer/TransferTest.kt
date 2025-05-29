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

import androidx.appcompat.widget.Toolbar
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.passbolt.mobile.android.commontest.viewassertions.CastedViewAssertion
import com.passbolt.mobile.android.core.idlingresource.SignInIdlingResource
import com.passbolt.mobile.android.core.idlingresource.TransferAccountIdlingResource
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.feature.authentication.AuthenticationMainActivity
import com.passbolt.mobile.android.helpers.signIn
import com.passbolt.mobile.android.instrumentationTestsModule
import com.passbolt.mobile.android.intents.ManagedAccountIntentCreator
import com.passbolt.mobile.android.mappers.AccountModelMapper
import com.passbolt.mobile.android.matchers.hasDrawable
import com.passbolt.mobile.android.matchers.withImageViewContainingAnyImage
import com.passbolt.mobile.android.rules.IdlingResourceRule
import com.passbolt.mobile.android.rules.lazyActivitySetupScenarioRule
import com.passbolt.mobile.android.scenarios.setup.configurebiometric.biometricSetupUnavailableModuleTests
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.component.inject
import org.koin.test.KoinTest
import kotlin.test.BeforeTest
import com.google.android.material.R as MaterialR
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

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

    @BeforeTest
    fun setup() {
        //    #MOBILE_USER_ON_SETTINGS_PAGE:
        //    Given	I am a mobile user with the application installed
        //    And	I am logged in
        //    And 	I am on Passbolt PRO/CE/Cloud
        signIn(managedAccountIntentCreator.getPassphrase())
        onView(withId(com.passbolt.mobile.android.feature.settings.R.id.settingsNav)).perform(click())
        onView(withId(com.passbolt.mobile.android.feature.settings.R.id.accountsSettings)).perform(click())
        onView(withId(com.passbolt.mobile.android.feature.settings.R.id.transferAccountSetting)).perform(click())
    }

    // https://passbolt.testrail.io/index.php?/cases/view/8147
    @Test
    fun asAUserICanSeeAnExplanationOnHowToTransferAnExistingAccount() {
        //      Given   I’m logged in user on <page> screen
        //      When    I click “Transfer account to another device”
        //      Then    the “Transfer account details” explanation screen is presented with a corresponding title
        onView(withText(LocalizationR.string.transfer_account_title)).check(matches(isDisplayed()))
        //      And     the screen has an arrow button on the top left to go back to the previous screen
        onView(ViewMatchers.isAssignableFrom(Toolbar::class.java))
            .check(CastedViewAssertion<Toolbar> { it.navigationIcon != null })
        //      And     it has an explanation of the different steps of the transfer process
        onView(withId(com.passbolt.mobile.android.feature.setup.R.id.steps)).check(matches(isDisplayed()))
        //      And     an illustration giving some context about the process
        onView(withId(com.passbolt.mobile.android.feature.setup.R.id.qrCode)).check(matches(isDisplayed()))
        //      And     a "Start transfer" primary action button
        onView(
            withId(com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.R.id.startTransferButton),
        ).check(matches(isDisplayed()))
        //
        //          | page            |
        //          | Account details |
        //          | Accounts        |
    }

    // https://passbolt.testrail.io/index.php?/cases/view/8150
    @Test
    @FlakyTest(detail = "On Huawei Mate 30 Pro it is not working when iterated in a group")
    fun asAMobileUserIShouldSeeEnterYourPassphraseScreenWhenTransferStarted() {
        //      Given   I’m on mobile without any biometry enabled for the Passbolt app
        //      And     I’m logged in user on “Transfer account details” screen
        //      When    I click “Start Transfer”
        onView(withId(com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.R.id.startTransferButton)).perform(click())
        //      Then    I see a "Enter your passphrase" page
        onView(withText(LocalizationR.string.auth_enter_passphrase)).check(matches(isDisplayed()))
        //      And     I see a back arrow button
        onView(ViewMatchers.isAssignableFrom(Toolbar::class.java))
            .check(CastedViewAssertion<Toolbar> { it.navigationIcon != null })
        onView(withId(com.passbolt.mobile.android.feature.accountdetails.R.id.avatarImage)).check(matches(isDisplayed()))
        //      And     I see my current user's name
        onView(withId(com.passbolt.mobile.android.feature.settings.R.id.nameLabel)).check(matches(isDisplayed()))
        val firstName = managedAccountIntentCreator.getFirstName()
        val lastName = managedAccountIntentCreator.getLastName()
        onView(withText(AccountModelMapper.defaultLabel(firstName, lastName))).check(matches(isDisplayed()))
        //      And     I see my current user's email
        onView(withId(com.passbolt.mobile.android.feature.accountdetails.R.id.emailLabel)).check(matches(isDisplayed()))
        val email = managedAccountIntentCreator.getUsername()
        onView(withText(email)).check(matches(isDisplayed()))
        //      And     I see the url of the server
        onView(withId(com.passbolt.mobile.android.feature.authentication.R.id.domainLabel)).check(matches(isDisplayed()))
        val url = managedAccountIntentCreator.getDomain()
        onView(withText(url)).check(matches(isDisplayed()))
        //      And     I see a passphrase input field
        onView(withId(CoreUiR.id.input)).check(matches(isDisplayed()))
        //      And     I see an eye icon to toggle passphrase visibility
        onView(withId(MaterialR.id.text_input_end_icon)).check(matches(isDisplayed()))
        //      And     I see a “Confirm passphrase” primary action button
        onView(withId(com.passbolt.mobile.android.feature.authentication.R.id.authButton)).check(matches(isDisplayed()))
    }

    // https://passbolt.testrail.io/index.php?/cases/view/8151
    @Test
    fun asAUserIShouldSeeTransferringYourAccountDetailsScreen() {
        //      Given    I’m on “Transfer account details” process
        //      And      I am on the "Enter your passphrase" page
        onView(withId(com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.R.id.startTransferButton)).perform(click())
        //      When     I click “Confirm passphrase” or provide valid biometric authentication
        onView(withId(CoreUiR.id.input)).perform(typeText(managedAccountIntentCreator.getPassphrase()))
        onView(withId(com.passbolt.mobile.android.feature.authentication.R.id.authButton)).perform(scrollTo(), click())
        //      Then     I see a “Transferring your account details” page with corresponding title
        onView(withText(LocalizationR.string.transfer_account_title)).check(matches(isDisplayed()))
        //      And      I see a first QR code
        // check if any image is loaded into the QR code image view
        onView(withId(com.passbolt.mobile.android.feature.setup.R.id.qrCode)).check(
            matches(
                withImageViewContainingAnyImage(),
            ),
        )
        //      And      I see a “Cancel transfer” primary action button
        onView(
            withId(com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.R.id.cancelTransferButton),
        ).check(matches(isDisplayed()))
    }

    // https://passbolt.testrail.io/index.php?/cases/view/8153
    @Test
    fun asAUserINeedToConfirmToStopTheQrCodePresentation() {
        //      Given   I’m on a “Transferring your account details” page
        onView(withId(com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.R.id.startTransferButton)).perform(click())
        onView(withId(CoreUiR.id.input)).perform(typeText(managedAccountIntentCreator.getPassphrase()))
        onView(withId(com.passbolt.mobile.android.feature.authentication.R.id.authButton)).perform(scrollTo(), click())
        //      When    I click “Cancel Transfer” button
        onView(withId(com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.R.id.cancelTransferButton)).perform(click())
        //      Then    I see a confirmation dialog
        onView(withText(LocalizationR.string.scan_qr_exit_confirmation_dialog_message)).check(matches(isDisplayed()))
        //      And     I see a message titled “Are you sure?”
        onView(withId(androidx.appcompat.R.id.alertTitle)).check(matches(isDisplayed()))
        //      And     I see some explanation
        onView(withId(android.R.id.message)).check(matches(isDisplayed()))
        //      And     I see a “Cancel” and “Stop transfer” options
        onView(withText(LocalizationR.string.cancel))
            .check(matches(isDisplayed()))
            .check(matches(ViewMatchers.isClickable()))
        onView(withText(LocalizationR.string.transfer_account_stop_button))
            .check(matches(isDisplayed()))
            .check(matches(ViewMatchers.isClickable()))
    }

    // https://passbolt.testrail.io/index.php?/cases/view/8154
    @Test
    fun asAUserICanStopTheQrCodePresentation() {
        //       Given   I’m on a “Transferring your account details” page
        //       And     I see a prompt with “Cancel”                   // ** tested before
        //       And     I see action buttons                           // ** tested before
        openStopTransferPrompt()
        //       When    I click on the “Stop transfer” button
        onView(withText(LocalizationR.string.transfer_account_stop_button)).perform(click())
        //       Then    the prompt is dismissed
        onView(withId(android.R.id.message)).check(doesNotExist())
        //       And     the process is stopped
        onView(withId(com.passbolt.mobile.android.feature.setup.R.id.qrCode)).check(doesNotExist())
        //       And     I see “Failed feedback” screen
        onView(withId(com.passbolt.mobile.android.feature.setup.R.id.resultView)).check(matches(isDisplayed()))
        //       And     I see “Transfer cancelled” explanation
        onView(withText(LocalizationR.string.transfer_account_summary_cancelled)).check(matches(isDisplayed()))
    }

    // https://passbolt.testrail.io/index.php?/cases/view/8156
    @Test
    fun asAUserIShouldSeeAFailedFeedbackInCaseOfErrorDuringQrCodesSequence() {
        //      Given   I’m on a “Transferring your account details” page
        //      When    there is an error during the transfer process
        openStopTransferPrompt()
        onView(withText(LocalizationR.string.transfer_account_stop_button)).perform(click())
        //      Then    I see an unsuccessful “Something went wrong!” screen with a corresponding title
        onView(withText(LocalizationR.string.transfer_account_summary_cancelled)).check(matches(isDisplayed()))
        //      And     I see an unsuccessful illustration
        onView(withId(com.passbolt.mobile.android.feature.setup.R.id.icon))
            .check(matches(isDisplayed()))
            .check(matches(hasDrawable(id = CoreUiR.drawable.ic_failed)))
        //      And     I see an error message // ** there is cancel process tested here now so there is no msg
        //      And     I see a “Go back to my account”
        onView(withId(com.passbolt.mobile.android.feature.autofill.R.id.button)).check(matches(isDisplayed()))
    }

    // https://passbolt.testrail.io/index.php?/cases/view/C8157
    @Test
    fun asAUserICouldGoBackFromAFailedFeedbackInCaseOfErrorDuringQrCodesSequence() {
        //      Given   I’m on a “Transferring your account details” page
        //      And     there was an error during the transfer process
        openStopTransferPrompt()
        onView(withText(LocalizationR.string.transfer_account_stop_button)).perform(click())
        //      And     I see an unsuccessful “Something went wrong!” screen
        onView(withText(LocalizationR.string.transfer_account_summary_cancelled)).check(matches(isDisplayed()))
        //      When    I click a “Go back to my account”
        onView(withId(com.passbolt.mobile.android.feature.autofill.R.id.button)).perform(click())
        //      Then    I see the Account details page
        onView(withText(LocalizationR.string.settings_accounts)).check(matches(isDisplayed()))
    }

    private fun openStopTransferPrompt() {
        onView(withId(com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.R.id.startTransferButton)).perform(click())
        onView(withId(CoreUiR.id.input)).perform(typeText(managedAccountIntentCreator.getPassphrase()))
        onView(withId(com.passbolt.mobile.android.feature.authentication.R.id.authButton)).perform(scrollTo(), click())
        onView(withId(com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.R.id.cancelTransferButton)).perform(click())
    }
}
