package com.passbolt.mobile.android.scenarios.setupconfigurebiometric

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.BeforeTest

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
class SetupConfigureBiometricAvailableTest : KoinTest {

    @get:Rule
    val startActivityRule = lazyActivitySetupScenarioRule<SetUpActivity>(
        koinOverrideModules = listOf(instrumentationTestsModule, biometricSetupAvailableModuleTests),
        intentSupplier = {
            managedAccountIntentCreator.createIntent(
                InstrumentationRegistry.getInstrumentation().targetContext
            )
        }
    )

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
        onView(withId(R.id.qrCode)).perform(swipeUp())
        onView(withId(R.id.scanQrCodesButton)).perform(click())
        onView(withId(R.id.button)).perform(click())
    }

    @Test
    fun asAMobileUserICanUseBiometricsInsteadOfMyPassphrase() {
        //    Given     I have biometrics configured on my device
        //    And       I am on the Passphrase screen
        //    When      I successfully entered my passphrase
        onView(withId(R.id.input))
            .perform(typeText(managedAccountIntentCreator.getUsername()))
            .perform(swipeUp())
        onView(withId(R.id.authButton)).perform(click())
        //    Then      I am prompted to use biometrics instead of my passphrase
        //    And       I see a “Configure {biometric provider}” primary button
        onView(withId(R.id.icon)).check(matches(isDisplayed()))
        onView(withId(R.id.icon)).check(matches(hasDrawable(R.drawable.ic_use_fingerprint)))
        onView(withText(R.string.fingerprint_setup_use_title)).check(matches(isDisplayed()))
        onView(withText(R.string.fingerprint_setup_use_description)).check(matches(isDisplayed()))
        onView(withId(R.id.useFingerprintButton)).check(matches(isDisplayed()))
        //    And       I see a “Maybe later” button
        onView(withId(R.id.maybeLaterButton)).check(matches(isDisplayed()))
    }

    @Test
    fun asAMobileUserIAmInformedIfTheBiometricsOfMyDeviceChangedInTheMiddleOfTheSetup() {
        //    Given     that I am a mobile user with one or more account configured with biometric
        //    When      I changed my biometric on my device in the middle of the setup
        //    And       I open the passbolt application
        //    Then      I see a modal telling me that my biometric changed
        onView(withId(R.id.input)).perform(typeText(managedAccountIntentCreator.getUsername()))
        onView(withId(R.id.input)).perform(swipeUp())
        onView(withId(R.id.authButton)).perform(click())
        onView(withId(R.id.useFingerprintButton)).perform(click())
        onView(withText(R.string.fingerprint_authenticate_again)).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click())
        //    And       I need to enter my passphrase to complete the setup
        // TODO currently we cannot handle the biometric changes information dialog during configuration,
        //  we can only handle this dialog after pressing the "Use fingerprint" button on the biometric screen
    }
}
