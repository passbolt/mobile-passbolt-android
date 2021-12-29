package com.passbolt.mobile.android.core.commonresource.moremenu

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.passbolt.mobile.android.common.extension.setDebouncingOnClick
import com.passbolt.mobile.android.common.extension.visible
import com.passbolt.mobile.android.common.lifecycleawarelazy.lifecycleAwareLazy
import com.passbolt.mobile.android.commonresource.databinding.ViewPasswordBottomsheetBinding
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.scope.fragmentScope

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

class ResourceMoreMenuFragment : BottomSheetDialogFragment(), ResourceMoreMenuContract.View, AndroidScopeComponent {

    override val scope by fragmentScope()
    private val presenter: ResourceMoreMenuContract.Presenter by scope.inject()
    private lateinit var binding: ViewPasswordBottomsheetBinding
    private var listener: Listener? = null
    private val menuModel: ResourceMoreMenuModel by lifecycleAwareLazy {
        requireNotNull(
            requireArguments().getParcelable(EXTRA_RESOURCE_MENU_MODEL)
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ViewPasswordBottomsheetBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setListeners()
        presenter.attach(this)
        presenter.argsRetrieved(menuModel)
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
        listener = null
        super.onDetach()
    }

    private fun setListeners() {
        with(binding) {
            copyPassword.setDebouncingOnClick {
                listener?.menuCopyPasswordClick()
                dismiss()
            }
            copyDescription.setDebouncingOnClick {
                listener?.menuCopyDescriptionClick()
                dismiss()
            }
            copyUrl.setDebouncingOnClick {
                listener?.menuCopyUrlClick()
                dismiss()
            }
            copyUsername.setDebouncingOnClick {
                listener?.menuCopyUsernameClick()
                dismiss()
            }
            launchWebsite.setDebouncingOnClick {
                listener?.menuLaunchWebsiteClick()
                dismiss()
            }
            delete.setDebouncingOnClick {
                listener?.menuDeleteClick()
                dismiss()
            }
            edit.setDebouncingOnClick {
                listener?.menuEditClick()
                dismiss()
            }
            close.setDebouncingOnClick {
                dismiss()
            }
        }
    }

    override fun showTitle(title: String) {
        binding.title.text = menuModel.title
    }

    override fun showSeparator() {
        binding.separator.visible()
    }

    override fun showDeleteButton() {
        binding.delete.visible()
    }

    override fun showEditButton() {
        binding.edit.visible()
    }

    companion object {
        private const val EXTRA_RESOURCE_MENU_MODEL = "RESOURCE_MENU_MODEL"

        fun newInstance(model: ResourceMoreMenuModel) =
            ResourceMoreMenuFragment().apply {
                arguments = bundleOf(EXTRA_RESOURCE_MENU_MODEL to model)
            }
    }

    interface Listener {
        fun menuCopyPasswordClick()
        fun menuCopyDescriptionClick()
        fun menuCopyUrlClick()
        fun menuCopyUsernameClick()
        fun menuLaunchWebsiteClick()
        fun menuDeleteClick()
        fun menuEditClick()
    }
}
