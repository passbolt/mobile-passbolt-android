package com.passbolt.mobile.android.core.compose

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.passbolt.mobile.android.core.ui.R

val InterMedium =
    FontFamily(
        Font(R.font.inter_medium, FontWeight.Medium),
        Font(R.font.inter_semi_bold, FontWeight.SemiBold),
    )

val AppTypography =
    Typography(
        titleMedium =
            TextStyle(
                fontFamily = InterMedium,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                letterSpacing = 0.sp,
            ),
        displayMedium =
            TextStyle(
                fontFamily = InterMedium,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                letterSpacing = 0.sp,
            ),
    )
