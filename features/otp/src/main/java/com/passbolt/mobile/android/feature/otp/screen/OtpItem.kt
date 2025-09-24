package com.passbolt.mobile.android.feature.otp.screen

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.passbolt.mobile.android.core.compose.Inconsolata
import com.passbolt.mobile.android.core.resources.resourceicon.ResourceIconProvider
import com.passbolt.mobile.android.core.ui.R
import com.passbolt.mobile.android.core.ui.controller.TotpComposeController
import com.passbolt.mobile.android.core.ui.formatter.OtpFormatter
import com.passbolt.mobile.android.ui.OtpItemWrapper
import com.passbolt.mobile.android.ui.isExpired
import org.koin.compose.koinInject

@Composable
internal fun OtpItem(
    otpItem: OtpItemWrapper,
    resourceIconProvider: ResourceIconProvider,
    onItemClick: (OtpItemWrapper) -> Unit,
    onMoreClick: (OtpItemWrapper) -> Unit,
    otpFormatter: OtpFormatter = koinInject(),
    totpController: TotpComposeController = koinInject(),
) {
    val context = LocalContext.current
    val totpColors =
        totpController.calculateTotpColors(
            remainingSeconds = otpItem.remainingSecondsCounter,
            expirySeconds = otpItem.otpExpirySeconds,
            isOtpVisible = otpItem.isVisible,
        )
    val totpAnimations =
        totpController.calculateTotpAnimations(
            remainingSeconds = otpItem.remainingSecondsCounter,
            expirySeconds = otpItem.otpExpirySeconds,
        )

    var resourceIcon by remember { mutableStateOf<Drawable?>(null) }
    LaunchedEffect(otpItem) {
        resourceIcon = resourceIconProvider.getResourceIcon(context, otpItem.resource)
    }

    Row(
        modifier =
            Modifier.Companion
                .fillMaxWidth()
                .height(64.dp)
                .clickable { onItemClick(otpItem) }
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.Companion.CenterVertically,
    ) {
        Box(modifier = Modifier.Companion.size(46.dp, 52.dp)) {
            if (resourceIcon != null) {
                Image(
                    painter = rememberDrawablePainter(resourceIcon),
                    contentDescription = null,
                    modifier =
                        Modifier.Companion
                            .size(40.dp)
                            .align(Alignment.Companion.CenterStart),
                )
            }

            if (otpItem.resource.isExpired()) {
                Image(
                    painter = painterResource(R.drawable.ic_excl_indicator),
                    contentDescription = null,
                    modifier =
                        Modifier.Companion
                            .size(12.dp)
                            .align(Alignment.Companion.BottomEnd),
                )
            }
        }

        Spacer(modifier = Modifier.Companion.width(12.dp))

        Column(modifier = Modifier.Companion.weight(1f)) {
            Text(
                text = otpItem.resource.metadataJsonModel.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Companion.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Row(
                verticalAlignment = Alignment.Companion.CenterVertically,
                modifier = Modifier.Companion.padding(top = 4.dp),
            ) {
                Text(
                    text =
                        otpFormatter.format(
                            if (otpItem.isVisible) {
                                otpItem.otpValue.orEmpty()
                            } else {
                                stringResource(
                                    com.passbolt.mobile.android.core.localization.R.string.otp_hide_otp,
                                )
                            },
                        ),
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp),
                    fontFamily = Inconsolata,
                    maxLines = 1,
                    overflow = TextOverflow.Companion.Ellipsis,
                    color = totpColors.otpTextColor,
                )

                Spacer(modifier = Modifier.Companion.width(16.dp))

                when {
                    otpItem.isRefreshing -> {
                        Icon(
                            painter = painterResource(R.drawable.ic_refresh),
                            contentDescription = null,
                            modifier =
                                Modifier.Companion
                                    .size(20.dp)
                                    .rotate(totpAnimations.rotationAngle),
                        )
                    }
                    otpItem.isVisible && otpItem.remainingSecondsCounter != null -> {
                        CircularProgressIndicator(
                            progress = { totpAnimations.animatedProgress },
                            modifier = Modifier.Companion.size(20.dp),
                            strokeWidth = 3.dp,
                            color = totpColors.progressIndicatorColor,
                            trackColor = Color.Companion.Transparent,
                        )
                    }
                    else -> {
                        Icon(
                            painter = painterResource(R.drawable.ic_eye_visible),
                            contentDescription = null,
                            modifier = Modifier.Companion.size(24.dp, 20.dp),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.Companion.width(8.dp))

        IconButton(
            onClick = { onMoreClick(otpItem) },
            modifier = Modifier.Companion.size(40.dp),
        ) {
            Icon(Icons.Default.MoreVert, contentDescription = null)
        }
    }
}
