package com.passbolt.mobile.android.feature.main.mainscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.passbolt.mobile.android.core.compose.Inter
import com.passbolt.mobile.android.core.navigation.compose.BottomTab
import com.passbolt.mobile.android.feature.main.R
import com.passbolt.mobile.android.core.localization.R as LocalizationR

@Composable
internal fun MainBottomNavigationBar(
    selectedTab: BottomTab,
    onTabSelect: (BottomTab) -> Unit,
    isOtpTabVisible: Boolean,
) {
    val unselectedColor = MaterialTheme.colorScheme.onBackground.copy(alpha = UNSELECTED_ALPHA)
    val itemColors =
        NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = unselectedColor,
            unselectedTextColor = unselectedColor,
            indicatorColor = MaterialTheme.colorScheme.surface,
        )
    val labelStyle =
        TextStyle(
            fontFamily = Inter,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
        )

    Column(
        modifier =
            Modifier
                .background(MaterialTheme.colorScheme.surface)
                .navigationBarsPadding(),
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        NavigationBar(
            modifier =
                Modifier
                    .height(BOTTOM_NAV_HEIGHT_DP.dp)
                    .padding(top = NAV_BAR_TOP_PADDING_DP.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            windowInsets = WindowInsets(0, 0, 0, 0),
        ) {
            NavigationBarItem(
                selected = selectedTab == BottomTab.HOME,
                onClick = { onTabSelect(BottomTab.HOME) },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_home),
                        contentDescription = null,
                    )
                },
                label = {
                    Text(
                        stringResource(LocalizationR.string.main_menu_home),
                        style = labelStyle,
                        modifier = Modifier.offset(y = LABEL_OFFSET_DP.dp),
                    )
                },
                colors = itemColors,
            )
            if (isOtpTabVisible) {
                NavigationBarItem(
                    selected = selectedTab == BottomTab.OTP,
                    onClick = { onTabSelect(BottomTab.OTP) },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_otp),
                            contentDescription = null,
                        )
                    },
                    label = {
                        Text(
                            stringResource(LocalizationR.string.main_menu_otp),
                            style = labelStyle,
                            modifier = Modifier.offset(y = LABEL_OFFSET_DP.dp),
                        )
                    },
                    colors = itemColors,
                )
            }
            NavigationBarItem(
                selected = selectedTab == BottomTab.SETTINGS,
                onClick = { onTabSelect(BottomTab.SETTINGS) },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings),
                        contentDescription = null,
                    )
                },
                label = {
                    Text(
                        stringResource(LocalizationR.string.main_menu_settings),
                        style = labelStyle,
                        modifier = Modifier.offset(y = LABEL_OFFSET_DP.dp),
                    )
                },
                colors = itemColors,
            )
        }
    }
}

private const val BOTTOM_NAV_HEIGHT_DP = 64
private const val NAV_BAR_TOP_PADDING_DP = 6
private const val LABEL_OFFSET_DP = -4
private const val UNSELECTED_ALPHA = 0.8f
