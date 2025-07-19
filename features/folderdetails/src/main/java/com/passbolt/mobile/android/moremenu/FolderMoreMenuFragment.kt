package com.passbolt.mobile.android.moremenu

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.passbolt.mobile.android.common.lifecycleawarelazy.lifecycleAwareLazy
import com.passbolt.mobile.android.core.extension.setDebouncingOnClickAndDismiss
import com.passbolt.mobile.android.feature.folderdetails.databinding.ViewFolderMoreMenuBottomsheetBinding
import com.passbolt.mobile.android.ui.FolderMoreMenuModel
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.scope.fragmentScope
import com.passbolt.mobile.android.core.localization.R as LocalizationR

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

class FolderMoreMenuFragment :
    BottomSheetDialogFragment(),
    FolderMoreMenuContract.View,
    AndroidScopeComponent {
    override val scope by fragmentScope(useParentActivityScope = false)
    private val presenter: FolderMoreMenuContract.Presenter by scope.inject()
    private lateinit var binding: ViewFolderMoreMenuBottomsheetBinding
    private var listener: Listener? = null
    private val menuModel by lifecycleAwareLazy {
        requireNotNull(
            BundleCompat.getParcelable(requireArguments(), EXTRA_FOLDER_MENU_MODEL, FolderMoreMenuModel::class.java),
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = ViewFolderMoreMenuBottomsheetBinding.inflate(inflater)
        presenter.attach(this)
        presenter.argsRetrieved(menuModel)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        setListeners()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener =
            when {
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
            setDebouncingOnClickAndDismiss(seeDetails) { listener?.menuSeeFolderDetailsClick() }
            setDebouncingOnClickAndDismiss(close)
        }
    }

    override fun showTitle(title: String?) {
        binding.title.text = title ?: getString(LocalizationR.string.folder_root)
    }

    companion object {
        private const val EXTRA_FOLDER_MENU_MODEL = "FOLDER_MENU_MODEL"

        fun newInstance(model: FolderMoreMenuModel) =
            FolderMoreMenuFragment().apply {
                arguments = bundleOf(EXTRA_FOLDER_MENU_MODEL to model)
            }
    }

    interface Listener {
        fun menuSeeFolderDetailsClick()
    }
}
