/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021 Passbolt SA
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * The name "Passbolt" is a registered trademark of Passbolt SA, and Passbolt SA hereby declines to grant a trademark
 * license to "Passbolt" pursuant to the GNU Affero General Public License version 3 Section 7(e), without a separate
 * agreement with Passbolt SA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Passbolt SA (https://www.passbolt.com)
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://www.passbolt.com Passbolt (tm)
 * @since v1.0
 */

package com.passbolt.mobile.android.feature.authentication.mfa.duo.duowebviewsheet

import android.content.Context
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import androidx.core.os.bundleOf
import com.passbolt.mobile.android.common.lifecycleawarelazy.lifecycleAwareLazy
import com.passbolt.mobile.android.core.extension.gone
import com.passbolt.mobile.android.core.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.extension.visible
import com.passbolt.mobile.android.core.mvp.scoped.BindingScopedBottomSheetFragment
import com.passbolt.mobile.android.feature.authentication.databinding.BottomSheetDuoWebViewBinding
import org.koin.android.ext.android.inject

class DuoWebViewBottomSheetFragment :
    BindingScopedBottomSheetFragment<BottomSheetDuoWebViewBinding>(BottomSheetDuoWebViewBinding::inflate),
    DuoWebViewContract.View {

    private val presenter: DuoWebViewContract.Presenter by inject()
    private val duoWebViewClient: DuoWebViewClient by inject()
    private val bundledDuoPromptUrl by lifecycleAwareLazy {
        requireNotNull(requireArguments().getString(EXTRA_DUO_PROMPT_URL))
    }
    private var listener: Listener? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setListeners()
        setupWebView()
        presenter.attach(this)
        presenter.argsRetrieved(bundledDuoPromptUrl)
    }

    private fun setListeners() {
        binding.closeButton.setDebouncingOnClick {
            dismiss()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = when {
            activity is Listener -> activity as Listener
            parentFragment is Listener -> parentFragment as Listener
            else -> error("Parent must implement ${Listener::class.java.name}")
        }
    }

    override fun onDetach() {
        duoWebViewClient.release()
        presenter.detach()
        super.onDetach()
    }

    private fun setupWebView() {
        binding.webView.webViewClient = duoWebViewClient.apply {
            onPageCommitVisible = {
                binding.apply {
                    configureDuoPageViaJs(webView)
                    progressBar.gone()
                    webView.visible()
                }
            }
            onDuoAuthFinishedIntercepted = {
                listener?.duoAuthFinished(it)
                dismiss()
            }
        }
        binding.webView.settings.javaScriptEnabled = true
    }

    // duo page has gray background and mobile-unfriendly padding configured - remove it
    private fun configureDuoPageViaJs(webView: WebView) {
        webView.evaluateJavascript(
            "document.body.children[0].style.margin='0 auto 0 auto';"
        ) {}
        webView.evaluateJavascript(
            "document.body.style.background='#ffffff';"
        ) {}
    }

    override fun loadUrl(duoPromptUrl: String) {
        binding.webView.loadUrl(duoPromptUrl)
    }

    companion object {
        private const val EXTRA_DUO_PROMPT_URL = "DUO_PROMPT_URL"

        fun newInstance(duoPromptUrl: String) = DuoWebViewBottomSheetFragment()
            .apply {
                arguments = bundleOf(
                    EXTRA_DUO_PROMPT_URL to duoPromptUrl
                )
            }
    }

    interface Listener {
        fun duoAuthFinished(state: DuoState)
    }
}
