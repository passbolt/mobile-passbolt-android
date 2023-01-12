package com.passbolt.mobile.android.scenarios.scanqrcode

import android.content.Intent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.rule.GrantPermissionRule
import com.passbolt.mobile.android.feature.setup.R
import com.passbolt.mobile.android.feature.setup.SetUpActivity
import com.passbolt.mobile.android.helpmenu.HelpMenuFragment
import com.passbolt.mobile.android.instrumentationTestsModule
import com.passbolt.mobile.android.rules.lazyActivityScenarioRule
import org.hamcrest.Matcher
import org.hamcrest.core.AllOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.KoinTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import com.passbolt.mobile.android.core.ui.R as CoreUiR

/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021 Passbolt SA
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

@RunWith(AndroidJUnit4::class)
@MediumTest
class ScanQrCodeTest : KoinTest {

    @get:Rule
    val activityRule = lazyActivityScenarioRule<SetUpActivity>(
        koinOverrideModules = listOf(instrumentationTestsModule)
    )

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

    @BeforeTest
    fun setup() {
        Intents.init()
        onView(withId(R.id.connectToAccountButton)).perform(click())
        onView(withId(R.id.scanQrCodesButton)).perform(scrollTo(), click())
    }

    @AfterTest
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun asAMobileUserICanGetHelpDuringTheQrCodeScanningProcess() {
        //        Given   the user is on the “Scanning QR codes” screen
        //        When    the user clicks on the “information” icon next to the progress bar
        onView(withContentDescription(CoreUiR.string.help_button_description)).perform(click())
        //        Then    a modal with help options is presented
        onView(withId(R.id.title)).check(matches(isDisplayed()))
        //        And     a "Why scanning QR codes?” button is available
        onView(withId(R.id.whyScanQrCodes)).check(matches(isDisplayed()))
        //        And     an "Enable debug logs" switch is available (on Android only)
        onView(withId(R.id.enableLogsSwitch)).check(matches(isDisplayed()))
        //        And     an "Access the logs" button is available
        onView(withId(R.id.accessLogs)).check(matches(isDisplayed()))
        //        And     a "Visit help site" button is available
        onView(withId(R.id.visitHelpWebsite)).check(matches(isDisplayed()))
    }

    @Test
    fun asAMobileUserICanOpenHelpWebpageDuringTheQrCodeScanningProcess() {
        //        Given   the user is on the “Help” modal
        onView(withContentDescription(CoreUiR.string.help_button_description)).perform(click())
        //        When    the user clicks on the “Visit help site” button
        onView(withId(R.id.visitHelpWebsite)).perform(click())
        //        Then    a webpage with help is presented
        val expectedIntent: Matcher<Intent> = AllOf.allOf(
            IntentMatchers.hasAction(Intent.ACTION_VIEW),
            IntentMatchers.hasData(HelpMenuFragment.HELP_WEBSITE_URL)
        )
        Intents.intended(expectedIntent)
    }

    @Test
    fun asAMobileUserICanSeeAnExplanationWhyScanningQrCodes() {
        //      Given   the user is on the “Help” modal
        onView(withContentDescription(CoreUiR.string.help_button_description)).perform(click())
        //      When    the user clicks on the "Why scanning QR codes" button
        onView(withId(R.id.whyScanQrCodes)).perform(click())
        //      Then    a dialog explaining the process of scanning QR codes is presented
        onView(withId(R.id.alertTitle)).check(matches(isDisplayed()))
        //      And     the message says how the process would look like
        onView(withId(android.R.id.message)).check(matches(isDisplayed()))
        //      And     a “Got it” button to close the dialog is presented
        //      And     a “Got it” button is clickable
        onView(withId(android.R.id.button1))
            .check(matches(isDisplayed()))
            .check(matches(ViewMatchers.isClickable()))
    }
}
