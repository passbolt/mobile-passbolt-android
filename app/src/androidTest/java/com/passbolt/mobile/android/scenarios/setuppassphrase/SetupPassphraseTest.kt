package com.passbolt.mobile.android.scenarios.setuppassphrase

import androidx.appcompat.widget.Toolbar
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.passbolt.mobile.android.commontest.viewassertions.CastedViewAssertion
import com.passbolt.mobile.android.feature.setup.R
import com.passbolt.mobile.android.feature.startup.StartUpActivity
import com.passbolt.mobile.android.instrumentationTestsModule
import com.passbolt.mobile.android.intents.ManagedAccountIntentCreator
import com.passbolt.mobile.android.mappers.AccountModelMapper
import com.passbolt.mobile.android.rules.lazyActivityScenarioRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
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
class SetupPassphraseTest : KoinTest {

    @get:Rule
    val startUpActivityRule = lazyActivityScenarioRule<StartUpActivity>(
        koinOverrideModule = instrumentationTestsModule,
        intentSupplier = {
            managedAccountIntentCreator.createIntent(
                InstrumentationRegistry.getInstrumentation().targetContext
            )
        }
    )

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

    private val managedAccountIntentCreator: ManagedAccountIntentCreator by inject()

    @BeforeTest
    fun setup() {
        loadKoinModules(instrumentationTestsModule)
        onView(withId(R.id.connectToAccountButton)).perform(click())
        onView(withId(R.id.scanQrCodesButton)).perform(click())
    }

    @AfterTest
    fun tearDown() {
        unloadKoinModules(instrumentationTestsModule)
    }

    @Test
    fun asAMobileUserIShouldSeeTheEnterMyPassphraseScreenAfterISuccessfullyScannedQrCodes() {
        //Given  the user is on the "Success feedback" screen at the end of the QR code scanning process
        //When   the user clicks the "Continue" button
        onView(withId(R.id.button)).perform(click())
        //Then   an "Enter your passphrase" page is presented
        onView(withText(R.string.auth_enter_passphrase)).check(matches(isDisplayed()))
        //And    a back arrow button is presented
        onView(isAssignableFrom(Toolbar::class.java))
            .check(CastedViewAssertion<Toolbar> { it.navigationIcon != null })
        //And    current user's name is presented
        val firstName = managedAccountIntentCreator.getFirstName()
        val lastName = managedAccountIntentCreator.getLastName()
        onView(withText(AccountModelMapper.defaultLabel(firstName, lastName))).check(matches(isDisplayed()))
        //And    current user's email is presented
        val email = managedAccountIntentCreator.getUsername()
        onView(withText(email)).check(matches(isDisplayed()))
        //And    the url of the server is presented
        val url = managedAccountIntentCreator.getDomain()
        onView(withText(url)).check(matches(isDisplayed()))
        //And    current user's avatar or the default avatar is presented
        onView(withId(R.id.avatarImage)).check(matches(isDisplayed()))
        //And    a passphrase input field is presented
        onView(withId(R.id.input)).check(matches(isDisplayed()))
        //And    an eye icon to toggle passphrase visibility is presented
        onView(withId(R.id.text_input_end_icon)).check(matches(isDisplayed()))
        //And    a sign in the primary action button is presented
        onView(withId(R.id.authButton)).check(matches(isDisplayed()))
        //And    “I forgot my passphrase” link is presented
        onView(withId(R.id.forgotPasswordButton)).check(matches(isDisplayed()))
    }
}
