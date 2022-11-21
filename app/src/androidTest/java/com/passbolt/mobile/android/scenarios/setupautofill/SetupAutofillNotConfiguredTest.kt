package com.passbolt.mobile.android.scenarios.setupautofill

import android.content.Intent
import android.provider.Settings
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.passbolt.mobile.android.core.idlingresource.SignInIdlingResource
import com.passbolt.mobile.android.feature.setup.R
import com.passbolt.mobile.android.feature.startup.StartUpActivity
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
class SetupAutofillNotConfiguredTest : KoinTest {

    @get:Rule
    val startActivityRule = lazyActivitySetupScenarioRule<StartUpActivity>(
        koinOverrideModules = listOf(instrumentationTestsModule, autofillNotConfiguredModuleTests),
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
        onView(withId(R.id.scanQrCodesButton)).perform(click())
        onView(withId(R.id.button)).perform(click())
        onView(withId(R.id.input)).perform(typeText(managedAccountIntentCreator.getUsername()))
        onView(withId(R.id.authButton)).perform(click())
        Intents.init()
    }

    @AfterTest
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun asAMobileUserIShouldBePromptedToEnableTheSettingsOfTheAutofillDuringTheSetupProcess() {
        //    Given     Autofill is not configured for Passbolt
        //    When      I skip or finish the biometric configuration
        onView(withId(R.id.maybeLaterButton)).perform((click()))
        //    Then      I am on the page explaining the Autofill configuration
        //    And       I see a "Go to settings" primary button
        onView(withId(R.id.goToSettingsButton)).check(matches(isDisplayed()))
        onView(withId(R.id.closeButton)).check(matches(isDisplayed()))
        onView(withId(R.id.stepsView)).check(matches(isDisplayed()))
        onView(withText(R.string.dialog_encourage_autofill_header)).check(matches(isDisplayed()))
        //    And       I see a "Maybe later" button
        onView(withId(R.id.maybeLaterButton)).check(matches(isDisplayed()))
    }

    @Test
    fun asAMobileUserIShouldBeAbleToSetupPassboltAutofillDuringTheSetupProcessIfItIsNotAlreadyConfigured() {
        //    Given     I am on the Autofill setup page
        onView(withId(R.id.maybeLaterButton)).perform((click()))
        //    When      I click on the "Go to settings" button
        onView(withId(R.id.goToSettingsButton)).perform(click())
        //    Then      I am redirected to the settings of the page for Autofill or to the Settings where I can enable the autofill
        val expectedIntent: Matcher<Intent> = AllOf.allOf(
            IntentMatchers.hasAction(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE),
        )
        Intents.intended(expectedIntent)
        //    And       I can go back to the application
    }

    @Test
    fun asAMobileUserIShouldBeAbleToSkipTheAutofillConfigurationDuringTheSetupProcess() {
        //    Given     I am on the Autofill setup page
        onView(withId(R.id.maybeLaterButton)).perform((click()))
        //    When      I click on the "Maybe later" button
        onView(withId(R.id.maybeLaterButton)).perform(click())
        //    Then      I am redirected to the home page
        onView(withId(R.id.rootLayout)).check(matches(isDisplayed()))
    }
}
