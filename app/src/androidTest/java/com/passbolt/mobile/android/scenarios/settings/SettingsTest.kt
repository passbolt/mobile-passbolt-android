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
import androidx.annotation.IdRes
import androidx.appcompat.widget.Toolbar
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
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.isNotEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withTagValue
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.google.android.material.switchmaterial.SwitchMaterial
import com.passbolt.mobile.android.commontest.viewassertions.CastedViewAssertion
import com.passbolt.mobile.android.core.idlingresource.ResourcesFullRefreshIdlingResource
import com.passbolt.mobile.android.core.idlingresource.SignInIdlingResource
import com.passbolt.mobile.android.core.idlingresource.SignOutIdlingResource
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.feature.authentication.AuthenticationMainActivity
import com.passbolt.mobile.android.hasDrawable
import com.passbolt.mobile.android.instrumentationTestsModule
import com.passbolt.mobile.android.intents.ManagedAccountIntentCreator
import com.passbolt.mobile.android.rules.IdlingResourceRule
import com.passbolt.mobile.android.rules.lazyActivitySetupScenarioRule
import com.passbolt.mobile.android.scenarios.actions.setChecked
import com.passbolt.mobile.android.scenarios.setup.autofill.autofillConfiguredModuleTests
import com.passbolt.mobile.android.scenarios.setup.configurebiometric.biometricSetupUnavailableModuleTests
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
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR


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
        //    #MOBILE_USER_ON_SETTINGS_PAGE:
        //    Given	I am a mobile user with the application installed
        //    And	I am logged in
        //    And 	I am on Passbolt PRO/CE/Cloud
        onView(withId(CoreUiR.id.input)).perform(typeText(managedAccountIntentCreator.getUsername()))
        onView(withId(com.passbolt.mobile.android.feature.authentication.R.id.authButton)).perform(scrollTo(), click())
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
        onView(withId(com.passbolt.mobile.android.feature.settings.R.id.settingsNav)).check(matches(isDisplayed()))
        //    When      I click on the settings icon
        onView(withId(com.passbolt.mobile.android.feature.settings.R.id.settingsNav)).perform(click())
        //    Then      I am redirected on the settings page
        onView(withId(com.passbolt.mobile.android.feature.settings.R.id.settingsRoot)).check(matches(isDisplayed()))
    }

    @Test
//    https://passbolt.testrail.io/index.php?/cases/view/2438
    fun asAMobileUserOnTheMainSettingsPageICanSeeTheListOfSettingsIHaveAccessTo() {
        //    Given     that I am #MOBILE_USER_ON_SETTINGS_PAGE
        //    When      I’m staying on the Settings page
        onView(withId(com.passbolt.mobile.android.feature.settings.R.id.settingsNav)).perform(click())
        //    Then      I see the “Settings” title
        onView(withId(com.passbolt.mobile.android.feature.settings.R.id.settingsTitle)).check(matches(isDisplayed()))
        //    And       I see a "App settings" with an settings icon and a caret on the right
        //    And       I see a "Accounts" with an personas icon and a caret on the right
        //    And       I see a "Terms & licences" with an info icon and a caret on the right
        //    And       I see a "Debug, logs" with an bug icon and a caret on the right
        //    And       I see a "Sign out" with an exit icon
        val settingsItems = SettingsMenuItemModel.values()
        settingsItems.forEach { settingsItem ->
            onView(withText(settingsItem.settingsItemTextId)).perform(scrollTo()).check(matches(isDisplayed()))
            onView(
                allOf(
                    isDescendantOfA(withId(settingsItem.settingsItemId)),
                    withId(CoreUiR.id.iconImage)
                )
            )
                .check(matches(hasDrawable(id = settingsItem.settingsItemIconId, tint = CoreUiR.color.icon_tint)))
        }
    }

    @Test
    fun asAnAndroidUserICanSeeAppSettings() {
        //    Given     that I am #MOBILE_USER_ON_SETTINGS_PAGE
        onView(withId(com.passbolt.mobile.android.feature.settings.R.id.settingsNav)).perform(click())
        //    When 	    I click on the “App settings” button
        onView(withId(com.passbolt.mobile.android.feature.settings.R.id.appSettings)).perform(click())
        //    Then      I see the “App settings” title
        onView(allOf(isDescendantOfA(withId(com.passbolt.mobile.android.feature.setup.R.id.toolbar)), withText(LocalizationR.string.settings_app_settings)))
            .check(matches(isDisplayed()))
        //    And	    I see a back button to go to the main settings page
        onView(isAssignableFrom(Toolbar::class.java))
            .check(CastedViewAssertion<Toolbar> { it.navigationIcon != null })
        //    And 	    I see a Fingerprint with an fingerprint icon and a switch on the right
        //    And 	    I see a Autofill with an key icon and a caret on the right
        //    And 	    I see a Default filter with an filter icon and a caret on the right
        //    And 	    I see a Expert settings with an gear icon and a caret on the right
        AppSettingsItemModel.values().forEach { appSettingsItem ->
            onView(withText(appSettingsItem.settingsItemTextId)).perform(scrollTo()).check(matches(isDisplayed()))
            onView(allOf(isDescendantOfA(withId(appSettingsItem.settingsItemId)), withId(CoreUiR.id.iconImage)))
                .check(matches(hasDrawable(id = appSettingsItem.settingsItemIconId, tint = CoreUiR.color.icon_tint)))
        }
    }

    @Test
    fun asAnAndroidUserICanSeeExpertSettings() {
        //    Given     that I am #MOBILE_USER_ON_SETTINGS_PAGE
        onView(withId(com.passbolt.mobile.android.feature.settings.R.id.settingsNav)).perform(click())
        //    And	    I’m on the “App settings” screen
        onView(withId(com.passbolt.mobile.android.feature.settings.R.id.appSettings)).perform(click())
        //    When 	    I click on the “Expert settings” button
        onView(withId(com.passbolt.mobile.android.feature.settings.R.id.expertSettings)).perform(click())
        //    Then      I see the “Expert settings” title
        onView(allOf(isDescendantOfA(withId(com.passbolt.mobile.android.feature.setup.R.id.toolbar)), withText(LocalizationR.string.settings_app_settings_expert_settings)))
            .check(matches(isDisplayed()))
        //    And 	    I see the back button to go to the main settings page
        onView(isAssignableFrom(Toolbar::class.java))
            .check(CastedViewAssertion<Toolbar> { it.navigationIcon != null })
        //    And 	    I see a Developer mode with an nodes icon and a switch on the right
        //    And 	    I see a Hide “device is rooted” dialog with an hash icon and a switch on the right
        ExpertSettingsItemModel.values().forEach { expertSettingsItem ->
            onView(withText(expertSettingsItem.settingsItemTextId)).perform(scrollTo()).check(matches(isDisplayed()))
            onView(allOf(isDescendantOfA(withId(expertSettingsItem.settingsItemId)), withId(CoreUiR.id.iconImage)))
                .check(matches(hasDrawable(id = expertSettingsItem.settingsItemIconId, tint = CoreUiR.color.icon_tint)))
        }
    }

    @Test
    fun asAnAndroidUserICanEnableDeveloperMode() {
        //    Given     that I am #MOBILE_USER_ON_SETTINGS_PAGE
        onView(withId(com.passbolt.mobile.android.feature.settings.R.id.settingsNav)).perform(click())
        //    And	    I’m on the “Expert settings” screen
        onView(withId(com.passbolt.mobile.android.feature.settings.R.id.appSettings)).perform(click())
        onView(withId(com.passbolt.mobile.android.feature.settings.R.id.expertSettings)).perform(click())
        //    When 	    I enable the “Developer mode” switch
        onView(isChildSwitchOfSetting(com.passbolt.mobile.android.feature.settings.R.id.developerModeSetting))
            .perform(setChecked(true))
        //    Then      I see that switch is enabled
        onView(isChildSwitchOfSetting(com.passbolt.mobile.android.feature.settings.R.id.developerModeSetting))
            .check(matches(isEnabled()))
        //    And 	    I see that “Hide “device is rooted” dialog” switch is available
        onView(isChildSwitchOfSetting(com.passbolt.mobile.android.feature.settings.R.id.hideRootWarningSetting))
            .check(matches(isEnabled()))
    }

    @Test
    fun asAnAndroidUserICanDisableDeveloperMode() {
        //    Given     that I am #MOBILE_USER_ON_SETTINGS_PAGE
        onView(withId(com.passbolt.mobile.android.feature.settings.R.id.settingsNav)).perform(click())
        //    And	    I’m on the “Expert settings” screen
        onView(withId(com.passbolt.mobile.android.feature.settings.R.id.appSettings)).perform(click())
        onView(withId(com.passbolt.mobile.android.feature.settings.R.id.expertSettings)).perform(click())
        //    When 	    I disable the “Developer mode” switch
        onView(isChildSwitchOfSetting(com.passbolt.mobile.android.feature.settings.R.id.developerModeSetting))
            .perform(setChecked(false))
        //    And 	    I see that every subsequent position is unavailable
        onView(isChildSwitchOfSetting(com.passbolt.mobile.android.feature.settings.R.id.hideRootWarningSetting))
            .check(matches(isNotEnabled()))
    }

    @Test
    fun asAnAndroidUserICanHideDeviceIsRootedDialog() {
        //    Given     that I am #MOBILE_USER_ON_SETTINGS_PAGE
        onView(withId(com.passbolt.mobile.android.feature.settings.R.id.settingsNav)).perform(click())
        //    And	    I’m on the “Expert settings” screen
        onView(withId(com.passbolt.mobile.android.feature.settings.R.id.appSettings)).perform(click())
        onView(withId(com.passbolt.mobile.android.feature.settings.R.id.expertSettings)).perform(click())
        //    When 	    I enable the “Hide “device is rooted” dialog” switch
        onView(isChildSwitchOfSetting(com.passbolt.mobile.android.feature.settings.R.id.developerModeSetting))
            .perform(setChecked(true))
        onView(isChildSwitchOfSetting(com.passbolt.mobile.android.feature.settings.R.id.hideRootWarningSetting))
            .perform(setChecked(true))
    }

    @Test
    fun asAMobileUserICanSeeAccounts() {
        //    Given     that I am #MOBILE_USER_ON_SETTINGS_PAGE
        //    When 	    I click on the “Accounts” button
        //    Then      I see the “Accounts” title
        //    And 	    I see the back button to go to the main settings page
        onView(withId(com.passbolt.mobile.android.feature.settings.R.id.settingsNav)).perform(click())
        //    And 	    I see a Manage accounts with an personas icon and a caret on the right
        onView(withId(com.passbolt.mobile.android.feature.settings.R.id.accountsSettings)).perform(click())
        onView(withId(com.passbolt.mobile.android.feature.settings.R.id.manageAccountsSetting)).perform(click())
        //    And 	    I see a Transfer account to another device with an lorry icon and a caret on the right
        onView(withText(LocalizationR.string.accounts_list_manage_accounts)).check(matches(isDisplayed()))
        onView(withId(com.passbolt.mobile.android.feature.otp.R.id.recyclerView)).check(matches(isDisplayed()))
    }

    @Test
    fun asALoggedInMobileUserOnTheSettingsPageAndBiometricIsDisabledOnMyDeviceICanEnableBiometrics() {
        //    Given     that I am a mobile user with the application installed
        //    And       I completed the login step
        //    And       Biometric is disabled on my device
        //    And       I am on the settings page
        onView(withId(com.passbolt.mobile.android.feature.settings.R.id.settingsNav)).perform(click())
        onView(withId(com.passbolt.mobile.android.feature.settings.R.id.appSettings)).perform(click())
        //    When      I switch on the biometrics toggle button
        onView(
            withTagValue(
                `is`(
                    getInstrumentation().targetContext.resources.getString(LocalizationR.string.settings_app_settings_fingerprint)
                )
            )
        ).perform(
            click()
        )
        //    Then      I am prompted to configure biometrics in the device settings
        onView(withText(LocalizationR.string.settings_add_first_fingerprint_title)).check(matches(isDisplayed()))
        //    And       I see a “Cancel” button to go back to the previous state
        onView(withText(LocalizationR.string.cancel)).check(matches(isDisplayed()))
        //    And       I see a “Go to settings” button to configure biometrics
        onView(withText(LocalizationR.string.dialog_encourage_autofill_go_to_settings)).check(matches(isDisplayed()))
    }

    @Test
    fun asALoggedInAndroidUserOnTheSettingsPageICanEnableAutofill() {
        //    Given     that I am a mobile user with the application installed
        //    And       the Passbolt application is already opened
        //    And       I completed the login step
        //    And       Autofill is enabled
        //    And       I am on the settings page
        onView(withId(com.passbolt.mobile.android.feature.settings.R.id.settingsNav)).perform(click())
        onView(withId(com.passbolt.mobile.android.feature.settings.R.id.appSettings)).perform(click())
        //    When      I click on the “Autofill” list item
        onView(withId(com.passbolt.mobile.android.feature.settings.R.id.autofillSetting)).perform(click())
        onView(withId(com.passbolt.mobile.android.feature.settings.R.id.autofillServiceSwitchContainer)).perform(click())
        //    Then      I see the “Passbolt Autofill enabled!” screen
        onView(withText(LocalizationR.string.dialog_autofill_enabled_title)).check(matches(isDisplayed()))
    }

    @Test
    fun asAnAndroidUserICanSeeTermsAndLicences() {
        //    Given     that I am #MOBILE_USER_ON_SETTINGS_PAGE
        //    Then      I see the “Terms & licences” title
        onView(withId(com.passbolt.mobile.android.feature.settings.R.id.settingsNav)).perform(click())
        //    When 	    I click on the “Terms & licences” button
        onView(withId(com.passbolt.mobile.android.feature.settings.R.id.termsAndLicensesSettings)).perform(click())
        onView(withId(com.passbolt.mobile.android.feature.settings.R.id.termsAndConditionsSetting)).perform(click())
        //    And 	    I see the back button to go to the main settings page
        //    And 	    I see a Terms & Conditions with an info icon and a caret on the right
        //    And 	    I see a Privacy policy with an lock icon and a caret on the right
        //    And 	    I see a Open Source Licences with an feather icon and a caret on the right
        val expectedIntent: Matcher<Intent> = allOf(hasAction(Intent.ACTION_VIEW), hasData("passbolt.com/terms"))
        intending(expectedIntent).respondWith(ActivityResult(0, null))
    }

    @Test
    fun asALoggedInMobileUserOnTheSettingsPageICanOpenThePrivacyPolicePage() {
        //    Given     that I am a mobile user with the application installed
        //    And       the Passbolt application is already opened
        //    And       I completed the login step
        //    And       I am on the settings page
        onView(withId(com.passbolt.mobile.android.feature.settings.R.id.settingsNav)).perform(click())
        onView(withId(com.passbolt.mobile.android.feature.settings.R.id.termsAndLicensesSettings)).perform(click())
        //    When      I click on “Privacy policy” list item
        onView(withId(com.passbolt.mobile.android.feature.settings.R.id.privacyPolicySetting)).perform(click())
        //    Then      I see a “Privacy policy” page (as a web page)
        val expectedIntent: Matcher<Intent> = allOf(hasAction(Intent.ACTION_VIEW), hasData("passbolt.com/privacy"))
        intending(expectedIntent).respondWith(ActivityResult(0, null))
    }

    @Test
    fun asAnAndroidUserISeeDebugLogs() {
        //    Given     that I am #MOBILE_USER_ON_SETTINGS_PAGE
        onView(withId(com.passbolt.mobile.android.feature.settings.R.id.settingsNav)).perform(click())
        //    When 	    I click on the “Debug, logs” button
        onView(withId(com.passbolt.mobile.android.feature.settings.R.id.debugLogsSettings)).perform(click())
        //    Then      I see the “Debug, logs” title
        onView(allOf(isDescendantOfA(withId(com.passbolt.mobile.android.feature.setup.R.id.toolbar)), withText(LocalizationR.string.settings_debug_logs)))
            .check(matches(isDisplayed()))
        //    And 	    I see the back button to go to the main settings page
        onView(isAssignableFrom(Toolbar::class.java))
            .check(CastedViewAssertion<Toolbar> { it.navigationIcon != null })
        //    And 	    I see a Enable debug logs with an bug icon and a switch on the right
        //    And 	    I see a Access the logs with an sheet icon and a caret on the right
        //    And 	    I see a Visit help site with an chain icon and a caret on the right
        DebugLogsItemModel.values().forEach { debugLogsSettingsItem ->
            onView(withText(debugLogsSettingsItem.settingsItemTextId)).perform(scrollTo()).check(matches(isDisplayed()))
            onView(allOf(isDescendantOfA(withId(debugLogsSettingsItem.settingsItemId)), withId(CoreUiR.id.iconImage)))
                .check(matches(hasDrawable(id = debugLogsSettingsItem.settingsItemIconId, tint = CoreUiR.color.icon_tint)))
        }
    }

    @Test
    fun asALoggedInMobileUserOnTheSettingsPageICanSignOut() {
        //    Given     that I am a mobile user with the application installed
        //    And       the Passbolt application is already opened
        //    And       I completed the login step
        //    And       I am on the settings page
        onView(withId(com.passbolt.mobile.android.feature.settings.R.id.settingsNav)).perform(click())
        //    When      I click on the “Sign out” list item
        onView(withId(com.passbolt.mobile.android.feature.settings.R.id.signOut)).perform(scrollTo(), click())
        //    Then      I see an confirmation modal
        onView(withText(LocalizationR.string.are_you_sure)).check(matches(isDisplayed()))
        onView(withText(LocalizationR.string.logout_dialog_message)).check(matches(isDisplayed()))
        //    And       I see a sign out button
        onView(withText(LocalizationR.string.common_sign_out)).check(matches(isDisplayed()))
        //    And       I see a cancel button
        onView(withText(LocalizationR.string.cancel)).check(matches(isDisplayed()))
        //    When      I click on the "Sign out" button
        onView(withText(LocalizationR.string.common_sign_out)).inRoot(isDialog()).perform(click())
        //    Then      I see the “Sign in - List of accounts” welcome screen
        onView(withText(LocalizationR.string.accounts_list_title)).check(matches(isDisplayed()))
    }

    @Test
    fun asALoggedInMobileUserOnTheSettingsPageINeedToConfirmSignOut() {
        //    Given     that I am a mobile user with the application installed
        //    And       I am on the settings page
        onView(withId(com.passbolt.mobile.android.feature.settings.R.id.settingsNav)).perform(click())
        //    When      I click on the “Sign out” list item
        onView(withId(com.passbolt.mobile.android.feature.settings.R.id.signOut)).perform(scrollTo(), click())
        //    Then      I see an confirmation modal
        onView(withText(LocalizationR.string.are_you_sure)).check(matches(isDisplayed()))
        onView(withText(LocalizationR.string.logout_dialog_message)).check(matches(isDisplayed()))
        //    When      I click "Cancel" button
        onView(withText(LocalizationR.string.cancel)).inRoot(isDialog()).perform(click())
        //    Then      I do not see the modal
        //    And       I am not signed out
        onView(withId(com.passbolt.mobile.android.feature.settings.R.id.signOut)).check(matches(isDisplayed()))
    }

    private fun isChildSwitchOfSetting(@IdRes id: Int) = allOf(
        isDescendantOfA(withId(id)),
        isAssignableFrom(SwitchMaterial::class.java)
    )
}
