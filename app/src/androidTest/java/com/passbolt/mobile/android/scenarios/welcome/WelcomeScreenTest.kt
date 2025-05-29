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
import androidx.appcompat.widget.Toolbar
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.passbolt.mobile.android.commontest.viewassertions.CastedViewAssertion
import com.passbolt.mobile.android.feature.setup.R
import com.passbolt.mobile.android.feature.setup.SetUpActivity
import com.passbolt.mobile.android.helpers.getString
import com.passbolt.mobile.android.instrumentationTestsModule
import com.passbolt.mobile.android.rules.lazyActivityScenarioRule
import org.hamcrest.Matcher
import org.hamcrest.core.AllOf.allOf
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

    //    https://passbolt.testrail.io/index.php?/cases/view/2332
    @Test
    fun asAMobileUserICanSeeTheWelcomeScreenWhenIOpenTheApplicationAndNoAccountIsSetup() {
        //    Given     that the application is not configured for any users
        //    When      I launch the application
        //    Then      I see a welcome screen
        //    And       I see a Passbolt logo
        onView(withId(R.id.logoImage)).check(matches(isDisplayed()))
        //    And       I see a welcome illustration
        onView(withId(R.id.appsImage)).check(matches(isDisplayed()))
        //    And       I see a welcome message
        onView(withId(R.id.titleLabel)).check(matches(isDisplayed()))
        onView(withId(R.id.descriptionLabel)).check(matches(isDisplayed()))
        //    And       I see a “connect to an existing account” primary action
        onView(withId(R.id.connectToAccountButton)).check(matches(isDisplayed()))
        //    And       I see an "I don’t have an account" secondary action
        onView(withId(R.id.noAccountButton)).check(matches(isDisplayed()))
        //    And       I see a "Help" side action
        onView(withId(R.id.helpButton)).check(matches(isDisplayed()))
    }

    //    https://passbolt.testrail.io/index.php?/cases/view/2333
    @Test
    fun asAMobileUserICanSeeAnExplanationWhyICannotCreateAnAccountOnTheMobileApp() {
        //      Given   the welcome screen is displayed
        //      When    the user click on the "I don't have an account" button
        onView(withId(R.id.noAccountButton)).perform(click())
        //      Then    a dialog explaining why I can’t create an account is presented
        onView(withId(androidx.appcompat.R.id.alertTitle)).check(matches(isDisplayed()))
        //      And     the message says "I need to create an account first using the web"
        onView(withId(android.R.id.message)).check(matches(isDisplayed()))
        //      And     a “Got it” button to close the dialog is presented
        //      And     a “Got it” button is clickable
        onView(withId(android.R.id.button1))
            .check(matches(isDisplayed()))
            .check(matches(isClickable()))
    }

    //    https://passbolt.testrail.io/index.php?/cases/view/6190
    @Test
    fun asAMobileUserICanGetHelpBeforeTheQrCodeScanningProcess() {
        //    Given   the user is on the “Welcome” screen
        //    When    the user clicks on the “information” icon on the top
        onView(withId(R.id.helpButton)).perform(click())
        //    Then    a modal with help options is presented
        onView(withId(R.id.title)).check(matches(isDisplayed()))
        //    And     an "Enable debug logs" switch is available (on Android only)
        onView(withId(com.passbolt.mobile.android.feature.helpmenu.R.id.enableLogsSwitch)).check(matches(isDisplayed()))
        //    And     an "Access the logs" button is available
        onView(withId(com.passbolt.mobile.android.feature.helpmenu.R.id.accessLogs)).check(matches(isDisplayed()))
        //    And     a "Visit help site" button is available
        onView(withId(com.passbolt.mobile.android.feature.settings.R.id.visitHelpWebsite)).check(matches(isDisplayed()))
    }

    //    https://passbolt.testrail.io/index.php?/cases/view/6191
    @Test
    fun asAMobileUserICanOpenHelpWebpageBeforeTheQrCodeScanningProcess() {
        Intents.init()

        //        Given   the user is on the “Help” modal
        onView(withId(R.id.helpButton)).perform(click())
        //        When    the user clicks on the “Visit help site” button
        onView(withId(com.passbolt.mobile.android.feature.settings.R.id.visitHelpWebsite)).perform(click())
        //        Then    a webpage with help is presented
        val expectedIntent: Matcher<Intent> =
            allOf(
                hasAction(Intent.ACTION_VIEW),
                hasData(getString(LocalizationR.string.help_website)),
            )
        intended(expectedIntent)

        Intents.release()
    }

    //    https://passbolt.testrail.io/index.php?/cases/view/2334
    @Test
    fun asAMobileUserICanSeeAnExplanationOnHowToConnectAnExistingAccount() {
        //        Given   the welcome screen is displayed
        //        When    the user clicks on “connect to an existing account”
        onView(withId(R.id.connectToAccountButton)).perform(click())
        //        Then    the “Transfer account details” explanation screen is presented
        onView(withId(R.id.header)).check(matches(isDisplayed()))
        //        And     the screen has an arrow button on the top left to go back to the welcome screen
        onView(isAssignableFrom(Toolbar::class.java))
            .check(CastedViewAssertion<Toolbar> { it.navigationIcon != null })
        //        And             it has an explanation of the different steps of the setup process
        onView(withId(R.id.steps)).check(matches(isDisplayed()))
        //        And             it has an illustration giving some context about the process
        onView(withId(R.id.qrCode)).check(matches(isDisplayed()))
        //        And             it has a "Scan QR codes" primary action button
        onView(withId(R.id.scanQrCodesButton)).perform(scrollTo()).check(matches(isDisplayed()))
    }
}
