package com.passbolt.mobile.android.feature.autofill.accessibility

import android.content.Context
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Alignment.Companion.Center
import androidx.glance.layout.Alignment.Companion.CenterEnd
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.passbolt.mobile.android.core.compose.PassboltGlanceColorScheme
import com.passbolt.mobile.android.core.extension.setDebouncingOnClick
import com.passbolt.mobile.android.feature.autofill.R
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
internal fun AutofillLabelContent() {
    val context = LocalContext.current
    GlanceTheme(colors = PassboltGlanceColorScheme) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
                GlanceModifier
                    .width(250.dp)
                    .padding(16.dp)
                    .background(ImageProvider(R.drawable.bg_autofill_overlay)),
        ) {
            Box(
                modifier = GlanceModifier.fillMaxWidth(),
            ) {
                Box(
                    contentAlignment = Center,
                    modifier = GlanceModifier.fillMaxWidth(),
                ) {
                    Image(
                        provider = ImageProvider(CoreUiR.drawable.logo_text_icon),
                        contentDescription = null,
                        modifier = GlanceModifier.width(120.dp),
                    )
                }
                Box(
                    contentAlignment = CenterEnd,
                    modifier = GlanceModifier.fillMaxWidth(),
                ) {
                    Image(
                        provider = ImageProvider(CoreUiR.drawable.ic_close),
                        contentDescription = context.getString(LocalizationR.string.autofill_overlay_close),
                        modifier = GlanceModifier.padding(8.dp),
                    )
                }
            }
            Text(
                text = context.getString(LocalizationR.string.autofill_select_password),
                style =
                    TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = GlanceTheme.colors.onBackground,
                    ),
                modifier = GlanceModifier.padding(top = 16.dp),
            )
        }
    }
}

@OptIn(ExperimentalGlanceRemoteViewsApi::class)
internal suspend fun composeAutofillLabelView(
    context: Context,
    onClick: () -> Unit,
    onClose: () -> Unit,
): View {
    val remoteViews =
        GlanceRemoteViews()
            .compose(
                context = context,
                size = DpSize(250.dp, 150.dp),
            ) {
                AutofillLabelContent()
            }.remoteViews

    val overlayView = remoteViews.apply(context, null)
    overlayView.setDebouncingOnClick { onClick() }

    val closeView = overlayView.findViewByContentDescription(context, LocalizationR.string.autofill_overlay_close)
    closeView?.apply { setDebouncingOnClick { onClose() } }

    return overlayView
}
