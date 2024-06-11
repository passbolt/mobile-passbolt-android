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

package com.passbolt.mobile.android.scenarios.resourcesedition.updateexpiry

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.passbolt.mobile.android.core.idlingresource.CreateResourceIdlingResource
import com.passbolt.mobile.android.core.idlingresource.ResourcesFullRefreshIdlingResource
import com.passbolt.mobile.android.core.idlingresource.SignInIdlingResource
import com.passbolt.mobile.android.core.idlingresource.UpdateResourceIdlingResource
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.feature.authentication.AuthenticationMainActivity
import com.passbolt.mobile.android.first
import com.passbolt.mobile.android.instrumentationTestsModule
import com.passbolt.mobile.android.intents.ManagedAccountIntentCreator
import com.passbolt.mobile.android.rules.IdlingResourceRule
import com.passbolt.mobile.android.rules.lazyActivitySetupScenarioRule
import com.passbolt.mobile.android.scenarios.helpers.createNewPasswordFromHomeScreen
import com.passbolt.mobile.android.scenarios.helpers.signIn
import com.passbolt.mobile.android.scenarios.resourcesedition.EditableFieldInput
import com.passbolt.mobile.android.withHint
import com.passbolt.mobile.android.withIndex
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.component.inject
import org.koin.test.KoinTest
import kotlin.test.BeforeTest
import com.google.android.material.R as MaterialR
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR


@RunWith(AndroidJUnit4::class)
@MediumTest
class UpdateExpiryTest : KoinTest {

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
        val updateResourceIdlingResource: UpdateResourceIdlingResource by inject()
        val createResourceIdlingResource: CreateResourceIdlingResource by inject()
        IdlingResourceRule(
            arrayOf(
                signInIdlingResource,
                resourcesFullRefreshIdlingResource,
                updateResourceIdlingResource,
                createResourceIdlingResource
            )
        )
    }

    @BeforeTest
    fun setup() {
        signIn(managedAccountIntentCreator.getPassphrase())
        chooseFilter(com.passbolt.mobile.android.feature.home.R.id.expiry)
    }

    @Test
    //  https://passbolt.testrail.io/index.php?/cases/view/11937
    fun updateExpiryOfAResourceWhenSecretHasChanged() {
        // create a resource for future use - it will expire in 7 days
        // alternatively create expired resource in webExtension named ExpiringResource
        createNewPasswordFromHomeScreen("ExpiringResource")
        //  Given  I am logged in as a Pro or Cloud user // Cloud
        //  And    automatic expiry is enabled on the server
        //  And    automatic expiry is set to <number of days> //7 days
        onView(withId(com.passbolt.mobile.android.feature.otp.R.id.searchEditText)).perform(typeText("ExpiringR")) // search for a different but expired resource
        onView(first(withId(com.passbolt.mobile.android.feature.otp.R.id.more))).perform(click())
        onView(withId(com.passbolt.mobile.android.feature.resourcemoremenu.R.id.editPassword)).perform(click())
        //    When   I edit a password of the resource
        onViewInputWithHintName("Enter password").perform(replaceText("UpdatedForExpiryTest"))
        onView(withId(com.passbolt.mobile.android.feature.resources.R.id.updateButton)).perform(scrollTo(), click())
        //    Then   The resource is marked to expire after <number of days> //7 days
        chooseFilter(com.passbolt.mobile.android.feature.home.R.id.recentlyModified)
        onView(withIndex(index = 0, withText("ExpiringResource"))).perform(click())
        onView(withId(com.passbolt.mobile.android.feature.resources.R.id.expiryItem)).check(matches(isDisplayed()))
        onView(withText("In 7 days")).check(matches(isDisplayed()))
    }

    @Test
    //  https://passbolt.testrail.io/index.php?/cases/view/11938
    fun doNotUpdateExpiryOfAResourceWhenAllItemsExceptPasswordHasChanged() {
        //  Given  I am logged in as a Pro or Cloud user // Cloud
        //  And    automatic expiry is enabled on the server
        //  And    automatic expiry is set to <number of days> // 7 days
        //  And    the resource is set to expire <expire date> // in the past
        onView(withId(com.passbolt.mobile.android.feature.otp.R.id.searchEditText)).perform(typeText("Expired"))
        //  When   I edit an resource omitting password
        onView(first(withId(com.passbolt.mobile.android.feature.otp.R.id.more))).perform(click())
        onView(withId(com.passbolt.mobile.android.feature.resourcemoremenu.R.id.editPassword)).perform(click())
        onView(withText(LocalizationR.string.resource_update_edit_password_title)).check(matches(isDisplayed()))
        val randomizedName = "StillExpired ${java.util.UUID.randomUUID().toString().take(5)}"
        val fieldsToUpdate = EditableFieldInput.entries.filterNot {
            it.hintName == "Enter Name" || it.hintName == "Enter password"
        }
        fieldsToUpdate.forEach { field ->
            onViewInputWithHintName("Enter Name").perform(replaceText(randomizedName))
            onViewInputWithHintName(field.hintName).perform(replaceText(field.textToReplace))
        }
        onView(withId(com.passbolt.mobile.android.feature.resources.R.id.updateButton)).perform(scrollTo(), click())
        // Then   The resource is marked to expire <expire date> // look for this edited resource with randomized name
        chooseFilter(com.passbolt.mobile.android.feature.home.R.id.recentlyModified)
        onView(withId(com.passbolt.mobile.android.feature.otp.R.id.searchEditText)).perform(typeText(randomizedName))
        onView(withIndex(index = 0, withText("$randomizedName (expired)"))).perform(click())
        onView(withId(com.passbolt.mobile.android.feature.resources.R.id.expiryItem)).check(matches(isDisplayed()))
        onView(withText(containsString(" ago"))).check(matches(isDisplayed()))
    }

    private fun onViewInputWithHintName(hintName: String): ViewInteraction =
        onView(allOf(isDescendantOfA(withHint(equalTo(hintName))), withId(CoreUiR.id.input)))

    private fun chooseFilter(filter: Int) {
        onView(withId(MaterialR.id.text_input_start_icon)).perform(click())
        onView(withId(filter)).perform(click())
    }
}
