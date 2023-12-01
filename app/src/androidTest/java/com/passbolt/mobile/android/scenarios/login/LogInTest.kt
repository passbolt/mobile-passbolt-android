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

package com.passbolt.mobile.android.scenarios.login

import androidx.appcompat.widget.Toolbar
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.R
import com.passbolt.mobile.android.commontest.viewassertions.CastedViewAssertion
import com.passbolt.mobile.android.core.idlingresource.ResourcesFullRefreshIdlingResource
import com.passbolt.mobile.android.core.idlingresource.SignInIdlingResource
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.feature.authentication.AuthenticationMainActivity
import com.passbolt.mobile.android.instrumentationTestsModule
import com.passbolt.mobile.android.intents.ManagedAccountIntentCreator
import com.passbolt.mobile.android.rules.IdlingResourceRule
import com.passbolt.mobile.android.rules.lazyActivitySetupScenarioRule
import org.hamcrest.CoreMatchers.containsString
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.passbolt.mobile.android.feature.authentication.R as authR

@RunWith(AndroidJUnit4::class)
@LargeTest
class LogInTest : KoinComponent {

    @get:Rule
    val startUpActivityRule = lazyActivitySetupScenarioRule<AuthenticationMainActivity>(
        koinOverrideModules = listOf(instrumentationTestsModule),
        intentSupplier = {
            ActivityIntents.authentication(
                InstrumentationRegistry.getInstrumentation().targetContext,
                ActivityIntents.AuthConfig.Startup,
                AppContext.APP,
                managedAccountIntentCreator.getUserLocalId()
            )
        }
    )

    private val managedAccountIntentCreator: ManagedAccountIntentCreator by inject()

    @get:Rule
    val idlingResourceRule = let {
        val signInIdlingResource: SignInIdlingResource by inject()
        val resourcesFullRefreshIdlingResource: ResourcesFullRefreshIdlingResource by inject()
        IdlingResourceRule(arrayOf(signInIdlingResource, resourcesFullRefreshIdlingResource))
    }


    @Test
//  https://passbolt.testrail.io/index.php?/cases/view/2368
    fun asAMobileUserWithoutBiometricsConfiguredIAmPromptedToEnterMyPassphraseToLogin() {
        //  Given  that I am a mobile user with the application installed
        //  And    I have only one account configured
        //  And    I am logged out
        //  And    I didn't configured biometrics for my passphrase
        //  When   I open the application
        //  Then   I see the splash screen for a few seconds
        //  And    I see a login page prompting me to enter the passphrase
        //  And    I see an input field to enter my passphrase
        onView(withClassName(containsString("TextInputEditText")))
            .check(matches(isDisplayed()))
        //  And    I see an eye button to show my passphrase
        onView(withId(R.id.text_input_end_icon))
            .check(matches(isDisplayed()))
        //  And    I do not see a biometric provider button <<not automated>>
        //  And    I see a “Sign in” primary button
        onView(withId(authR.id.authButton))
            .check(matches(isDisplayed()))
        //  And    I see a forgot my passphrase button
        onView(withId(authR.id.forgotPasswordButton))
            .check(matches(isDisplayed()))
        //  And    I see a back arrow leading to the "List of accounts" welcome screen
        onView(ViewMatchers.isAssignableFrom(Toolbar::class.java))
            .check(CastedViewAssertion<Toolbar> { it.navigationIcon != null })
        //  And    I see my avatar
        onView(withId(authR.id.avatarImage))
            .check(matches(isDisplayed()))
    }
}
