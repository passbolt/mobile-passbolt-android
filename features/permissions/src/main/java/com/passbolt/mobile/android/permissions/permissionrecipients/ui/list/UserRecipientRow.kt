package com.passbolt.mobile.android.permissions.permissionrecipients.ui.list

import PassboltTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.core.localization.R
import com.passbolt.mobile.android.core.ui.compose.circularimage.CircularProfileImage
import com.passbolt.mobile.android.ui.GpgKeyModel
import com.passbolt.mobile.android.ui.UserModel
import com.passbolt.mobile.android.ui.UserProfileModel

@Composable
internal fun UserRecipientRow(
    user: UserModel,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    disabledUsedAlpha: Float = 0.5f,
) {
    val isDisabled = user.disabled
    val alpha = if (isDisabled) disabledUsedAlpha else 1f
    val userProfileImageSize = 40.dp

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp),
    ) {
        Spacer(modifier = Modifier.width(16.dp))
        CircularProfileImage(
            imageUrl = user.profile.avatarUrl,
            width = userProfileImageSize,
            height = userProfileImageSize,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text =
                    if (isDisabled) {
                        stringResource(R.string.name_suspended, user.fullName)
                    } else {
                        user.fullName
                    },
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.alpha(alpha),
            )
            Text(
                text = user.userName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.alpha(alpha),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        RadioButton(
            selected = isSelected,
            onClick = null,
        )
        Spacer(modifier = Modifier.width(16.dp))
    }
}

private val previewUser =
    UserModel(
        id = "1",
        userName = "john@passbolt.com",
        disabled = false,
        gpgKey =
            GpgKeyModel(
                id = "1",
                armoredKey = "",
                fingerprint = "",
                bits = 4096,
                uid = null,
                keyId = "",
                type = null,
                keyExpirationDate = null,
                keyCreationDate = null,
            ),
        profile =
            UserProfileModel(
                username = "john@passbolt.com",
                firstName = "John",
                lastName = "Doe",
                avatarUrl = null,
            ),
    )

@Preview(showBackground = true)
@Composable
private fun UserRecipientRowSelectedPreview() {
    PassboltTheme {
        UserRecipientRow(
            user = previewUser,
            isSelected = true,
            onClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun UserRecipientRowUnselectedPreview() {
    PassboltTheme {
        UserRecipientRow(
            user = previewUser,
            isSelected = false,
            onClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun UserRecipientRowDisabledPreview() {
    PassboltTheme {
        UserRecipientRow(
            user = previewUser.copy(disabled = true),
            isSelected = false,
            onClick = {},
        )
    }
}
