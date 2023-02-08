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

package com.passbolt.mobile.android.scenarios.settings

import android.app.Instrumentation.ActivityResult
import android.content.Intent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withTagValue
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.passbolt.mobile.android.core.idlingresource.ResourcesFullRefreshIdlingResource
import com.passbolt.mobile.android.core.idlingresource.SignInIdlingResource
import com.passbolt.mobile.android.core.idlingresource.SignOutIdlingResource
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.feature.authentication.AuthenticationMainActivity
import com.passbolt.mobile.android.feature.main.R
import com.passbolt.mobile.android.hasDrawable
import com.passbolt.mobile.android.instrumentationTestsModule
import com.passbolt.mobile.android.intents.ManagedAccountIntentCreator
import com.passbolt.mobile.android.rules.IdlingResourceRule
import com.passbolt.mobile.android.rules.lazyActivitySetupScenarioRule
import com.passbolt.mobile.android.scenarios.setupautofill.autofillConfiguredModuleTests
import com.passbolt.mobile.android.scenarios.setupconfigurebiometric.biometricSetupUnavailableModuleTests
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Matcher
import org.hamcrest.Matchers.`is`
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.component.inject
import org.koin.test.KoinTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest


@RunWith(AndroidJUnit4::class)
@LargeTest
class SettingsTest : KoinTest {

    @get:Rule
    val startUpActivityRule = lazyActivitySetupScenarioRule<AuthenticationMainActivity>(
        koinOverrideModules = listOf(
            instrumentationTestsModule,
            biometricSetupUnavailableModuleTests,
            autofillConfiguredModuleTests
        ),
        intentSupplier = {
            ActivityIntents.authentication(
                getInstrumentation().targetContext,
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
        val signOutIdlingResource: SignOutIdlingResource by inject()
        val resourcesFullRefreshIdlingResource: ResourcesFullRefreshIdlingResource by inject()
        IdlingResourceRule(arrayOf(signInIdlingResource, resourcesFullRefreshIdlingResource, signOutIdlingResource))
    }

    @BeforeTest
    fun setup() {
        onView(withId(R.id.input)).perform(typeText(managedAccountIntentCreator.getUsername()))
        onView(withId(R.id.authButton)).perform(scrollTo(), click())
        Intents.init()
    }

    @AfterTest
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun asALoggedInMobileUserOnTheHomepageICanAccessTheSettingsPage() {
        //    Given     that I am a mobile user with the application installed
        //    And       the Passbolt application is already opened
        //    And       I completed the login step
        //    And       I am on the homepage
        //    And       I see a setting icon at the bottom
        onView(withId(R.id.settingsNav)).check(matches(isDisplayed()))
        //    When      I click on the settings icon
        onView(withId(R.id.settingsNav)).perform(click())
        //    Then      I am redirected on the settings page
        onView(withId(R.id.settingsRoot)).check(matches(isDisplayed()))
    }

    @Test
    fun asALoggedInMobileUserOnTheSettingsPageICanSeeTheListOfSettingsIHaveAccessTo() {
        //    Given     that I am a mobile user with the application installed
        //    And       I completed the login step
        //    When      I access the settings page
        onView(withId(R.id.settingsNav)).perform(click())
        //    Then      I see the “Settings” title
        onView(withId(R.id.settingsTitle)).check(matches(isDisplayed()))
        //    And       I see a { "biometric", "Autofill", "Manage accounts", "Terms & Conditions",
        //              "Privacy policy", "Enable debug logs", "Access the logs", "Open Source Licenses",
        //              "Sign out" } list item with a icon
        val settingsItems = SettingsMenuItemModel.values()
        settingsItems.forEach { settingsItem ->
            onView(withText(settingsItem.settingsItemTextId)).perform(scrollTo()).check(matches(isDisplayed()))
            onView(
                allOf(
                    isDescendantOfA(withId(settingsItem.settingsItemId)),
                    withId(R.id.iconImage)
                )
            )
                .check(matches(hasDrawable(id = settingsItem.settingsItemIconId, tint = R.color.icon_tint)))
        }
    }

    @Test
    fun asALoggedInMobileUserOnTheSettingsPageICanSwitchAccount() {
        //    Given     that I am a mobile user with the application installed
        //    And       I have one or more accounts configured
        //    And       the Passbolt application is already opened
        //    And       I am on the settings page
        onView(withId(R.id.settingsNav)).perform(click())
        //    When      I click on the “Manage accounts” list item
        onView(withId(R.id.accountsSettings)).perform(click())
        onView(withId(R.id.manageAccountsSetting)).perform(click())
        //    Then      I see the “List of accounts” screen
        onView(withText(R.string.accounts_list_manage_accounts)).check(matches(isDisplayed()))
        onView(withId(R.id.recyclerView)).check(matches(isDisplayed()))
    }

    @Test
    fun asALoggedInMobileUserOnTheSettingsPageAndBiometricIsDisabledOnMyDeviceICanEnableBiometrics() {
        //    Given     that I am a mobile user with the application installed 
        //    And       I completed the login step 
        //    And       Biometric is disabled on my device 
        //    And       I am on the settings page
        onView(withId(R.id.settingsNav)).perform(click())
        onView(withId(R.id.appSettings)).perform(click())
        //    When      I switch on the biometrics toggle button
        onView(
            withTagValue(
                `is`(
                    getInstrumentation().targetContext.resources.getString(R.string.settings_app_settings_fingerprint)
                )
            )
        ).perform(
            click()
        )
        //    Then      I am prompted to configure biometrics in the device settings
        onView(withText(R.string.settings_add_first_fingerprint_title)).check(matches(isDisplayed()))
        //    And       I see a “Cancel” button to go back to the previous state
        onView(withText(R.string.cancel)).check(matches(isDisplayed()))
        //    And       I see a “Go to settings” button to configure biometrics
        onView(withText(R.string.dialog_encourage_autofill_go_to_settings)).check(matches(isDisplayed()))
    }

    @Test
    fun asALoggedInAndroidUserOnTheSettingsPageICanEnableAutofill() {
        //    Given     that I am a mobile user with the application installed
        //    And       the Passbolt application is already opened
        //    And       I completed the login step
        //    And       Autofill is enabled
        //    And       I am on the settings page
        onView(withId(R.id.settingsNav)).perform(click())
        onView(withId(R.id.appSettings)).perform(click())
        //    When      I click on the “Autofill” list item
        onView(withId(R.id.autofillSetting)).perform(click())
        onView(withId(R.id.autofillServiceSwitchContainer)).perform(click())
        //    Then      I see the “Passbolt Autofill enabled!” screen
        onView(withText(R.string.dialog_autofill_enabled_title)).check(matches(isDisplayed()))
    }

    @Test
    fun asALoggedInMobileUserOnTheSettingsPageICanOpenTheTermsAndConditionsPage() {
        //    Given     that I am a mobile user with the application installed
        //    And       the Passbolt application is already opened
        //    And       I completed the login step
        //    And       I am on the settings page
        onView(withId(R.id.settingsNav)).perform(click())
        //    When      I click on “Terms & Conditions” list item
        onView(withId(R.id.termsAndLicensesSettings)).perform(click())
        onView(withId(R.id.termsAndConditionsSetting)).perform(click())
        //    Then      I see a “Terms & Conditions” page (as web page)
        val expectedIntent: Matcher<Intent> = allOf(hasAction(Intent.ACTION_VIEW), hasData("passbolt.com/terms"))
        intending(expectedIntent).respondWith(ActivityResult(0, null))
    }

    @Test
    fun asALoggedInMobileUserOnTheSettingsPageICanOpenThePrivacyPolicePage() {
        //    Given     that I am a mobile user with the application installed
        //    And       the Passbolt application is already opened
        //    And       I completed the login step
        //    And       I am on the settings page
        onView(withId(R.id.settingsNav)).perform(click())
        onView(withId(R.id.termsAndLicensesSettings)).perform(click())
        //    When      I click on “Privacy policy” list item
        onView(withId(R.id.privacyPolicySetting)).perform(click())
        //    Then      I see a “Privacy policy” page (as a web page)
        val expectedIntent: Matcher<Intent> = allOf(hasAction(Intent.ACTION_VIEW), hasData("passbolt.com/privacy"))
        intending(expectedIntent).respondWith(ActivityResult(0, null))
    }

    @Test
    fun asALoggedInMobileUserOnTheSettingsPageICanSignOut() {
        //    Given     that I am a mobile user with the application installed
        //    And       the Passbolt application is already opened
        //    And       I completed the login step
        //    And       I am on the settings page
        onView(withId(R.id.settingsNav)).perform(click())
        //    When      I click on the “Sign out” list item
        onView(withId(R.id.signOut)).perform(scrollTo(), click())
        //    Then      I see an confirmation modal
        onView(withText(R.string.are_you_sure)).check(matches(isDisplayed()))
        onView(withText(R.string.logout_dialog_message)).check(matches(isDisplayed()))
        //    And       I see a sign out button
        onView(withText(R.string.common_sign_out)).check(matches(isDisplayed()))
        //    And       I see a cancel button
        onView(withText(R.string.cancel)).check(matches(isDisplayed()))
        //    When      I click on the "Sign out" button
        onView(withText(R.string.common_sign_out)).inRoot(isDialog()).perform(click())
        //    Then      I see the “Sign in - List of accounts” welcome screen
        onView(withText(R.string.accounts_list_title)).check(matches(isDisplayed()))
    }

    @Test
    fun asALoggedInMobileUserOnTheSettingsPageINeedToConfirmSignOut() {
        //    Given     that I am a mobile user with the application installed
        //    And       I am on the settings page
        onView(withId(R.id.settingsNav)).perform(click())
        //    When      I click on the “Sign out” list item
        onView(withId(R.id.signOut)).perform(scrollTo(), click())
        //    Then      I see an confirmation modal
        onView(withText(R.string.are_you_sure)).check(matches(isDisplayed()))
        onView(withText(R.string.logout_dialog_message)).check(matches(isDisplayed()))
        //    When      I click "Cancel" button
        onView(withText(R.string.cancel)).inRoot(isDialog()).perform(click())
        //    Then      I do not see the modal
        //    And       I am not signed out
        onView(withId(R.id.signOut)).check(matches(isDisplayed()))
    }
}
