/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021,2024-2025 Passbolt SA
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
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.passbolt.mobile.android.core.idlingresource.ResourceDetailActionIdlingResource
import com.passbolt.mobile.android.core.idlingresource.ResourcesFullRefreshIdlingResource
import com.passbolt.mobile.android.core.idlingresource.SignInIdlingResource
import com.passbolt.mobile.android.core.localization.R.string.filters_menu_all_items
import com.passbolt.mobile.android.core.localization.R.string.location
import com.passbolt.mobile.android.core.localization.R.string.resource_details_metadata_header
import com.passbolt.mobile.android.core.localization.R.string.resource_details_password_header
import com.passbolt.mobile.android.core.localization.R.string.resource_details_tags_header
import com.passbolt.mobile.android.core.localization.R.string.resource_details_url_header
import com.passbolt.mobile.android.core.localization.R.string.resource_details_username_header
import com.passbolt.mobile.android.core.localization.R.string.shared_with
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.core.ui.R.color.icon_tint
import com.passbolt.mobile.android.core.ui.R.drawable.ic_arrow_left
import com.passbolt.mobile.android.core.ui.R.drawable.ic_copy
import com.passbolt.mobile.android.core.ui.R.drawable.ic_eye_visible
import com.passbolt.mobile.android.core.ui.R.drawable.ic_more
import com.passbolt.mobile.android.core.ui.R.id.actionIcon
import com.passbolt.mobile.android.feature.authentication.AuthenticationMainActivity
import com.passbolt.mobile.android.feature.autofill.R.id.close
import com.passbolt.mobile.android.feature.resources.R.id.backArrow
import com.passbolt.mobile.android.feature.resources.R.id.locationHeader
import com.passbolt.mobile.android.feature.resources.R.id.metadataSectionTitle
import com.passbolt.mobile.android.feature.resources.R.id.moreIcon
import com.passbolt.mobile.android.feature.resources.R.id.name
import com.passbolt.mobile.android.feature.resources.R.id.passwordItem
import com.passbolt.mobile.android.feature.resources.R.id.passwordSectionTitle
import com.passbolt.mobile.android.feature.resources.R.id.sharedWithLabel
import com.passbolt.mobile.android.feature.resources.R.id.tagsHeader
import com.passbolt.mobile.android.feature.resources.R.id.urlItem
import com.passbolt.mobile.android.feature.resources.R.id.usernameItem
import com.passbolt.mobile.android.feature.setup.R.id.icon
import com.passbolt.mobile.android.feature.setup.R.id.title
import com.passbolt.mobile.android.helpers.chooseFilter
import com.passbolt.mobile.android.helpers.getClipboardText
import com.passbolt.mobile.android.helpers.pickFirstResourceWithName
import com.passbolt.mobile.android.helpers.signIn
import com.passbolt.mobile.android.instrumentationTestsModule
import com.passbolt.mobile.android.intents.ManagedAccountIntentCreator
import com.passbolt.mobile.android.matchers.hasDrawable
import com.passbolt.mobile.android.rules.IdlingResourceRule
import com.passbolt.mobile.android.rules.lazyActivitySetupScenarioRule
import org.hamcrest.Matchers.allOf
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.koin.core.component.inject
import org.koin.test.KoinTest

@RunWith(Parameterized::class)
@MediumTest
class ResourcesDetailsTest(
    private val resourceType: TestResourceType,
) : KoinTest {
    @get:Rule
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

        // Exclude description check from password types with note
        // Exclude note check from simple password
        private val EXCLUDED_ITEMS_MAP =
            mapOf(
                TestResourceType.PASSWORD_WITH_DESCRIPTION to listOf(ResourcesDetailsItemModel.COPY_METADATA_DESCRIPTION),
                TestResourceType.PASSWORD_DESCRIPTION_TOTP to listOf(ResourcesDetailsItemModel.COPY_METADATA_DESCRIPTION),
                TestResourceType.SIMPLE_PASSWORD to listOf(ResourcesDetailsItemModel.COPY_NOTE),
            )

        // List of expected clipboard values for each copy item
        val EXPECTED_CLIPBOARD_VALUES =
            mapOf(
                ResourcesDetailsItemModel.COPY_URI to "https://cloud.passbolt.com/automate",
                ResourcesDetailsItemModel.COPY_USERNAME to "BettyAutomate",
                ResourcesDetailsItemModel.COPY_PASSWORD to "BettyPassword",
                ResourcesDetailsItemModel.COPY_METADATA_DESCRIPTION to "Betty Description is unencrypted this time",
                ResourcesDetailsItemModel.COPY_NOTE to "This is a Note which is secret",
            )

        // Get excluded items for a specific resource type
        fun getExcludedItems(resourceType: TestResourceType): List<ResourcesDetailsItemModel> =
            EXCLUDED_ITEMS_MAP[resourceType] ?: emptyList()
    }

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Before
    fun setup() {
        composeTestRule.signIn(managedAccountIntentCreator.getPassphrase())
        composeTestRule.chooseFilter(filters_menu_all_items)
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
     * And     I see an arrow on the top left corner to go back to the previous page
     * And     I see a "3 dots" icon on the top right corner
     * And     I see a resource's type default icon
     * And     I see the resource name
     * And     I see the "Username" list item with title, value and a copy icon
     * And     I see the "Password" list item with title, hidden value and a show icon
     * And     I see the "Note" subsection with title, hidden value and a show icon
     * And     I see the "Metadata" subsection with title
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
        composeTestRule.pickFirstResourceWithName(resourceType.displayName)
        onView(withId(backArrow))
            .check(matches(isDisplayed()))
            .check(matches(hasDrawable(id = ic_arrow_left, tint = icon_tint)))
        onView(withId(moreIcon))
            .check(matches(isDisplayed()))
            .check(matches(hasDrawable(id = ic_more, tint = icon_tint)))
        onView(withId(icon)).check(matches(isDisplayed()))
        onView(allOf(withId(name), withText(resourceType.displayName)))
            .check(matches(isDisplayed()))
        onView(allOf(withId(passwordSectionTitle), withText(resource_details_password_header)))
            .check(matches(isDisplayed()))
        onView(allOf(withId(title), withText(resource_details_url_header)))
            .check(matches(isDisplayed()))
        onView(withId(urlItem)).check(matches(isDisplayed()))
        onView(
            allOf(
                isDescendantOfA(withId(usernameItem)),
                withId(actionIcon),
            ),
        ).check(matches(isDisplayed()))
            .check(matches(hasDrawable(id = ic_copy, tint = icon_tint)))
        onView(allOf(withId(title), withText(resource_details_username_header)))
            .check(matches(isDisplayed()))
        onView(withId(usernameItem)).check(matches(isDisplayed()))
        onView(
            allOf(
                isDescendantOfA(withId(usernameItem)),
                withId(actionIcon),
            ),
        ).check(matches(isDisplayed()))
            .check(matches(hasDrawable(id = ic_copy, tint = icon_tint)))
        onView(allOf(withId(title), withText(resource_details_password_header)))
            .check(matches(isDisplayed()))
        onView(withId(passwordItem)).check(matches(isDisplayed()))
        onView(
            allOf(
                isDescendantOfA(withId(passwordItem)),
                withId(actionIcon),
            ),
        ).check(matches(isDisplayed()))
            .check(matches(hasDrawable(id = ic_eye_visible, tint = icon_tint)))
        onView(allOf(withId(metadataSectionTitle), withText(resource_details_metadata_header)))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        onView(allOf(withId(tagsHeader), withText(resource_details_tags_header)))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        onView(allOf(withId(locationHeader), withText(location)))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        onView(allOf(withId(sharedWithLabel), withText(shared_with)))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }

    /**
     * [As a user on the resource display I can trigger the action menu](https://passbolt.testrail.io/index.php?/cases/view/16008)
     *
     * Given   I am a user on a <resource> view page
     * And     I have copy password permission
     * When	I click on the “3 dots” icon
     * Then	I see the action menu drawer
     * And	I see the name of the resource
     * And	I see a cross button to go back to the resource display
     * And	I see a “Launch website” item
     * And	I see a “Copy URI” item
     * And	I see a “Copy password” list item
     * And	I see a “Copy note” item when not on Simple Password
     * And  I see a “Copy description” list item when on Simple Password
     * And	I see a “Copy username” list item
     * And I see Add to favourite list item
     * And I see Share list item
     * And I see Edit list item
     * And I see Delete list item
     *
     *  Examples:
     *     | resource |
     *  | Simple Password |
     *     | Password with description |
     *     | Password description totp      |
     */
    @Test
    fun asAUserOnTheResourceDisplayICanTriggerTheActionMenu() {
        composeTestRule.pickFirstResourceWithName(resourceType.displayName)
        onView(withId(moreIcon))
            .perform(click())
        onView(withId(title))
            .check(matches(isDisplayed()))
        onView(withId(close))
            .check(matches(isDisplayed()))

        // Get excluded items for this resource type
        val itemsToExclude = getExcludedItems(resourceType)

        // Verify each element in the action drawer is displayed with correct icon and tint
        ResourcesDetailsItemModel.entries
            .filter { it !in itemsToExclude }
            .forEach { resourceItem ->
                onView(withId(resourceItem.resourceId))
                    .check(matches(isDisplayed()))
                    .check(matches(hasDrawable(id = resourceItem.resourceIconId, tint = resourceItem.resourceTintColorId)))
            }
    }

    /**
     * [As a user on the resource display I can trigger the action menu and copy credentials to the clipboard. 	](https://passbolt.testrail.io/index.php?/cases/view/2459)
     *
     * Given   I am a user on a <resource> view page
     * And     I have copy password permission
     * When    I click on the "3 dots" icon
     * Then    I see the action menu drawer
     * And     I can "Copy URI" item
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
        composeTestRule.pickFirstResourceWithName(resourceType.displayName)
        onView(withId(moreIcon))
            .check(matches(isDisplayed()))
            .perform(click())

        // Get excluded items for this resource type
        val itemsToExclude = getExcludedItems(resourceType)

        // Get only copy-related items from the ResourcesDetailsItemModel
        val copyItems =
            ResourcesDetailsItemModel.entries
                .filter { it !in itemsToExclude }
                .filter { it.name.startsWith("COPY_") }

        // Verify each copy element in the action drawer copies the correct string to clipboard
        copyItems.forEach { resourceItem ->
            // Clear clipboard before copying
            clipboardManager.setPrimaryClip(ClipData.newPlainText("", ""))

            // Click on the item to copy its value to clipboard
            onView(withId(resourceItem.resourceId))
                .perform(click())

            // Get clipboard content
            val clipboardText = getClipboardText(clipboardManager)

            // Verify clipboard contains the expected value
            val expectedValue = EXPECTED_CLIPBOARD_VALUES[resourceItem]
            assertEquals(
                "Clipboard should contain the expected value for ${resourceItem.name}",
                expectedValue,
                clipboardText,
            )

            // Re-open the action drawer for the next item
            onView(withId(moreIcon))
                .check(matches(isDisplayed()))
                .perform(click())
        }
    }
}
