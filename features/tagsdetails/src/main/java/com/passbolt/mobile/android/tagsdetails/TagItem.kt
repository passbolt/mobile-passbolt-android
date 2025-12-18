package com.passbolt.mobile.android.tagsdetails

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.core.ui.R
import com.passbolt.mobile.android.ui.TagModel

@Composable
fun TagItem(
    tag: TagModel,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter =
                painterResource(
                    if (tag.isShared) {
                        R.drawable.ic_filled_shared_tag_with_bg
                    } else {
                        R.drawable.ic_filled_tag_with_bg
                    },
                ),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
        )

        Text(
            text = tag.slug,
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier =
                Modifier
                    .padding(start = 12.dp)
                    .weight(1f),
            maxLines = 1,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TagItemPreview() {
    MaterialTheme {
        TagItem(
            tag =
                TagModel(
                    id = "1",
                    slug = "Work",
                    isShared = false,
                ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TagItemSharedPreview() {
    MaterialTheme {
        TagItem(
            tag =
                TagModel(
                    id = "2",
                    slug = "Shared Tag",
                    isShared = true,
                ),
        )
    }
}
