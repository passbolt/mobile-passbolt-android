package com.passbolt.mobile.android.feature.authentication.mfa.duo.duowebviewsheet

import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

class DuoWebViewClient : WebViewClient() {

    var onPageCommitVisible: (() -> Unit)? = null
    var onDuoAuthFinishedIntercepted: ((DuoState) -> Unit)? = null

    override fun onPageCommitVisible(view: WebView?, url: String?) {
        super.onPageCommitVisible(view, url)
        onPageCommitVisible?.invoke()
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        request?.let {
            return if (it.url.toString().contains(DUO_CALLBACK_REDIRECTION)) {
                onDuoAuthFinishedIntercepted?.invoke(
                    DuoState(
                        state = it.url.getQueryParameter(QUERY_DUO_STATE),
                        duoCode = it.url.getQueryParameter(QUERY_DUO_CODE)
                    )
                )
                true
            } else {
                super.shouldOverrideUrlLoading(view, request)
            }
        }
        return super.shouldOverrideUrlLoading(view, request)
    }

    fun release() {
        onPageCommitVisible = null
        onDuoAuthFinishedIntercepted = null
    }

    private companion object {
        private const val DUO_CALLBACK_REDIRECTION = "/mfa/verify/duo/callback"
        private const val QUERY_DUO_STATE = "state"
        private const val QUERY_DUO_CODE = "duo_code"
    }
}
