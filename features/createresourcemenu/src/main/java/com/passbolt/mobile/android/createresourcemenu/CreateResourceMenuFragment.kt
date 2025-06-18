package com.passbolt.mobile.android.createresourcemenu

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import com.passbolt.mobile.android.common.lifecycleawarelazy.lifecycleAwareLazy
import com.passbolt.mobile.android.core.extension.setDebouncingOnClickAndDismiss
import com.passbolt.mobile.android.core.extension.visible
import com.passbolt.mobile.android.core.mvp.scoped.BindingScopedBottomSheetFragment
import com.passbolt.mobile.android.feature.createresourcemenu.databinding.BottomsheetCreateResourceMenuBinding
import com.passbolt.mobile.android.ui.HomeDisplayViewModel
import org.koin.android.ext.android.inject

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

class CreateResourceMenuFragment :
    BindingScopedBottomSheetFragment<BottomsheetCreateResourceMenuBinding>(
        BottomsheetCreateResourceMenuBinding::inflate
    ),
    CreateResourceMenuContract.View {

    private val presenter: CreateResourceMenuContract.Presenter by inject()
    private var listener: Listener? = null
    private val homeDisplayViewModel: HomeDisplayViewModel? by lifecycleAwareLazy {
        BundleCompat.getParcelable(
            requireArguments(),
            EXTRA_HOME_DISPLAY_VIEW_MODEL,
            HomeDisplayViewModel::class.java
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setListeners()
        presenter.attach(this)
        presenter.argsRetrieved(homeDisplayViewModel)
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
        presenter.detach()
        listener?.resourceMoreMenuDismissed()
        listener = null
        super.onDetach()
    }

    private fun setListeners() {
        with(binding) {
            setDebouncingOnClickAndDismiss(createPassword) { listener?.createPasswordClick() }
            setDebouncingOnClickAndDismiss(createTotp) { listener?.createTotpClick() }
            setDebouncingOnClickAndDismiss(createFolder) { listener?.createFolderClick() }
            setDebouncingOnClickAndDismiss(close)
        }
    }

    override fun showPasswordButton() {
        binding.createPassword.visible()
    }

    override fun showTotpButton() {
        binding.createTotp.visible()
    }

    override fun showFoldersButton() {
        binding.createFolder.visible()
    }

    override fun hideMenu() {
        dismiss()
    }

    companion object {
        private const val EXTRA_HOME_DISPLAY_VIEW_MODEL = "HOME_DISPLAY_VIEW_MODEL"

        fun newInstance(homeDisplayViewModel: HomeDisplayViewModel?) =
            CreateResourceMenuFragment().apply {
                arguments = bundleOf(
                    EXTRA_HOME_DISPLAY_VIEW_MODEL to homeDisplayViewModel
                )
            }
    }

    interface Listener {
        fun createTotpClick() {}
        fun createPasswordClick() {}
        fun createFolderClick() {}
        fun resourceMoreMenuDismissed() {}
    }
}
