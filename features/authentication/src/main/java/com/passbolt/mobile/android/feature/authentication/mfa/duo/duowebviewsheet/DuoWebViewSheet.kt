package com.passbolt.mobile.android.feature.authentication.mfa.duo.duowebviewsheet

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

private const val DEFAULT_SHEET_HEIGHT_FRACTION = 0.9f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuoWebViewSheet(
    duoPromptUrl: String,
    onDuoAuthFinish: (DuoState) -> Unit,
    onDismiss: () -> Unit,
    sheetHeightFraction: Float = DEFAULT_SHEET_HEIGHT_FRACTION,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isLoading by remember { mutableStateOf(true) }

    val duoWebViewClient =
        remember {
            DuoWebViewClient().apply {
                onPageCommitVisible = {
                    isLoading = false
                }
                onDuoAuthFinishedIntercepted = { state ->
                    onDuoAuthFinish(state)
                }
            }
        }

    DisposableEffect(Unit) {
        onDispose {
            duoWebViewClient.release()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(sheetHeightFraction),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp),
        ) {
            DuoWebView(
                url = duoPromptUrl,
                webViewClient = duoWebViewClient,
                modifier = Modifier.fillMaxWidth(),
            )

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun DuoWebView(
    url: String,
    webViewClient: DuoWebViewClient,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                this.webViewClient = webViewClient
                settings.apply {
                    javaScriptEnabled = true
                    allowFileAccess = false
                    allowContentAccess = false
                }
                loadUrl(url)
            }
        },
        onRelease = { webView ->
            webView.destroy()
        },
        modifier = modifier,
    )
}
