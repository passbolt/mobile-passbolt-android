/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2024 Passbolt SA
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

package com.passbolt.mobile.android.scenarios.resourcescreation.setexpiry

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.passbolt.mobile.android.core.idlingresource.CreateResourceIdlingResource
import com.passbolt.mobile.android.core.idlingresource.ResourcesFullRefreshIdlingResource
import com.passbolt.mobile.android.core.idlingresource.SignInIdlingResource
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.feature.authentication.AuthenticationMainActivity
import com.passbolt.mobile.android.helpers.createNewPasswordFromHomeScreen
import com.passbolt.mobile.android.helpers.pickFirstResourceWithName
import com.passbolt.mobile.android.helpers.signIn
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
import com.google.android.material.R as MaterialR


@RunWith(AndroidJUnit4::class)
@MediumTest
class SetExpiryTest : KoinTest {

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

    private val resourcesFullRefreshIdlingResource: ResourcesFullRefreshIdlingResource by inject()

    @get:Rule
    val idlingResourceRule = let {
        val signInIdlingResource: SignInIdlingResource by inject()
        val createResourceIdlingResource: CreateResourceIdlingResource by inject()
        IdlingResourceRule(
            arrayOf(
                signInIdlingResource,
                resourcesFullRefreshIdlingResource,
                createResourceIdlingResource,
            )
        )
    }

    @BeforeTest
    fun setup() {
        //Given  I am logged in as a Pro or Cloud user // Cloud user
        //And    automatic expiry is enabled on the server
        //And    automatic expiry is set to <number of days> // 7 days
        //When   I create a new resource
        signIn(managedAccountIntentCreator.getPassphrase())
        createNewPasswordFromHomeScreen("PasswordWithExpirySet")
        onView(withId(MaterialR.id.text_input_start_icon)).perform(click())
        onView(withId(com.passbolt.mobile.android.feature.home.R.id.recentlyModified)).perform(click())
    }


    @Test
    //    https://passbolt.testrail.io/index.php?/cases/view/11935
    fun setExpiryOfCreatedResourceToDefaultExpiryPeriod() {
        pickFirstResourceWithName("PasswordWithExpirySet")
        onView(withId(com.passbolt.mobile.android.feature.resources.R.id.expiryItem)).check(matches(isDisplayed()))
        //Then   The resource is marked to expire after <number of days>
        onView(withText("In 7 days")).check(
            matches(
                isDisplayed()
            )
        )
    }
}
