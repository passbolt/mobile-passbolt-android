/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2024-2026 Passbolt SA
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

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.passbolt.mobile.android.core.idlingresource.CreateResourceIdlingResource
import com.passbolt.mobile.android.core.idlingresource.ResourcesFullRefreshIdlingResource
import com.passbolt.mobile.android.core.idlingresource.SignInIdlingResource
import com.passbolt.mobile.android.core.idlingresource.UpdateResourceIdlingResource
import com.passbolt.mobile.android.core.localization.R.string.filters_menu_expiry
import com.passbolt.mobile.android.core.localization.R.string.filters_menu_recently_modified
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.feature.authentication.AuthenticationMainActivity
import com.passbolt.mobile.android.helpers.chooseFilter
import com.passbolt.mobile.android.helpers.createNewPasswordFromHomeScreen
import com.passbolt.mobile.android.helpers.getString
import com.passbolt.mobile.android.helpers.searchAndClickMoreOfFirstResource
import com.passbolt.mobile.android.helpers.searchAndOpenFirstResourceByName
import com.passbolt.mobile.android.helpers.signIn
import com.passbolt.mobile.android.helpers.waitForHomeScreen
import com.passbolt.mobile.android.helpers.waitForResourceForm
import com.passbolt.mobile.android.instrumentationTestsModule
import com.passbolt.mobile.android.intents.ManagedAccountIntentCreator
import com.passbolt.mobile.android.rules.IdlingResourceRule
import com.passbolt.mobile.android.rules.lazyActivitySetupScenarioRule
import com.passbolt.mobile.android.testtags.composetags.Home
import com.passbolt.mobile.android.testtags.composetags.ResourceForm
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.component.inject
import org.koin.test.KoinTest
import com.passbolt.mobile.android.core.localization.R as LocalizationR

@RunWith(AndroidJUnit4::class)
@MediumTest
class UpdateExpiryTest : KoinTest {
    @get:Rule(order = 0)
    val startUpActivityRule =
        lazyActivitySetupScenarioRule<AuthenticationMainActivity>(
            koinOverrideModules = listOf(instrumentationTestsModule),
            intentSupplier = {
                ActivityIntents.authentication(
                    InstrumentationRegistry.getInstrumentation().targetContext,
                    ActivityIntents.AuthConfig.Startup,
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
            val updateResourceIdlingResource: UpdateResourceIdlingResource by inject()
            val createResourceIdlingResource: CreateResourceIdlingResource by inject()
            val resourcesFullRefreshIdlingResource: ResourcesFullRefreshIdlingResource by inject()

            IdlingResourceRule(
                arrayOf(
                    signInIdlingResource,
                    resourcesFullRefreshIdlingResource,
                    updateResourceIdlingResource,
                    createResourceIdlingResource,
                ),
            )
        }

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Before
    fun setup() {
        composeTestRule.apply {
            signIn(managedAccountIntentCreator.getPassphrase())
            chooseFilter(filters_menu_expiry)
        }
    }

    /**  [Update expiry of a resource when secret has changed](https://passbolt.testrail.io/index.php?/cases/view/11937)
     *
     *      Given   I am logged in as a Pro or Cloud user
     *      And     automatic expiry is enabled on the server (7 days)
     *      When    I edit the password of a resource
     *      Then    The resource expiry is reset to expire after 7 days
     */
    @Test
    @Ignore("Not working yet - after create resource cannot be searched")
    fun updateExpiryOfAResourceWhenSecretHasChanged() {
        composeTestRule.apply {
            // create a resource for future use - it will expire in 7 days
            createNewPasswordFromHomeScreen(RESOURCE_NAME)

            // open more menu and click edit
            searchAndClickMoreOfFirstResource(RESOURCE_NAME)
            onNodeWithText(getString(LocalizationR.string.more_edit)).performClick()
            waitForResourceForm()

            // edit the password and save
            onNodeWithTag(ResourceForm.PASSWORD_INPUT).performTextReplacement("UpdatedForExpiryTest")
            onNodeWithTag(ResourceForm.SAVE_BUTTON).performClick()

            waitForHomeScreen()
            onNodeWithTag(Home.SCREEN).assertExists()

            // verify expiry is shown on the resource details
            chooseFilter(filters_menu_recently_modified)
            searchAndOpenFirstResourceByName(RESOURCE_NAME)
            onNodeWithText(getString(LocalizationR.string.resource_details_expiry_header)).assertExists()
            onNodeWithText("In 7 days").assertExists()
        }
    }

    /**  [Do not update expiry of a resource when all items except password have changed](https://passbolt.testrail.io/index.php?/cases/view/11938)
     *
     *      Given   I am logged in as a Pro or Cloud user
     *      And     automatic expiry is enabled on the server (7 days)
     *      And     the resource is set to expire in the past
     *      When    I edit a resource omitting the password
     *      Then    The resource expiry date is not updated (still expired)
     */
    @Test
    fun doNotUpdateExpiryOfAResourceWhenAllItemsExceptPasswordHasChanged() {
        composeTestRule.apply {
            // search for an already expired resource
            searchAndClickMoreOfFirstResource(EXPIRED_RESOURCE_NAME)
            onNodeWithText(getString(LocalizationR.string.more_edit)).performClick()
            waitForResourceForm()

            // edit only non-password fields
            val randomizedName = "StillExpired ${java.util.UUID.randomUUID().toString().take(5)}"
            onNodeWithTag(ResourceForm.NAME_INPUT).performTextReplacement(randomizedName)
            onNodeWithTag(ResourceForm.URI_INPUT).performTextReplacement("UpdatedURL")
            onNodeWithTag(ResourceForm.USERNAME_INPUT).performTextReplacement("UpdatedUsername")
            onNodeWithTag(ResourceForm.SAVE_BUTTON).performClick()

            waitForHomeScreen()
            onNodeWithTag(Home.SCREEN).assertExists()

            // verify the resource still shows as expired
            chooseFilter(filters_menu_recently_modified)
            searchAndOpenFirstResourceByName(randomizedName)
            onNodeWithText(getString(LocalizationR.string.resource_details_expiry_header)).assertExists()
        }
    }

    private companion object {
        private const val RESOURCE_NAME = "ExpiringResource"
        private const val EXPIRED_RESOURCE_NAME = "Expired"
    }
}
