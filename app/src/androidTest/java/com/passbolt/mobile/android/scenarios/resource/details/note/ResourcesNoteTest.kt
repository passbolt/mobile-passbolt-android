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

package com.passbolt.mobile.android.scenarios.resource.details.note

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
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.core.ui.R.id.actionIcon
import com.passbolt.mobile.android.core.ui.R.id.conceal
import com.passbolt.mobile.android.feature.authentication.AuthenticationMainActivity
import com.passbolt.mobile.android.feature.resources.R.id.note_item
import com.passbolt.mobile.android.helpers.pickFirstResourceWithName
import com.passbolt.mobile.android.helpers.signIn
import com.passbolt.mobile.android.instrumentationTestsModule
import com.passbolt.mobile.android.intents.ManagedAccountIntentCreator
import com.passbolt.mobile.android.rules.IdlingResourceRule
import com.passbolt.mobile.android.rules.lazyActivitySetupScenarioRule
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.koin.core.component.inject
import org.koin.test.KoinTest

@RunWith(Parameterized::class)
@MediumTest
class ResourcesNoteTest(
    private val testedResource: String,
    private val expectedNote: String,
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

    private companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Resource: {0}")
        fun testData() =
            listOf(
                arrayOf(
                    "Password with description",
                    "This is a Note which is secret",
                ),
                arrayOf(
                    "Password with description and long note",
                    // Long string literal kept as is to preserve content
                    "Free and open-source software (FOSS) is software available under a license that grants users the right to use, modify, and distribute the software – modified or not – to everyone. FOSS is an inclusive umbrella term encompassing free software and open-source software.[a][1] The rights guaranteed by FOSS originate from the \"Four Essential Freedoms\" of The Free Software Definition and the criteria of The Open Source Definition.[4][6] All FOSS can have publicly available source code, but not all source-available software is FOSS. FOSS is the opposite of proprietary software, which is licensed restrictively or has undisclosed source code.[4]\n" +
                        "\n" +
                        "The historical precursor to FOSS was the hobbyist and academic public domain software ecosystem of the 1960s to 1980s. Free and open-source operating systems such as Linux distributions and descendants of BSD are widely used, powering millions of servers, desktops, smartphones, and other devices.[9][10] Free-software licenses and open-source licenses have been adopted by many software packages. Reasons for using FOSS include decreased software costs, increased security against malware, stability, privacy, opportunities for educational usage, and giving users more control over their own hardware.\n" +
                        "\n" +
                        "The free software movement and the open-source software movement are online social movements behind widespread production, adoption and promotion of FOSS, with the former preferring to use the equivalent term free/libre and open-source software (FLOSS). FOSS is supported by a loosely associated movement of multiple organizations, foundations, communities and individuals who share basic philosophical perspectives and collaborate practically, but may diverge in detail questions.",
                ),
                arrayOf(
                    "Password, Description and TOTP",
                    "This is a Note which is secret",
                ),
                arrayOf(
                    "Password, Description and TOTP with long note",
                    // Long string literal kept as is to preserve content
                    "Free and open-source software (FOSS) is software available under a license that grants users the right to use, modify, and distribute the software – modified or not – to everyone. FOSS is an inclusive umbrella term encompassing free software and open-source software.[a][1] The rights guaranteed by FOSS originate from the \"Four Essential Freedoms\" of The Free Software Definition and the criteria of The Open Source Definition.[4][6] All FOSS can have publicly available source code, but not all source-available software is FOSS. FOSS is the opposite of proprietary software, which is licensed restrictively or has undisclosed source code.[4]\n" +
                        "\n" +
                        "The historical precursor to FOSS was the hobbyist and academic public domain software ecosystem of the 1960s to 1980s. Free and open-source operating systems such as Linux distributions and descendants of BSD are widely used, powering millions of servers, desktops, smartphones, and other devices.[9][10] Free-software licenses and open-source licenses have been adopted by many software packages. Reasons for using FOSS include decreased software costs, increased security against malware, stability, privacy, opportunities for educational usage, and giving users more control over their own hardware.\n" +
                        "\n" +
                        "The free software movement and the open-source software movement are online social movements behind widespread production, adoption and promotion of FOSS, with the former preferring to use the equivalent term free/libre and open-source software (FLOSS). FOSS is supported by a loosely associated movement of multiple organizations, foundations, communities and individuals who share basic philosophical perspectives and collaborate practically, but may diverge in detail questions.",
                ),
                // TODO - This need to be enabled after enabling V5 on cloud's `Betty` user")
//            "Default resource type",
//            "Default resource type with TOTP"
            )
    }

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Before
    fun setup() {
        composeTestRule.signIn(managedAccountIntentCreator.getPassphrase())
    }

    /**
     * [As a logged in mobile user on the resource display I can show or hide the resource note ](https://passbolt.testrail.io/index.php?/cases/view/2447)
     * Given I am a mobile user on the <resource> display screen
     * When  I click on the show icon in the "Note" item list
     * And   I successfully authenticate (if needed)
     * Then  I should see a spinner in place of the eye icon
     * And   I should see the note body
     * And   I should see a hide icon
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
        composeTestRule.pickFirstResourceWithName(testedResource)
        onView(
            allOf(
                isDescendantOfA(withId(note_item)),
                withId(actionIcon),
            ),
        ).perform(click())
        onView(withText(expectedNote)).check(matches(isDisplayed()))
        onView(
            allOf(
                isDescendantOfA(withId(note_item)),
                withId(actionIcon),
            ),
        ).perform(scrollTo())
            .perform(click())
        onView(
            allOf(
                isDescendantOfA(withId(note_item)),
                withId(conceal),
            ),
        ).check(matches(isDisplayed()))
    }
}
