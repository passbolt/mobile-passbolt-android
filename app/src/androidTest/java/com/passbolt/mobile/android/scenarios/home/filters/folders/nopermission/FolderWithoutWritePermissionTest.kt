/*
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

package com.passbolt.mobile.android.scenarios.home.filters.folders.nopermission

import android.view.KeyEvent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.pressKey
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.R.id as materialId
import com.passbolt.mobile.android.core.idlingresource.CreateFolderIdlingResource
import com.passbolt.mobile.android.core.idlingresource.ResourcesFullRefreshIdlingResource
import com.passbolt.mobile.android.core.idlingresource.SignInIdlingResource
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.feature.authentication.AuthenticationMainActivity
import com.passbolt.mobile.android.feature.home.R.id as homeId
import com.passbolt.mobile.android.first
import com.passbolt.mobile.android.instrumentationTestsModule
import com.passbolt.mobile.android.intents.ManagedAccountIntentCreator
import com.passbolt.mobile.android.rules.IdlingResourceRule
import com.passbolt.mobile.android.rules.lazyActivitySetupScenarioRule
import com.passbolt.mobile.android.scenarios.helpers.signIn
import org.hamcrest.CoreMatchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.component.inject
import org.koin.test.KoinTest
import kotlin.test.BeforeTest

@RunWith(AndroidJUnit4::class)
@MediumTest
class FolderWithoutWritePermissionTest : KoinTest {
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
        val createFolderIdlingResource: CreateFolderIdlingResource by inject()
        IdlingResourceRule(
            arrayOf(
                signInIdlingResource,
                createFolderIdlingResource,
                resourcesFullRefreshIdlingResource
            )
        )
    }

    @BeforeTest
    fun setup() {
        //  Background:
        //  Given I am using Passbolt PRO/Cloud/CE //Cloud
        //  And I am logged in as a mobile app user
        signIn(managedAccountIntentCreator.getPassphrase())
        //  And I am on the folders filter view
        onView(withId(materialId.text_input_start_icon)).perform(click())
        onView(withId(homeId.folders)).perform(click())
        //  And I have a folder which is shared with me
        //  And I do not have permission to create an item in this folder
        onView(withId(com.passbolt.mobile.android.feature.otp.R.id.searchEditText)).perform(
            typeText("Shared without permission to add"), pressKey(KeyEvent.KEYCODE_ENTER)
        )
    }

    @Test
    //  https://passbolt.testrail.io/index.php?/cases/view/11939
    fun viewFolderWithoutWritePermission() {
        //  Given  I have some subfolders in this folder
        //  And  I have some resources in it
        //  When I enter this folder
        onView(first(withId(homeId.itemFolder))).perform(click())
        //  Then I do not see the create button
        onView(withId(homeId.homeSpeedDialViewId)).check(matches(not(isDisplayed())))
        //  And  I see the list of folders
        onView(first(withId(homeId.itemFolder))).check((matches(isDisplayed())))
        //  And  I see the list of resources below the folders
        onView(first(withId(homeId.itemPassword))).check((matches(isDisplayed())))
    }
}
