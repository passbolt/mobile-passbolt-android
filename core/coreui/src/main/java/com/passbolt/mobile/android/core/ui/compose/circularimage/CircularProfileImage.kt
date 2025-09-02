package com.passbolt.mobile.android.core.ui.compose.circularimage

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import com.passbolt.mobile.android.core.ui.R

@Composable
fun CircularProfileImage(
    imageUrl: String?,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    @DrawableRes placeholderRes: Int = R.drawable.ic_user_avatar,
) {
    val painter =
        rememberAsyncImagePainter(
            model =
                ImageRequest
                    .Builder(LocalContext.current)
                    .data(imageUrl)
                    .placeholder(placeholderRes)
                    .error(placeholderRes)
                    .crossfade(true)
                    .build(),
        )

    Image(
        painter = painter,
        contentDescription = null,
        modifier =
            modifier
                .size(width, height)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
        contentScale = ContentScale.Crop,
    )
}
