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

package com.passbolt.mobile.android.scenarios.setupconfigurebiometric

import android.content.Intent
import android.provider.Settings
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.passbolt.mobile.android.core.idlingresource.SignInIdlingResource
import com.passbolt.mobile.android.feature.setup.R
import com.passbolt.mobile.android.feature.setup.SetUpActivity
import com.passbolt.mobile.android.hasDrawable
import com.passbolt.mobile.android.instrumentationTestsModule
import com.passbolt.mobile.android.intents.ManagedAccountIntentCreator
import com.passbolt.mobile.android.rules.IdlingResourceRule
import com.passbolt.mobile.android.rules.lazyActivitySetupScenarioRule
import org.hamcrest.Matcher
import org.hamcrest.core.AllOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@RunWith(AndroidJUnit4::class)
@LargeTest
class SetupConfigureBiometricUnavailableTest : KoinTest {

    @get:Rule
    val startActivityRule = lazyActivitySetupScenarioRule<SetUpActivity>(koinOverrideModules = listOf(
        instrumentationTestsModule,
        biometricSetupUnavailableModuleTests
    ),
        intentSupplier = {
            managedAccountIntentCreator.createIntent(
                InstrumentationRegistry.getInstrumentation().targetContext
            )
        })

    @get:Rule
    val idlingResourceRule = let {
        val signInIdlingResource: SignInIdlingResource by inject()
        IdlingResourceRule(arrayOf(signInIdlingResource))
    }

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

    private val managedAccountIntentCreator: ManagedAccountIntentCreator by inject()

    @BeforeTest
    fun setup() {
        onView(withId(R.id.connectToAccountButton)).perform(click())
        onView(withId(R.id.scanQrCodesButton)).perform(scrollTo(), click())
        onView(withId(R.id.button)).perform(click())
        Intents.init()
    }

    @AfterTest
    fun tearDown() {
        Intents.release()
    }

    @Test
    // https://passbolt.testrail.io/index.php?/cases/view/2358
    fun asAMobileUserIHaveAnOptionToConfigureBiometricsOnTheDevice() {
        //    Given     I don't have biometrics configured on my device
        //    And       I am on the Passphrase screen
        //    When      I successfully entered my passphrase
        onView(withId(R.id.input)).perform(typeText(managedAccountIntentCreator.getUsername()))
        onView(withId(R.id.authButton)).perform(scrollTo(), click())
        //    Then       I am prompted to Configure biometrics
        //    And        I see a “Configure {biometric provider}” primary button
        onView(withId(R.id.icon)).check(matches(isDisplayed()))
        onView(withId(R.id.icon)).check(matches(hasDrawable(R.drawable.ic_configure_fingerprint)))
        onView(withText(R.string.fingerprint_setup_configure_title)).check(matches(isDisplayed()))
        onView(withText(R.string.fingerprint_setup_configure_description)).check(matches(isDisplayed()))
        onView(withId(R.id.useFingerprintButton)).check(matches(isDisplayed()))
        //    And       I see a “Maybe later” button
        onView(withId(R.id.maybeLaterButton)).check(matches(isDisplayed()))
    }

    @Test
    // https://passbolt.testrail.io/index.php?/cases/view/2359
    fun asAMobileUserICanConfigureBiometricsToUseItOnTheDevice() {
        //    Given     I don't have biometrics configured on my device
        //    And       I am on the Configure {biometrics provider} screen
        onView(withId(R.id.input)).perform(typeText(managedAccountIntentCreator.getUsername()))
        onView(withId(R.id.authButton)).perform(scrollTo(), click())
        //    When      I click on Configure {biometrics provider} button
        onView(withId(R.id.useFingerprintButton)).perform(click())
        //    Then      I am taken to the phone security settings / OS-specific process where I can complete the biometric setup
        val expectedIntent: Matcher<Intent> = AllOf.allOf(
            IntentMatchers.hasAction(Settings.ACTION_SETTINGS),
        )
        Intents.intended(expectedIntent)
        //    And       I can go back to the application
    }

    @Test
    // https://passbolt.testrail.io/index.php?/cases/view/2360
    fun asAMobileUserIShouldBeAbleToSkipTheBiometricsConfiguration() {
        //    Given     I don't have biometrics configured on my device
        //    And       I am on the Configure {biometrics provider} screen
        onView(withId(R.id.input)).perform(typeText(managedAccountIntentCreator.getUsername()))
        onView(withId(R.id.authButton)).perform(scrollTo(), click())
        //    When      I click the “Maybe later” button
        onView(withId(R.id.maybeLaterButton)).perform(click())
        //    Then      I am redirected to the setup of the autofill screen
        onView(withText(R.string.dialog_encourage_autofill_header)).check(matches(isDisplayed()))
        onView(withId(R.id.stepsView)).check(matches(isDisplayed()))
        onView(withId(R.id.goToSettingsButton)).check(matches(isDisplayed()))
        onView(withId(R.id.maybeLaterButton)).check(matches(isDisplayed()))
    }
}
