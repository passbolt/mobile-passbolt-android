/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021-2026 Passbolt SA
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

package com.passbolt.mobile.android.scenarios.setup.passphrase

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.passbolt.mobile.android.core.idlingresource.ResourcesFullRefreshIdlingResource
import com.passbolt.mobile.android.core.idlingresource.SignInIdlingResource
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.core.ui.text.PasswordInputTestTags
import com.passbolt.mobile.android.core.ui.topbar.BackNavigationIcon.TestTags.ICON
import com.passbolt.mobile.android.feature.authentication.AuthenticationMainActivity
import com.passbolt.mobile.android.helpers.getString
import com.passbolt.mobile.android.instrumentationTestsModule
import com.passbolt.mobile.android.intents.ManagedAccountIntentCreator
import com.passbolt.mobile.android.mappers.AccountModelMapper
import com.passbolt.mobile.android.rules.IdlingResourceRule
import com.passbolt.mobile.android.rules.lazyActivitySetupScenarioRule
import com.passbolt.mobile.android.testtags.composetags.Auth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.KoinTest
import org.koin.test.inject
import com.passbolt.mobile.android.core.localization.R as LocalizationR

@RunWith(AndroidJUnit4::class)
@MediumTest
class SetupPassphraseTest : KoinTest {
    @get:Rule(order = 0)
    val startUpActivityRule =
        lazyActivitySetupScenarioRule<AuthenticationMainActivity>(
            koinOverrideModules = listOf(instrumentationTestsModule),
            intentSupplier = {
                ActivityIntents.authentication(
                    InstrumentationRegistry.getInstrumentation().targetContext,
                    ActivityIntents.AuthConfig.Setup,
                    AppContext.APP,
                    managedAccountIntentCreator.getUserLocalId(),
                )
            },
        )

    private val managedAccountIntentCreator: ManagedAccountIntentCreator by inject()

    @get:Rule
    val idlingResourceRule =
        let {
            val signInIdlingResource: SignInIdlingResource by inject()
            val resourcesFullRefreshIdlingResource: ResourcesFullRefreshIdlingResource by inject()
            IdlingResourceRule(arrayOf(signInIdlingResource, resourcesFullRefreshIdlingResource))
        }

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    /**  [As a mobile user I should see the enter my passphrase screen after I successfully scanned QR codes](https://passbolt.testrail.io/index.php?/cases/view/2349)
     *
     *      Given  the user is on the "Success feedback" screen at the end of the QR code scanning process
     *      When   the user clicks the "Continue" button
     *      Then   an "Sign In" page is presented
     *      And    a back arrow button is presented
     *      And    current user's name is presented
     *      And    current user's email is presented
     *      And    the url of the server is presented
     *      And    current user's avatar or the default avatar is presented
     *      And    a passphrase input field is presented
     *      And    an eye icon to toggle passphrase visibility is presented
     *      And    a sign in the primary action button is presented
     *      And    “I forgot my passphrase” link is presented
     */
    @Test
    fun asAMobileUserIShouldSeeTheEnterMyPassphraseScreenAfterISuccessfullyScannedQrCodes() {
        composeTestRule.onNode(hasTestTag(ICON), useUnmergedTree = true).assertExists()
        val firstName = managedAccountIntentCreator.getFirstName()
        val lastName = managedAccountIntentCreator.getLastName()
        composeTestRule.onNodeWithText(AccountModelMapper.defaultLabel(firstName, lastName)).assertIsDisplayed()
        composeTestRule.onNodeWithText(managedAccountIntentCreator.getUsername()).assertIsDisplayed()
        composeTestRule.onNodeWithText(managedAccountIntentCreator.getDomain()).assertIsDisplayed()
        composeTestRule.onNodeWithTag(Auth.AVATAR).assertIsDisplayed()
        composeTestRule.onNodeWithTag(Auth.PASSPHRASE_INPUT).assertIsDisplayed()
        composeTestRule.onNodeWithText(getString(LocalizationR.string.auth_enter_passphrase)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(Auth.SIGN_IN_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithText(getString(LocalizationR.string.auth_forgot_password_button)).assertIsDisplayed()
    }

    /**  [As a mobile user I can preview my passphrase](https://passbolt.testrail.io/index.php?/cases/view/2353)
     *
     *      Given   I am on the "Enter your passphrase" page
     *      And     there is some <initial text> inside the passphrase field
     *      When    I click the "eye" button inside the passphrase field
     *      Then    I see the content of the passphrase field in <output format>
     *      And     I see the plain text uses Inconsolata (monospace) font (not testable via Compose test APIs)
     *
     *      | initial text | output format |
     *      |--------------|---------------|
     *      | hidden text  | plain text    |
     *      | plain text   | hidden text   |
     */
    @Test
    fun asAMobileUserICanPreviewMyPassphrase() {
        composeTestRule.onNodeWithTag(Auth.PASSPHRASE_INPUT).performTextReplacement(PREVIEW_PASSPHRASE)
        composeTestRule
            .onNodeWithTag(PasswordInputTestTags.VISIBILITY_TOGGLE, useUnmergedTree = true)
            .performClick()
        composeTestRule.onNodeWithTag(Auth.PASSPHRASE_INPUT).assertTextEquals(PREVIEW_PASSPHRASE)
    }

    /**  [As a mobile user I can see a feedback message if I entered the wrong passphrase](https://passbolt.testrail.io/index.php?/cases/view/2354)
     *
     *      Given   I am on the "Enter your passphrase" page
     *      When    I submit a wrong passphrase
     *      Then    I see a toast notification with error message
     *      And     the toast is at the bottom of the screen (sidenote: colors and positions aren't accessible in the semantics tree)
     *      And     the toast is in red
     *      And     the input and label are still in the same colours
     *      And     the message says "Incorrect passphrase or decryption error. Please try again."
     */
    @Test
    fun asAMobileUserICanSeeAFeedbackMessageIfIEnteredTheWrongPassphrase() {
        composeTestRule.onNodeWithTag(Auth.PASSPHRASE_INPUT).performTextReplacement("wrongPass1!@")
        composeTestRule.onNodeWithTag(Auth.SIGN_IN_BUTTON).performClick()
        composeTestRule.onNodeWithText(getString(LocalizationR.string.auth_incorrect_passphrase)).assertIsDisplayed()
    }

    /**  [As a mobile user I can get some help if I forgot my passphrase](https://passbolt.testrail.io/index.php?/cases/view/2352)
     *
     *      Given   I am on the "Enter your passphrase" page
     *      When    I click the "forgot my passphrase" link
     *      Then    I see a dialog with a help
     *      And     help text says the setup process can't be completed without a passphrase
     *      And     I see a message telling me to contact my administrator
     *      And     a "Got it" button to close the dialog is presented
     *      And     a "Got it" button is clickable
     */
    @Test
    fun asAMobileUserICanGetSomeHelpIfIForgotMyPassphrase() {
        composeTestRule.onNodeWithText(getString(LocalizationR.string.auth_forgot_password_button)).performClick()
        composeTestRule.onNodeWithText(getString(LocalizationR.string.auth_forgot_password_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(getString(LocalizationR.string.auth_forgot_password_message)).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(getString(LocalizationR.string.got_it))
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    private companion object {
        private const val PREVIEW_PASSPHRASE = "SomeRandomText"
    }
}
