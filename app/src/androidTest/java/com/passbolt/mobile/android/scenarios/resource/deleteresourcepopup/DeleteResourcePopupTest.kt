/*
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2025 Passbolt SA
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

package com.passbolt.mobile.android.scenarios.resource.deleteresourcepopup

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.passbolt.mobile.android.core.idlingresource.CreateMenuModelIdlingResource
import com.passbolt.mobile.android.core.idlingresource.CreateResourceIdlingResource
import com.passbolt.mobile.android.core.idlingresource.DeleteResourceIdlingResource
import com.passbolt.mobile.android.core.idlingresource.ResourcesFullRefreshIdlingResource
import com.passbolt.mobile.android.core.idlingresource.SignInIdlingResource
import com.passbolt.mobile.android.core.localization.R.string.are_you_sure
import com.passbolt.mobile.android.core.localization.R.string.cancel
import com.passbolt.mobile.android.core.localization.R.string.delete
import com.passbolt.mobile.android.core.localization.R.string.filters_menu_all_items
import com.passbolt.mobile.android.core.localization.R.string.more_delete
import com.passbolt.mobile.android.core.localization.R.string.resource_will_be_deleted
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.feature.authentication.AuthenticationMainActivity
import com.passbolt.mobile.android.helpers.chooseFilter
import com.passbolt.mobile.android.helpers.getString
import com.passbolt.mobile.android.helpers.searchAndClickMoreOfFirstResource
import com.passbolt.mobile.android.helpers.signIn
import com.passbolt.mobile.android.instrumentationTestsModule
import com.passbolt.mobile.android.intents.ManagedAccountIntentCreator
import com.passbolt.mobile.android.rules.IdlingResourceRule
import com.passbolt.mobile.android.rules.lazyActivitySetupScenarioRule
import com.passbolt.mobile.android.scenarios.resource.TestResourceType
import com.passbolt.mobile.android.testtags.composetags.Home
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.koin.test.KoinTest
import org.koin.test.inject

@RunWith(Parameterized::class)
@MediumTest
class DeleteResourcePopupTest(
    private val testedResource: String,
) : KoinTest {
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
    private val resourcesFullRefreshIdlingResource: ResourcesFullRefreshIdlingResource by inject()

    @get:Rule
    val idlingResourceRule =
        let {
            val signInIdlingResource: SignInIdlingResource by inject()
            val deleteIdlingResource: DeleteResourceIdlingResource by inject()
            val createResourceIdlingResource: CreateResourceIdlingResource by inject()
            val createMenuModelIdlingResource: CreateMenuModelIdlingResource by inject()
            IdlingResourceRule(
                arrayOf(
                    signInIdlingResource,
                    resourcesFullRefreshIdlingResource,
                    deleteIdlingResource,
                    createResourceIdlingResource,
                    createMenuModelIdlingResource,
                ),
            )
        }

    private companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Resource name: {0}")
        fun resourceNames() = TestResourceType.entries.map { it.displayName }
    }

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Before
    fun setup() {
        composeTestRule.apply {
            signIn(managedAccountIntentCreator.getPassphrase())
            chooseFilter(filters_menu_all_items)
            searchAndClickMoreOfFirstResource(testedResource)
            onNodeWithText(getString(more_delete)).assertIsDisplayed().performClick()
        }
    }

    /**  [On the action menu drawer, I can click delete password element when V5 resources are enabled](https://passbolt.testrail.io/index.php?/cases/view/13119)
     *
     *     Given that I am on the v5 environment
     *     And I am on the <resource>’s action menu drawer
     *     And I see ‘Delete password’ element enabled
     *     When I click ‘Delete password’
     *     Then I see a popup with ‘Are you sure?’ information
     *     And I see description of this popup
     *     And I see ‘Cancel’ button in @blue
     *     And I see ‘Delete’ button in @blue
     *
     *     Examples:
     *     | resource                       |
     *     | Simple password                |
     *     | Password with description      |
     *     | Password description totp      |
     *     | Simple Password (Deprecated)   |
     *     | Default resource type          |
     *     | Default resource type with TOTP|
     *
     */
    @Test
    fun onTheActionMenuDrawerICanClickDeletePasswordElementWhenV5ResourcesAreEnabled() {
        composeTestRule.apply {
            onNodeWithText(getString(are_you_sure)).assertIsDisplayed()
            onNodeWithText(getString(resource_will_be_deleted)).assertIsDisplayed()
            onNodeWithText(getString(cancel)).assertIsDisplayed()
            onNodeWithText(getString(delete)).assertIsDisplayed()
        }
    }

    /**
     *  [On the password removal popup, I can click cancel when v5 resources are enabled](https://passbolt.testrail.io/index.php?/cases/view/13120)
     *
     *     Given that I am on removal popup of the <resource>
     *     When I click ‘Cancel’ button
     *     Then I am back on the resource view page
     *
     *     Examples:
     *     | resource                       |
     *     | Simple password                |
     *     | Password with description      |
     *     | Password description totp      |
     *     | Simple Password (Deprecated)   |
     *     | Default resource type          |
     *     | Default resource type with TOTP|
     *
     */
    @Test
    fun testOnThePasswordRemovalPopupICanClickCancelWhenV5ResourcesAreEnabled() {
        composeTestRule.apply {
            onNodeWithText(getString(cancel)).performClick()
            onNodeWithTag(Home.SCREEN).assertIsDisplayed()
        }
    }
}
