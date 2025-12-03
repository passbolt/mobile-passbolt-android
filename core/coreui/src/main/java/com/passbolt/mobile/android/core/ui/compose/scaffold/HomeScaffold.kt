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
package com.passbolt.mobile.android.core.ui.compose.scaffold

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.core.ui.compose.fab.AddFloatingActionButton
import com.passbolt.mobile.android.core.ui.compose.snackbar.ColoredSnackbarVisuals
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScaffold(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    appBarTitle: String? = null,
    @DrawableRes appBarIconRes: Int? = null,
    appBarSearchInput: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    onBackClick: () -> Unit = {},
    shouldShowBackIcon: Boolean = false,
    onCloseClick: () -> Unit = {},
    shouldShowCloseIcon: Boolean = false,
    onMoreClick: () -> Unit = {},
    shouldShowMoreIcon: Boolean = false,
    content: @Composable (PaddingValues) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Surface(shadowElevation = 4.dp) {
                TopAppBar(
                    colors =
                        TopAppBarColors(
                            containerColor = colorResource(CoreUiR.color.background),
                            scrolledContainerColor = colorResource(CoreUiR.color.background),
                            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    windowInsets = WindowInsets(0.dp),
                    scrollBehavior = scrollBehavior,
                    expandedHeight = 124.dp,
                    title = {
                        Column {
                            Spacer(modifier = Modifier.width(16.dp))
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (shouldShowBackIcon) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null,
                                        tint = colorResource(CoreUiR.color.icon_tint),
                                        modifier = Modifier.clickable { onBackClick() },
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    appBarIconRes?.let {
                                        Image(
                                            painter = painterResource(it),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    appBarTitle?.let {
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground,
                                        )
                                    }
                                }
                                if (shouldShowMoreIcon) {
                                    Icon(
                                        Icons.Default.MoreVert,
                                        contentDescription = null,
                                        tint = colorResource(CoreUiR.color.icon_tint),
                                        modifier = Modifier.clickable { onMoreClick() },
                                    )
                                }
                                if (shouldShowCloseIcon) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = null,
                                        tint = colorResource(CoreUiR.color.icon_tint),
                                        modifier = Modifier.clickable { onCloseClick() },
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            appBarSearchInput()
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    },
                )
            }
        },
        floatingActionButton = { floatingActionButton() },
        floatingActionButtonPosition = FabPosition.End,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { data ->
                    val customVisuals = data.visuals as? ColoredSnackbarVisuals
                    if (customVisuals != null) {
                        Snackbar(
                            snackbarData = data,
                            containerColor = customVisuals.backgroundColor,
                            contentColor = customVisuals.contentColor,
                        )
                    } else {
                        Snackbar(
                            snackbarData = data,
                        )
                    }
                },
            )
        },
        content = content,
    )
}

@Preview(showBackground = true)
@Composable
private fun HomeScaffoldPreview() {
    HomeScaffold(
        snackbarHostState = SnackbarHostState(),
        appBarTitle = "TOTP",
        appBarIconRes = CoreUiR.drawable.ic_time_lock,
        appBarSearchInput = {
            Text("Search input placeholder")
        },
        floatingActionButton = {
            AddFloatingActionButton(
                onClick = { },
            )
        },
        shouldShowBackIcon = true,
        shouldShowCloseIcon = true,
        shouldShowMoreIcon = true,
        content = { padding ->
            Box(
                modifier =
                    Modifier
                        .fillMaxSize(),
            ) {
                Text("Sample content")
            }
        },
    )
}
