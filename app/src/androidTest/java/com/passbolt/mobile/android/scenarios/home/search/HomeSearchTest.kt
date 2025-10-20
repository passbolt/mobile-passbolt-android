/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021-2025 Passbolt SA
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

package com.passbolt.mobile.android.scenarios.home.search

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.passbolt.mobile.android.core.idlingresource.ResourcesFullRefreshIdlingResource
import com.passbolt.mobile.android.core.idlingresource.SignInIdlingResource
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.feature.authentication.AuthenticationMainActivity
import com.passbolt.mobile.android.helpers.signIn
import com.passbolt.mobile.android.instrumentationTestsModule
import com.passbolt.mobile.android.intents.ManagedAccountIntentCreator
import com.passbolt.mobile.android.rules.IdlingResourceRule
import com.passbolt.mobile.android.rules.lazyActivitySetupScenarioRule
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.component.inject
import org.koin.test.KoinTest
import kotlin.test.BeforeTest

@RunWith(AndroidJUnit4::class)
@MediumTest
@Ignore("Deprecated: refactor needed - entire test class disabled")
class HomeSearchTest : KoinTest {
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

    @get:Rule
    val idlingResourceRule =
        let {
            val signInIdlingResource: SignInIdlingResource by inject()
            val resourcesFullRefreshIdlingResource: ResourcesFullRefreshIdlingResource by inject()
            IdlingResourceRule(arrayOf(signInIdlingResource, resourcesFullRefreshIdlingResource))
        }

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @BeforeTest
    fun setup() {
        composeTestRule.signIn(managedAccountIntentCreator.getPassphrase())
    }

    /**
     * Verifies that the search functionality displays the correct resources based on the active filter.
     *
     * This test covers various active filters and expects **specific resources to be present**:
     * - All Items: "cakephp"
     * - Favorites: "cakephp" marked as favourite
     * - Recently Modified: "cakephp"
     * - Shared with me: "Shared resource" shared from another account
     * - Owned by me: "cakephp"
     * - Expiry: "PasswordWithExpirySet" which is expired
     * - Folders: "Automated tests folder"
     * - Tags: "cakephp"
     * - Groups: "Automation Group"
     *
     * **Prerequisites:**
     * - Ensure the listed resources exist and are accessible under the corresponding filters.
     *
     * Test Case: [TestRail](https://passbolt.testrail.io/index.php?/cases/view/2629)
     */
    @Test
    fun asALoggedInMobileUserOnTheHomepageICanSeeTheResourcesCorrespondingToMySearchQuery() {
//        //      Given       the active filter is <filter>
//        val resourceNamesByFilter =
//            mapOf(
//                ResourceFilterModel.ALL_ITEMS to "cakephp",
//                ResourceFilterModel.FAVOURITES to "cakephp",
//                ResourceFilterModel.RECENTLY_MODIFIED to "cakephp",
//                ResourceFilterModel.SHARED_WITH_ME to "Shared resource",
//                ResourceFilterModel.OWNED_BY_ME to "cakephp",
//                ResourceFilterModel.FOLDERS to "Automated tests folder",
//                ResourceFilterModel.TAGS to "cakephp",
//                ResourceFilterModel.GROUPS to "Automation Group",
//            )
//        resourceNamesByFilter.entries.forEach { (model, testedResourceName) ->
//            chooseFilter(model.filterId)
//            //      When        I type a query in the search bar
//            onView(withId(homeID.searchEditText)).perform(click(), typeText(testedResourceName))
//            //      And         the query matches at least one resource within <Active Filter>
//            //      Then        I see the list of resources matching the query
//            onView(withId(homeID.recyclerView))
//                .check(matches(atAnyPosition(hasDescendant(withText(testedResourceName)))))
//            //
//            //      Examples:
//            //         | filter              |
//            //         | “All items”         |
//            //         | “Favourites”        |
//            //         | “Recently modified” |
//            //         | “Shared with me”    |
//            //         | “Owned by me”       |
//            //         | “Expiry”            |
//            //         | “Folders”           |
//            //         | “Tags”              |
//            //         | “Groups”            |
//        }
//        chooseFilter(homeID.expiry)
//        onView(withId(homeID.searchEditText)).perform(click(), typeText("PasswordWithExpirySet"))
//        onView(withId(homeID.recyclerView))
//            .check(matches(atAnyPosition(hasDescendant(withText("PasswordWithExpirySet (expired)")))))
    }

    /**
     * Test Case: [TestRail](https://passbolt.testrail.io/index.php?/cases/view/2630)
     */
    @Test
    fun asAMobileUserICanSeeAMessageWhenTheresNoMatchToMySearchQueryOnProAndCloudInstance() {
//        //      Given       I'm on Pro or Cloud server instance
//        //      And         RBAC settings are not preventing me from seeing any filter
//        ResourceFilterModel.entries.forEach { model ->
//            //      And         the active filter is <Active Filter>
//            chooseFilter(model.filterId)
//            //      And         I'm looking for a query not matching any resources within <Active Filter>
//            //      When        I type a query in the search bar
//            onView(withId(homeID.searchEditText)).perform(click(), typeText("Does Not Exist On List"))
//            //      Then        I see a “There are no passwords” message
//            onView(withText(LocalizationR.string.no_passwords)).check(matches(isDisplayed()))
//            //      And         I see an illustration
//            onView(withId(homeID.emptyListImage)).check(matches(isDisplayed()))
//            //
//            //      | Active Filter       |
//            //         | “All items”         |
//            //         | “Favorites”         |
//            //         | “Recently modified” |
//            //         | “Shared with me”    |
//            //         | “Owned by me”       |
//            //         | “Expiry”            |
//            //         | “Folders”           |
//            //         | “Tags”              |
//            //         | “Groups”            |
//        }
    }

    // https://passbolt.testrail.io/index.php?/cases/view/2627
    @Test
    fun asALoggedInMobileUserOnTheHomepageICanSeeTheCurrentAvatarSwitchesToACloseButtonWhenIFocusTheSearchBar() {
//        //      Given   that I am a logged in mobile user
//        //      When    I focus the search bar and start typing
//        onView(withId(homeID.searchEditText)).perform(click())
//        onView(withId(homeID.searchEditText)).perform(typeText("cakephp"))
//        //      Then    I do not see the user's current avatar
//        //      And     I see a close button
//        onView(withId(MaterialR.id.text_input_end_icon)).check(matches(hasDrawable(CoreUiR.drawable.ic_close)))
    }

    // https://passbolt.testrail.io/index.php?/cases/view/2628
    @Test
    fun asALoggedInMobileUserOnTheHomepageICanCancelASearchAndGoBackToTheHomepage() {
//        //      Given   I am a logged in mobile user
//        //      And     the search bar is focused
//        onView(withId(homeID.searchEditText)).perform(click())
//        onView(withId(homeID.searchEditText)).perform(typeText("Does Not Exist On List"))
//        //      When    I click on the close button
//        onView(withId(MaterialR.id.text_input_end_icon)).perform(click())
//        //      Then    I do not see the focus status on the search bar
//        //      And     I see the homepage with the current active filter
//        onView(withId(homeID.recyclerView)).check(matches(isDisplayed()))
//        onView(withId(homeID.emptyListImage)).check(matches(not(isDisplayed())))
    }

    // https://passbolt.testrail.io/index.php?/cases/view/2464
    @Test
    fun asALoggedInMobileUserOnTheSearchFieldICanGoBackToTheHomepageWhenThereAreNoCharactersInTheSearchField() {
//        //      Given   I am on the focused search field
//        onView(withId(homeID.searchEditText)).perform(click())
//        //      When    I delete all characters in the search input
//        onView(withId(homeID.searchEditText)).perform(typeText("Does Not Exist On List"))
//        onView(withId(homeID.searchEditText)).perform(replaceText(""))
//        //      Then    I see all the resources available
//        onView(withId(homeID.recyclerView)).check(matches(isDisplayed()))
//        onView(withId(homeID.emptyListImage)).check(matches(not(isDisplayed())))
//        // Note: Material TextInputLayout keeps its end icon view (text_input_end_icon) in the hierarchy.
//        // After clearing the search field, the view typically remains visible but its drawable changes
//        // from the close icon back to avatar/another icon. Therefore, asserting doesNotExist() is incorrect
//        // and may cause flakiness. We assert that the drawable is not ic_close instead.
//        onView(withId(MaterialR.id.text_input_end_icon)).check(matches(not(hasDrawable(CoreUiR.drawable.ic_close))))
    }
}
