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

package com.passbolt.mobile.android.scenarios.setup.passphrase

import androidx.appcompat.widget.Toolbar
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasTextColor
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.passbolt.mobile.android.commontest.viewassertions.CastedViewAssertion
import com.passbolt.mobile.android.core.idlingresource.SignInIdlingResource
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.feature.authentication.AuthenticationMainActivity
import com.passbolt.mobile.android.feature.setup.R
import com.passbolt.mobile.android.instrumentationTestsModule
import com.passbolt.mobile.android.intents.ManagedAccountIntentCreator
import com.passbolt.mobile.android.mappers.AccountModelMapper
import com.passbolt.mobile.android.matchers.hasToast
import com.passbolt.mobile.android.matchers.isTextHidden
import com.passbolt.mobile.android.rules.IdlingResourceRule
import com.passbolt.mobile.android.rules.lazyActivitySetupScenarioRule
import org.hamcrest.Matchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.BeforeTest
import com.google.android.material.R as MaterialR
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR


@RunWith(AndroidJUnit4::class)
@MediumTest
class SetupPassphraseTest : KoinTest {

    @get:Rule
    val startUpActivityRule = lazyActivitySetupScenarioRule<AuthenticationMainActivity>(
        koinOverrideModules = listOf(instrumentationTestsModule),
        intentSupplier = {
            ActivityIntents.authentication(
                InstrumentationRegistry.getInstrumentation().targetContext,
                ActivityIntents.AuthConfig.Setup,
                AppContext.APP,
                managedAccountIntentCreator.getUserLocalId()
            )
        }
    )

    private val managedAccountIntentCreator: ManagedAccountIntentCreator by inject()

    @get:Rule
    val idlingResourceRule = let {
        val signInIdlingResource: SignInIdlingResource by inject()
        IdlingResourceRule(
            arrayOf(
                signInIdlingResource
            )
        )
    }

    @BeforeTest
    fun setup() {
    }

    @Test
    //    https://passbolt.testrail.io/index.php?/cases/view/2349
    fun asAMobileUserIShouldSeeTheEnterMyPassphraseScreenAfterISuccessfullyScannedQrCodes() {
        //    Given     the user is on the "Success feedback" screen at the end of the QR code scanning process
        //    When      the user clicks the "Continue" button
        //    Then      an "Enter your passphrase" page is presented
        onView(withText(LocalizationR.string.auth_enter_passphrase)).check(matches(isDisplayed()))
        //    And       a back arrow button is presented
        onView(isAssignableFrom(Toolbar::class.java))
            .check(CastedViewAssertion<Toolbar> { it.navigationIcon != null })
        //    And       current user's name is presented
        val firstName = managedAccountIntentCreator.getFirstName()
        val lastName = managedAccountIntentCreator.getLastName()
        onView(withText(AccountModelMapper.defaultLabel(firstName, lastName))).check(matches(isDisplayed()))
        //    And       current user's email is presented
        val email = managedAccountIntentCreator.getUsername()
        onView(withText(email)).check(matches(isDisplayed()))
        //    And       the url of the server is presented
        val url = managedAccountIntentCreator.getDomain()
        onView(withText(url)).check(matches(isDisplayed()))
        //    And       current user's avatar or the default avatar is presented
        onView(withId(com.passbolt.mobile.android.feature.accountdetails.R.id.avatarImage)).check(matches(isDisplayed()))
        //    And       a passphrase input field is presented
        onView(withId(CoreUiR.id.input)).check(matches(isDisplayed()))
        //    And       an eye icon to toggle passphrase visibility is presented
        onView(withId(MaterialR.id.text_input_end_icon)).check(matches(isDisplayed()))
        //    And       a sign in the primary action button is presented
        onView(withId(com.passbolt.mobile.android.feature.authentication.R.id.authButton)).check(matches(isDisplayed()))
        //    And       “I forgot my passphrase” link is presented
        onView(withId(com.passbolt.mobile.android.feature.authentication.R.id.forgotPasswordButton)).check(
            matches(
                isDisplayed()
            )
        )
    }

    @Test
    //    https://passbolt.testrail.io/index.php?/cases/view/2353
    fun asAMobileUserICanPreviewMyPassphrase() {
        //    Given     I am on the "Enter your passphrase" page
        //    And       there is some <initial text> inside the passphrase field
        onView(withId(CoreUiR.id.input)).perform(typeText("SomeRandomText\n"))
        //    When      I click the "eye" button inside the passphrase field
        onView(withId(MaterialR.id.text_input_end_icon)).perform(click())
        //    Then      I see the content of the passphrase field in <output format>
        //              |initial text | output format |
        //              |hidden text  | plain text |
        //              |plain text   | hidden text |
        onView(withId(CoreUiR.id.input)).check(matches(not(isTextHidden())))
        onView(withId(MaterialR.id.text_input_end_icon)).perform(click())
        onView(withText(managedAccountIntentCreator.getUsername())).check(matches(isDisplayed()))
    }

    @Test
    //    https://passbolt.testrail.io/index.php?/cases/view/2354
    fun asAMobileUserICanSeeAFeedbackMessageIfIEnteredTheWrongPassphrase() {
        //    Given     I am on the “Enter your passphrase" page
        //    When      I submit a wrong passphrase
        onView(withId(CoreUiR.id.input)).perform(typeText("wrongPass1!@\n"))
        onView(withId(com.passbolt.mobile.android.feature.authentication.R.id.authButton)).perform(click())
        //    Then      I see a toast notification with error message
        //    And       the toast is at the bottom of the screen #Not automated
        onView(withText(LocalizationR.string.auth_enter_passphrase))
            .inRoot(hasToast())
            .check(matches(isDisplayed()))
        //    And       the toast is in red #Not automated
        //    And       the input and label are still in the same colors
        onView(withId(R.id.titleLabel)).check(matches(hasTextColor(CoreUiR.color.text_primary)))
        onView(withId(CoreUiR.id.input)).check(matches(hasTextColor(com.google.android.gms.base.R.color.common_google_signin_btn_text_light_pressed)))
        //    And       the message says "Incorrect passphrase or decryption error. Please try again."
        onView(withText("Incorrect passphrase or decryption error. Please try again.")).inRoot(hasToast())
            .check(matches(isDisplayed()))
    }

    @Test
    //    https://passbolt.testrail.io/index.php?/cases/view/2352
    fun asAMobileUserICanGetSomeHelpIfIForgotMyPassphrase() {
        //    Given     I am on the "Enter your passphrase" page
        //    When      I click the "forgot my passphrase" link
        onView(withId(com.passbolt.mobile.android.feature.authentication.R.id.forgotPasswordButton)).perform(click())
        //    Then      I see a dialog with a help
        onView(withId(androidx.appcompat.R.id.parentPanel)).check(matches(isDisplayed()))
        //    And       help text says the setup process can't be completed without a passphrase
        onView(withId(androidx.appcompat.R.id.alertTitle)).check(matches(isDisplayed()))
        //    And        I see a message telling me to contact my administrator
        onView(withId(android.R.id.message)).check(matches(isDisplayed()))
        //    And       a “Got it” button to close the dialog is presented
        //    And       a “Got it” button is clickable
        onView(withId(android.R.id.button1))
            .check(matches(isDisplayed()))
            .check(matches(isClickable()))
    }
}
