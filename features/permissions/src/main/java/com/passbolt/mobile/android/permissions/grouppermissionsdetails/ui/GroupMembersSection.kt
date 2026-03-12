package com.passbolt.mobile.android.permissions.grouppermissionsdetails.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.core.ui.circularimage.CircularProfileImage
import com.passbolt.mobile.android.core.ui.sharedwith.xFor
import com.passbolt.mobile.android.ui.UserModel
import com.passbolt.mobile.android.core.ui.R as CoreUiR

// TODO: Identical UI offset logic to "SharedWith" section - prepare one composable that abstracts model and use in both
@Composable
fun GroupMembersSection(
    users: List<UserModel>,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 40.dp,
) {
    val avatarSizePx = with(LocalDensity.current) { avatarSize.toPx() }

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxWidth()
                .height(avatarSize),
    ) {
        val containerWidth = constraints.maxWidth
        val dataset =
            remember(users, containerWidth) {
                UsersDatasetCreator(
                    membersRecyclerWidth = containerWidth,
                    membersItemWidth = avatarSizePx,
                ).prepareDataset(users)
            }

        dataset.users.forEachIndexed { index, user ->
            CircularProfileImage(
                imageUrl = user.profile.avatarUrl,
                width = avatarSize,
                height = avatarSize,
                modifier =
                    Modifier
                        .offset { IntOffset(xFor(index, avatarSizePx, dataset.overlap), 0) },
            )
        }

        dataset.counterValue.firstOrNull()?.let { counter ->
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(avatarSize)
                        .offset {
                            IntOffset(
                                xFor(dataset.users.size, avatarSizePx, dataset.overlap),
                                0,
                            )
                        }.background(
                            color = Color.White,
                            shape = CircleShape,
                        ).border(1.dp, colorResource(CoreUiR.color.divider), CircleShape),
            ) {
                Text(
                    text = counter,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
