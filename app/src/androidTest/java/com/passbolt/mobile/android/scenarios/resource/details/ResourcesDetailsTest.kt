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

package com.passbolt.mobile.android.scenarios.resource.details

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import com.passbolt.mobile.android.helpers.getClipboardText
import com.passbolt.mobile.android.helpers.getString
import com.passbolt.mobile.android.helpers.searchAndOpenFirstResourceByName
import com.passbolt.mobile.android.helpers.signIn
import com.passbolt.mobile.android.instrumentationTestsModule
import com.passbolt.mobile.android.intents.ManagedAccountIntentCreator
import com.passbolt.mobile.android.rules.IdlingResourceRule
import com.passbolt.mobile.android.rules.lazyActivitySetupScenarioRule
import com.passbolt.mobile.android.scenarios.resource.TestResourceType
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
class ResourcesDetailsTest(
    private val resourceType: TestResourceType,
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

    private lateinit var clipboardManager: ClipboardManager

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

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Resource type: {0}")
        fun resourceTypes() = TestResourceType.entries

        private val EXCLUDED_ITEMS_MAP =
            mapOf(
                TestResourceType.PASSWORD_WITH_DESCRIPTION to listOf(CopyItem.COPY_METADATA_DESCRIPTION),
                TestResourceType.PASSWORD_DESCRIPTION_TOTP to listOf(CopyItem.COPY_METADATA_DESCRIPTION),
                TestResourceType.SIMPLE_PASSWORD to listOf(CopyItem.COPY_NOTE),
            )

        val EXPECTED_CLIPBOARD_VALUES =
            mapOf(
                CopyItem.COPY_URI to "https://www.passbolt.com",
                CopyItem.COPY_USERNAME to "BettyAutomate",
                CopyItem.COPY_PASSWORD to "TestPassword123!",
                CopyItem.COPY_METADATA_DESCRIPTION to "This test description is unencrypted this time",
                CopyItem.COPY_NOTE to "This is a Note which is secret",
            )

        fun getExcludedItems(resourceType: TestResourceType): List<CopyItem> = EXCLUDED_ITEMS_MAP[resourceType] ?: emptyList()
    }

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Before
    fun setup() {
        composeTestRule.apply {
            signIn(managedAccountIntentCreator.getPassphrase())
            chooseFilter(filters_menu_all_items)
        }
        clipboardManager =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext
                .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    /**
     * [As a user on the homepage, I can access the resource page for which I have full permissions.](https://passbolt.testrail.io/index.php?/cases/view/2443)
     *
     * Given   I am on the homepage
     * And     I have permission to view the password
     * When    I click on a <resource>
     * Then    I see the resource display screen
     * And     I see the resource name
     * And     I see the "Username" header
     * And     I see the "Password" header
     * And     I see the "Metadata" header
     * And     I see "Location" item
     * And     I see "Tags" item
     * And     I see "Shared with" subsection
     *
     * Examples:
     * | resource                    |
     * | Simple password             |
     * | Password with description   |
     * | Password description totp   |
     */
    @Test
    fun asAUserOnTheHomepageICanAccessTheResourcePageForWhichIHaveFullPermissions() {
        composeTestRule.apply {
            searchAndOpenFirstResourceByName(resourceType.displayName)
            onNodeWithText(resourceType.displayName).assertExists()
            onAllNodesWithText(getString(LocalizationR.string.resource_details_username_header)).onFirst().assertExists()
            onAllNodesWithText(getString(LocalizationR.string.resource_details_password_header)).onFirst().assertExists()
            onNodeWithText(getString(LocalizationR.string.resource_details_metadata_header)).assertExists()
            onNodeWithText(getString(LocalizationR.string.location)).assertExists()
            onNodeWithText(getString(LocalizationR.string.resource_details_tags_header)).assertExists()
            onNodeWithText(getString(LocalizationR.string.shared_with)).assertExists()
        }
    }

    /**
     * [As a user on the resource display I can trigger the action menu](https://passbolt.testrail.io/index.php?/cases/view/16008)
     *
     * Given   I am a user on a <resource> view page
     * When    I click on the "3 dots" icon
     * Then    I see the action menu drawer
     * And     I see "Copy URI" item
     * And     I see "Copy password" item
     * And     I see "Copy username" item
     * And     I see "Copy note" item when not on Simple Password
     * And     I see "Copy description" item when on Simple Password
     *
     *  Examples:
     *     | resource |
     *     | Simple Password |
     *     | Password with description |
     *     | Password description totp      |
     */
    @Test
    fun asAUserOnTheResourceDisplayICanTriggerTheActionMenu() {
        composeTestRule.apply {
            searchAndOpenFirstResourceByName(resourceType.displayName)
            onNodeWithTag(ResourceDetails.MORE_ICON).performClick()

            onNodeWithText(getString(LocalizationR.string.more_copy_uri)).assertExists()
            onNodeWithText(getString(LocalizationR.string.more_copy_password)).assertExists()
            onNodeWithText(getString(LocalizationR.string.more_copy_username)).assertExists()

            val excludedItems = getExcludedItems(resourceType)
            if (CopyItem.COPY_NOTE !in excludedItems) {
                onNodeWithText(getString(LocalizationR.string.more_copy_note)).assertExists()
            }
            if (CopyItem.COPY_METADATA_DESCRIPTION !in excludedItems) {
                onNodeWithText(getString(LocalizationR.string.more_copy_metadata_desc)).assertExists()
            }
        }
    }

    /**
     * [As a user on the resource display I can trigger the action menu and copy credentials to the clipboard.](https://passbolt.testrail.io/index.php?/cases/view/2459)
     *
     * Given   I am a user on a <resource> view page
     * And     I have copy password permission
     * When    I click on the "3 dots" icon
     * Then    I can "Copy URI" item
     * And     I can "Copy password" list item
     * And     I can "Copy username" list item
     * And     I can "Copy description" list item when on Simple password
     * And     I can "Copy note" list item when not on Simple password
     *
     * Examples:
     * | resource                    |
     * | Simple password             |
     * | Password with description   |
     * | Password description totp   |
     */
    @Test
    fun asALoggedInMobileUserOnTheResourceDisplayICanTriggerTheActionMenuAndCopyCredentialsToTheClipboard() {
        composeTestRule.apply {
            searchAndOpenFirstResourceByName(resourceType.displayName)

            val excludedItems = getExcludedItems(resourceType)
            val copyItems = CopyItem.entries.filter { it !in excludedItems }

            copyItems.forEach { copyItem ->
                clipboardManager.setPrimaryClip(ClipData.newPlainText("", ""))

                onNodeWithTag(ResourceDetails.MORE_ICON).performClick()
                onNodeWithText(getString(copyItem.stringResId)).performClick()

                val expectedValue = EXPECTED_CLIPBOARD_VALUES[copyItem]
                waitUntil(timeoutMillis = 5_000) {
                    val actual = getClipboardText(clipboardManager)
                    actual == expectedValue
                }
            }
        }
    }
}
