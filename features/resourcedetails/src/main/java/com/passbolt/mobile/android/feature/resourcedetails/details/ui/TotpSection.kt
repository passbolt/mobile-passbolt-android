package com.passbolt.mobile.android.feature.resourcedetails.details.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.passbolt.mobile.android.core.compose.Inconsolata
import com.passbolt.mobile.android.core.formatter.OtpFormatter
import com.passbolt.mobile.android.core.localization.R
import com.passbolt.mobile.android.core.ui.controller.TotpComposeController
import com.passbolt.mobile.android.core.ui.section.Section
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsIntent
import com.passbolt.mobile.android.ui.OtpItemWrapper
import org.koin.compose.koinInject

@Composable
internal fun TotpSection(
    otpModel: OtpItemWrapper?,
    onIntent: (ResourceDetailsIntent) -> Unit,
    modifier: Modifier = Modifier,
    otpFormatter: OtpFormatter = koinInject(),
    totpController: TotpComposeController = koinInject(),
) {
    Section(title = stringResource(R.string.resource_details_totp_header).uppercase(), modifier = modifier) {
        val totpColors =
            totpController.calculateTotpColors(
                remainingSeconds = otpModel?.remainingSecondsCounter,
                expirySeconds = otpModel?.otpExpirySeconds,
                isOtpVisible = otpModel?.isVisible == true,
            )
        val totpAnimations =
            totpController.calculateTotpAnimations(
                remainingSeconds = otpModel?.remainingSecondsCounter,
                expirySeconds = otpModel?.otpExpirySeconds,
            )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { onIntent(ResourceDetailsIntent.CopyTotp) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text =
                    otpFormatter.format(
                        if (otpModel?.isVisible == true) {
                            otpModel.otpValue.orEmpty()
                        } else {
                            stringResource(R.string.otp_hide_otp)
                        },
                    ),
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp),
                fontFamily = Inconsolata,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = totpColors.otpTextColor,
            )

            Spacer(modifier = Modifier.width(16.dp))

            when {
                otpModel?.isRefreshing == true -> {
                    Icon(
                        painter = painterResource(com.passbolt.mobile.android.core.ui.R.drawable.ic_refresh),
                        contentDescription = null,
                        modifier =
                            Modifier
                                .size(20.dp)
                                .rotate(totpAnimations.rotationAngle),
                    )
                }
                otpModel?.isVisible == true && otpModel.remainingSecondsCounter != null -> {
                    CircularProgressIndicator(
                        progress = { totpAnimations.animatedProgress },
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 3.dp,
                        color = totpColors.progressIndicatorColor,
                        trackColor = Color.Transparent,
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = { onIntent(ResourceDetailsIntent.ToggleTotpVisibility) },
                modifier = Modifier.size(24.dp),
            ) {
                Icon(
                    painter =
                        painterResource(
                            if (otpModel?.isVisible == true) {
                                com.passbolt.mobile.android.core.ui.R.drawable.ic_eye_invisible
                            } else {
                                com.passbolt.mobile.android.core.ui.R.drawable.ic_eye_visible
                            },
                        ),
                    contentDescription = null,
                )
            }
        }
    }
}
