/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021,2024-2026 Passbolt SA
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

package com.passbolt.mobile.android.scenarios.resource.details.note

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.passbolt.mobile.android.core.idlingresource.ResourceDetailActionIdlingResource
import com.passbolt.mobile.android.core.idlingresource.ResourcesFullRefreshIdlingResource
import com.passbolt.mobile.android.core.idlingresource.SignInIdlingResource
import com.passbolt.mobile.android.core.localization.R.string.filters_menu_all_items
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.feature.authentication.AuthenticationMainActivity
import com.passbolt.mobile.android.helpers.chooseFilter
import com.passbolt.mobile.android.helpers.getString
import com.passbolt.mobile.android.helpers.searchAndOpenFirstResourceByName
import com.passbolt.mobile.android.helpers.signIn
import com.passbolt.mobile.android.instrumentationTestsModule
import com.passbolt.mobile.android.intents.ManagedAccountIntentCreator
import com.passbolt.mobile.android.rules.IdlingResourceRule
import com.passbolt.mobile.android.rules.lazyActivitySetupScenarioRule
import com.passbolt.mobile.android.testtags.composetags.ResourceDetails
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.koin.core.component.inject
import org.koin.test.KoinTest
import com.passbolt.mobile.android.core.localization.R as LocalizationR

@RunWith(Parameterized::class)
@MediumTest
class ResourcesNoteTest(
    private val testedResource: String,
    private val expectedNote: String,
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

    @get:Rule
    val idlingResourceRule =
        let {
            val signInIdlingResource: SignInIdlingResource by inject()
            val resourcesFullRefreshIdlingResource: ResourcesFullRefreshIdlingResource by inject()
            val resourceDetailActionIdlingResource: ResourceDetailActionIdlingResource by inject()
            IdlingResourceRule(
                arrayOf(
                    signInIdlingResource,
                    resourcesFullRefreshIdlingResource,
                    resourceDetailActionIdlingResource,
                ),
            )
        }

    private companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Resource: {0}")
        fun testData() =
            listOf(
                arrayOf(
                    "Password with description - v4",
                    "This is a Note which is secret",
                ),
                arrayOf(
                    "Password with description and long note",
                    "but may diverge in detail questions",
                ),
                arrayOf(
                    "Password, Description and TOTP - v4",
                    "This is a Note which is secret",
                ),
                arrayOf(
                    "Password, Description and TOTP with long note",
                    "but may diverge in detail questions",
                ),
                // TODO - This need to be enabled after enabling V5 on cloud's `Betty` user
//            "Default resource type",
//            "Default resource type with TOTP"
            )
    }

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Before
    fun setup() {
        composeTestRule.apply {
            signIn(managedAccountIntentCreator.getPassphrase())
            chooseFilter(filters_menu_all_items)
        }
    }

    /**
     * [As a logged in mobile user on the resource display I can show or hide the resource note ](https://passbolt.testrail.io/index.php?/cases/view/2447)
     * Given I am a mobile user on the <resource> display screen
     * When  I click on the show icon in the "Note" item list
     * And   I successfully authenticate (if needed)
     * Then  I should see the note body
     *
     * When  I click on the hide icon
     * Then  I should see the note hidden
     *
     * Examples:
     *     | resource |
     *     | Password with description |
     *     | Password description totp |
     */
    @Test
    fun asALoggedInMobileUserOnTheResourceDisplayICanShowOrHideResourceNote() {
        composeTestRule.apply {
            searchAndOpenFirstResourceByName(testedResource)

            val noteSection = hasTestTag(ResourceDetails.NOTE_SECTION)
            onNode(hasContentDescription(getString(LocalizationR.string.action_show)) and hasAnyAncestor(noteSection))
                .performClick()
            onNodeWithText(expectedNote, substring = true).assertExists()

            onNode(hasContentDescription(getString(LocalizationR.string.action_hide)) and hasAnyAncestor(noteSection))
                .performScrollTo()
                .performClick()
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText(expectedNote, substring = true)
                    .fetchSemanticsNodes()
                    .isEmpty()
            }
        }
    }
}
