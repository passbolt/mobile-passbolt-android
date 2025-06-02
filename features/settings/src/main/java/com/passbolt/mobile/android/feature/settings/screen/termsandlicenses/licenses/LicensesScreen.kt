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

package com.passbolt.mobile.android.feature.settings.screen.termsandlicenses.licenses

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.core.compose.AppTypography
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.ui.compose.topbar.TitleAppBar
import com.passbolt.mobile.android.feature.settings.screen.termsandlicenses.licenses.LicensesIntent.GoBack
import com.passbolt.mobile.android.feature.settings.screen.termsandlicenses.licenses.LicensesIntent.GoToLicenseUrl
import com.passbolt.mobile.android.feature.settings.screen.termsandlicenses.licenses.LicensesSideEffect.NavigateToLicenseUrl
import com.passbolt.mobile.android.ui.LicenseModelItem
import com.passbolt.mobile.android.ui.OpenSourceLicensesModel
import com.passbolt.mobile.android.ui.Scm
import com.passbolt.mobile.android.ui.SpdxLicense
import org.koin.androidx.compose.koinViewModel
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
internal fun LicensesScreen(
    navigation: LicensesNavigation,
    modifier: Modifier = Modifier,
    viewModel: LicensesViewModel = koinViewModel(),
) {
    val state = viewModel.viewState.collectAsStateWithLifecycle()

    LicensesScreen(
        modifier = modifier,
        state = state.value,
        onIntent = viewModel::onIntent,
    )

    SideEffectDispatcher(viewModel.sideEffect) {
        when (it) {
            LicensesSideEffect.NavigateUp -> navigation.navigateUp()
            is NavigateToLicenseUrl -> navigation.navigateToLicenseUrl(it.licenseUrl)
        }
    }
}

@Composable
private fun LicensesScreen(
    state: LicensesState,
    onIntent: (LicensesIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
    ) {
        TitleAppBar(
            title = stringResource(LocalizationR.string.settings_licenses_title),
            onBackClick = { onIntent(GoBack) },
        )
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            itemsIndexed(state.openSourceLicensesModel) { index, license ->
                LicenseItem(license = license, onIntent = onIntent)
                if (index < state.openSourceLicensesModel.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun LicenseItem(
    license: LicenseModelItem,
    onIntent: (LicensesIntent) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        LicenseArtifactSection(license)
        Spacer(modifier = Modifier.height(16.dp))
        LicenseNameSection(license)
        Spacer(modifier = Modifier.height(16.dp))
        LicenseLicensesSection(license)
        Spacer(modifier = Modifier.height(16.dp))
        LicenseUrlsSection(license, onIntent)
    }
}

@Composable
private fun LicenseUrlsSection(
    license: LicenseModelItem,
    onIntent: (LicensesIntent) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(style = AppTypography.titleMedium, text = stringResource(LocalizationR.string.licenses_urls))
        license.spdxLicenses
            ?.mapNotNull { it.url }
            ?.forEach { url ->
                Text(
                    text =
                        buildAnnotatedString {
                            append(stringResource(LocalizationR.string.common_bullet))
                            append(" ")
                            withLink(
                                link =
                                    LinkAnnotation.Url(
                                        url = url,
                                        linkInteractionListener =
                                            LinkInteractionListener {
                                                onIntent(GoToLicenseUrl(url))
                                            },
                                    ),
                            ) {
                                withStyle(
                                    style =
                                        SpanStyle(
                                            color = colorResource(CoreUiR.color.primary),
                                            textDecoration = TextDecoration.Underline,
                                        ),
                                ) {
                                    append(url)
                                }
                            }
                        },
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
    }
}

@Composable
private fun LicenseLicensesSection(license: LicenseModelItem) {
    Column(Modifier.fillMaxWidth()) {
        Text(style = AppTypography.titleMedium, text = stringResource(LocalizationR.string.licenses_licensees))
        license.spdxLicenses
            ?.mapNotNull { it.name }
            ?.forEach {
                Text(
                    text = stringResource(LocalizationR.string.common_bullet_format, it),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
    }
}

@Composable
private fun LicenseNameSection(license: LicenseModelItem) {
    Column(Modifier.fillMaxWidth()) {
        Text(style = AppTypography.titleMedium, text = stringResource(LocalizationR.string.licenses_name))
        Text(text = license.name.orEmpty())
    }
}

@Composable
private fun LicenseArtifactSection(license: LicenseModelItem) {
    Column(Modifier.fillMaxWidth()) {
        Text(style = AppTypography.titleMedium, text = stringResource(LocalizationR.string.licenses_artifact))
        Text(
            text =
                "%s:%s:%s".format(
                    license.groupId.orEmpty(),
                    license.artifactId.orEmpty(),
                    license.version.orEmpty(),
                ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LicensesPreview() {
    LicensesScreen(
        state =
            LicensesState(
                OpenSourceLicensesModel().apply {
                    add(
                        LicenseModelItem(
                            groupId = "com.passbolt",
                            artifactId = "passbolt-android",
                            version = "1.0.0",
                            name = "Passbolt Android",
                            spdxLicenses =
                                listOf(
                                    SpdxLicense(
                                        name = "GNU Affero General Public License v3",
                                        url = "https://www.gnu.org/licenses/agpl-3.0.html",
                                        identifier = "AGPL-3.0",
                                    ),
                                ),
                            scm = Scm(url = "https://www.gnu.org/licenses/agpl-3.0.html"),
                        ),
                    )
                },
            ),
        onIntent = {},
        modifier = Modifier,
    )
}
