package com.passbolt.mobile.android.groupdetails.groupmembers

import PassboltTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.core.ui.circularimage.CircularProfileImage
import com.passbolt.mobile.android.ui.GpgKeyModel
import com.passbolt.mobile.android.ui.UserModel
import com.passbolt.mobile.android.ui.UserProfileModel
import java.time.ZonedDateTime
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
internal fun GroupMemberItem(
    member: UserModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProfileImage(
            member.profile.avatarUrl,
            width = 40.dp,
            height = 40.dp,
        )

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 16.dp),
        ) {
            Text(
                text = "${member.profile.firstName.orEmpty()} ${member.profile.lastName.orEmpty()}".trim(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = member.userName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        Icon(
            painter = painterResource(id = CoreUiR.drawable.ic_chevron_right),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = colorResource(CoreUiR.color.icon_tint),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GroupMemberItemPreview() {
    PassboltTheme {
        GroupMemberItem(
            member =
                UserModel(
                    id = "1",
                    userName = "grace@passbolt.com",
                    disabled = false,
                    gpgKey =
                        GpgKeyModel(
                            id = "1",
                            armoredKey = "",
                            fingerprint = "93UT247Z1R1VF142",
                            bits = 2048,
                            uid = null,
                            keyId = "12345",
                            type = "RSA",
                            keyExpirationDate = null,
                            keyCreationDate = ZonedDateTime.now(),
                        ),
                    profile =
                        UserProfileModel(
                            username = "grace",
                            firstName = "Grace",
                            lastName = "Hopper",
                            avatarUrl = null,
                        ),
                ),
            onClick = {},
        )
    }
}
