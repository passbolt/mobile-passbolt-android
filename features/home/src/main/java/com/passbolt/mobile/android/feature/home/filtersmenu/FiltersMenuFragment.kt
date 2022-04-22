package com.passbolt.mobile.android.feature.home.filtersmenu

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
import com.passbolt.mobile.android.core.extension.selectableBackgroundResourceId
import com.passbolt.mobile.android.feature.home.R
import com.passbolt.mobile.android.feature.home.databinding.FiletrsBottomsheetBinding
import com.passbolt.mobile.android.ui.FiltersMenuModel
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

class FiltersMenuFragment : BottomSheetDialogFragment(), FiltersMenuContract.View, AndroidScopeComponent {

    override val scope by fragmentScope()
    private val presenter: FiltersMenuContract.Presenter by scope.inject()
    private lateinit var binding: FiletrsBottomsheetBinding
    private var listener: Listener? = null
    private val menuModel: FiltersMenuModel by lifecycleAwareLazy {
        requireNotNull(
            requireArguments().getParcelable(EXTRA_FILTERS_MENU_MODEL)
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FiletrsBottomsheetBinding.inflate(inflater)
        presenter.creatingView()
        presenter.attach(this)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setListeners()
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

    override fun onDestroyView() {
        presenter.detach()
        super.onDestroyView()
    }

    private fun setListeners() {
        with(binding) {
            allItems.setListenerWithDismiss { listener?.menuAllItemsClick() }
            favourites.setListenerWithDismiss { listener?.menuFavouritesClick() }
            recentlyModified.setListenerWithDismiss { listener?.menuRecentlyModifiedClick() }
            sharedWithMe.setListenerWithDismiss { listener?.menuSharedWithMeClick() }
            ownedByMe.setListenerWithDismiss { listener?.menuOwnedByMeClick() }
            folders.setListenerWithDismiss { listener?.menuFoldersClick() }
            tags.setListenerWithDismiss { listener?.menuTagsClick() }
            groups.setListenerWithDismiss { listener?.menuGroupsClick() }
            close.setListenerWithDismiss { }
        }
    }

    private fun View.setListenerWithDismiss(action: () -> Unit) {
        setDebouncingOnClick {
            action()
            dismiss()
        }
    }

    override fun unselectAll() {
        listOf(
            binding.allItems,
            binding.favourites,
            binding.recentlyModified,
            binding.sharedWithMe,
            binding.ownedByMe
        ).forEach {
            it.setBackgroundResource(requireContext().selectableBackgroundResourceId())
        }
    }

    override fun selectAllItemsItem() {
        setBackgroundPrimaryColor(binding.allItems)
    }

    override fun selectFavouritesItem() {
        setBackgroundPrimaryColor(binding.favourites)
    }

    override fun selectRecentlyModifiedItem() {
        setBackgroundPrimaryColor(binding.recentlyModified)
    }

    override fun selectGroupsMenuItem() {
        setBackgroundPrimaryColor(binding.groups)
    }

    override fun selectSharedWithMeItem() {
        setBackgroundPrimaryColor(binding.sharedWithMe)
    }

    override fun selectOwnedByMeItem() {
        setBackgroundPrimaryColor(binding.ownedByMe)
    }

    override fun selectTagsMenuItem() {
        setBackgroundPrimaryColor(binding.tags)
    }

    override fun selectFoldersMenuItem() {
        setBackgroundPrimaryColor(binding.folders)
    }

    private fun setBackgroundPrimaryColor(view: View) {
        view.setBackgroundColor(requireContext().getColor(R.color.primary))
    }

    override fun showFoldersMenuItem() {
        binding.folders.visible()
    }

    override fun showTagsMenuItem() {
        binding.tags.visible()
    }

    interface Listener {
        fun menuAllItemsClick()
        fun menuFavouritesClick()
        fun menuRecentlyModifiedClick()
        fun menuSharedWithMeClick()
        fun menuOwnedByMeClick()
        fun menuFoldersClick()
        fun menuTagsClick()
        fun menuGroupsClick()
    }

    companion object {
        private const val EXTRA_FILTERS_MENU_MODEL = "FILTERS_MENU_MODEL"

        fun newInstance(model: FiltersMenuModel) =
            FiltersMenuFragment().apply {
                arguments = bundleOf(EXTRA_FILTERS_MENU_MODEL to model)
            }
    }
}
