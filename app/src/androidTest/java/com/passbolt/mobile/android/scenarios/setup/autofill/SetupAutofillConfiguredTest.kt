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

package com.passbolt.mobile.android.scenarios.setup.autofill

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.passbolt.mobile.android.accountinit.AccountDataCleaner
import com.passbolt.mobile.android.accountinit.AccountInitializer
import com.passbolt.mobile.android.core.idlingresource.SignInIdlingResource
import com.passbolt.mobile.android.feature.setup.R
import com.passbolt.mobile.android.feature.startup.StartUpActivity
import com.passbolt.mobile.android.instrumentationTestsModule
import com.passbolt.mobile.android.intents.ManagedAccountIntentCreator
import com.passbolt.mobile.android.rules.IdlingResourceRule
import com.passbolt.mobile.android.rules.lazyActivityScenarioRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@RunWith(AndroidJUnit4::class)
@MediumTest
class SetupAutofillConfiguredTest : KoinTest {
    @get:Rule
    val startActivityRule =
        lazyActivityScenarioRule<StartUpActivity>(
            koinOverrideModules = listOf(instrumentationTestsModule, autofillConfiguredModuleTests),
            intentSupplier = {
                managedAccountIntentCreator.createIntent(
                    InstrumentationRegistry.getInstrumentation().targetContext,
                )
            },
        )

    @get:Rule
    val idlingResourceRule =
        let {
            val signInIdlingResource: SignInIdlingResource by inject()
            IdlingResourceRule(arrayOf(signInIdlingResource))
        }

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

    private val managedAccountIntentCreator: ManagedAccountIntentCreator by inject()
    private val accountDataCleaner: AccountDataCleaner by inject()
    private val accountDataInitializer: AccountInitializer by inject()

    @BeforeTest
    fun setup() {
        onView(withId(R.id.connectToAccountButton)).perform(click())
        onView(withId(R.id.scanQrCodesButton)).perform(scrollTo(), click())
        onView(withId(com.passbolt.mobile.android.feature.autofill.R.id.button)).perform(click())
        onView(withId(CoreUiR.id.input)).perform(typeText(managedAccountIntentCreator.getPassphrase()))
        onView(withId(com.passbolt.mobile.android.feature.authentication.R.id.authButton)).perform(scrollTo(), click())
        accountDataInitializer.initializeAccount()
    }

    @AfterTest
    fun tearDown() {
        accountDataCleaner.clearAccountData()
    }

    //    https://passbolt.testrail.io/index.php?/cases/view/2365
    @Test
    fun asAMobileUserIShouldNotSeeAPromptToEnableAutofillForPassboltIfItIsAlreadyConfigured() {
        //    Given     Autofill is configured for Passbolt
        //    When      I skip or finish the biometric configuration
        onView(withId(R.id.maybeLaterButton)).perform((click()))
        //    Then      I do not see the page explaining the autofill configuration
        //    And       I see the home page
        onView(withId(com.passbolt.mobile.android.feature.permissions.R.id.rootLayout)).check(matches(isDisplayed()))
    }
}
