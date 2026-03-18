package com.passbolt.mobile.android.feature.resourcedetails.details.ui.metadata

import PassboltTheme
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.passbolt.mobile.android.core.ui.span.RoundedBackgroundSpan
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
internal fun TagsItem(
    tags: List<String>,
    modifier: Modifier = Modifier,
) {
    // TODO no TextOverflow.Middle in compose for now - use existing solution based on View
    AndroidView(
        factory = { context ->
            TextView(context).apply {
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.MIDDLE
            }
        },
        update = { textView ->
            val builder = SpannableStringBuilder()
            tags.forEach { tag ->
                builder.append(tag)
                builder.setSpan(
                    RoundedBackgroundSpan(
                        ContextCompat.getColor(textView.context, CoreUiR.color.divider),
                        ContextCompat.getColor(textView.context, CoreUiR.color.text_primary),
                    ),
                    builder.length - tag.length,
                    builder.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
            textView.text = builder
        },
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun TagsItemPreview() {
    PassboltTheme {
        TagsItem(
            tags = listOf("work", "important"),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TagsItemDarkPreview() {
    PassboltTheme(darkTheme = true) {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            TagsItem(
                tags = listOf("work", "important"),
            )
        }
    }
}
