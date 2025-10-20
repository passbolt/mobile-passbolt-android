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

package com.passbolt.mobile.android.scenarios.resource.details.sharewithsubsection

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.swipeUp
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
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.feature.authentication.AuthenticationMainActivity
import com.passbolt.mobile.android.helpers.chooseFilter
import com.passbolt.mobile.android.helpers.pickFirstResourceWithName
import com.passbolt.mobile.android.helpers.signIn
import com.passbolt.mobile.android.instrumentationTestsModule
import com.passbolt.mobile.android.intents.ManagedAccountIntentCreator
import com.passbolt.mobile.android.rules.IdlingResourceRule
import com.passbolt.mobile.android.rules.lazyActivitySetupScenarioRule
import com.passbolt.mobile.android.scenarios.resource.details.TestResourceType
import org.hamcrest.Matchers.allOf
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.koin.core.component.inject
import org.koin.test.KoinTest
import kotlin.test.BeforeTest
import com.passbolt.mobile.android.core.localization.R.string as localizationString
import com.passbolt.mobile.android.feature.permissions.R.id as permissionsId
import com.passbolt.mobile.android.feature.resources.R.id as resourcesId

@RunWith(Parameterized::class)
@MediumTest
@Ignore("Deprecated: refactor needed - entire test class disabled")
class SharedWithSubsectionTest(
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

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @BeforeTest
    fun setup() {
        //      Given   that I am logged in mobile user
        composeTestRule.signIn(managedAccountIntentCreator.getPassphrase())
    }

    /**
     * [On the resource screen I can see Shared with subsection](https://passbolt.testrail.io/index.php?/cases/view/10599)
     *
     * Given I am a user on the <resource> display screen
     * And   I have Shared with permission
     * When  I review screen content
     * Then  I see Shared with subsection with corresponding title
     * And   Shared with subsection is filled with icons of users
     * And   At least one icon is presented
     * And   Shared with subsection contains caret
     *
     * Examples:
     *     | resource |
     *     | Simple password             |
     *     | Password with description   |
     *     | Password description totp   |
     */
    @Test
    fun onTheResourceScreenICanSeeSharedWithSubsection() {
        composeTestRule.chooseFilter(filters_menu_all_items)
        composeTestRule.pickFirstResourceWithName(resourceType.displayName)
        onView(withText(localizationString.shared_with))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        onView(withId(resourcesId.root)).perform(swipeUp())
        onView(withId(resourcesId.sharedWithRecycler)).check(matches(isDisplayed()))
        onView(
            allOf(
                isDescendantOfA(withId(resourcesId.sharedWithRecycler)),
                withId(permissionsId.userItem),
            ),
        ).check(matches(isDisplayed()))
        onView(withId(resourcesId.sharedWithNavIcon)).check(matches(isDisplayed()))
    }

    private companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Resource type: {0}")
        fun resourceTypes() = TestResourceType.entries
    }
}
