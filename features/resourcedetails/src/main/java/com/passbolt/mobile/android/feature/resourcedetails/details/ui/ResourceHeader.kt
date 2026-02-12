package com.passbolt.mobile.android.feature.resourcedetails.details.ui

import PassboltTheme
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.passbolt.mobile.android.core.ui.R

@Composable
internal fun ResourceHeader(
    title: String,
    isExpired: Boolean,
    isFavourite: Boolean,
    resourceIcon: Drawable?,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(top = 5.dp),
    ) {
        Box {
            if (resourceIcon != null) {
                Image(
                    painter = rememberDrawablePainter(resourceIcon),
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                )
            }
            if (isExpired) {
                Image(
                    painter = painterResource(R.drawable.ic_excl_indicator),
                    contentDescription = null,
                    modifier =
                        Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 5.dp, y = 5.dp)
                            .size(20.dp),
                )
            }
            if (isFavourite) {
                Image(
                    painter = painterResource(R.drawable.ic_favourite_star_filled),
                    contentDescription = null,
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-6).dp)
                            .size(24.dp),
                )
            }
        }

        Text(
            text =
                if (isExpired) {
                    stringResource(com.passbolt.mobile.android.core.localization.R.string.name_expired, title)
                } else {
                    title
                },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Suppress("MagicNumber") // preview only
private fun createCircleDrawable() =
    GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(Color.GRAY)
        setSize(60, 60)
    }

@Preview(showBackground = true)
@Composable
private fun ResourceHeaderPreview() {
    PassboltTheme {
        ResourceHeader(
            title = "Test Resource",
            isExpired = false,
            isFavourite = false,
            resourceIcon = createCircleDrawable(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ResourceHeaderFavouriteExpiredPreview() {
    PassboltTheme {
        ResourceHeader(
            title = "Test Resource",
            isExpired = true,
            isFavourite = true,
            resourceIcon = createCircleDrawable(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ResourceHeaderDarkPreview() {
    PassboltTheme(darkTheme = true) {
        ResourceHeader(
            title = "Test Resource",
            isExpired = false,
            isFavourite = true,
            resourceIcon = createCircleDrawable(),
        )
    }
}
