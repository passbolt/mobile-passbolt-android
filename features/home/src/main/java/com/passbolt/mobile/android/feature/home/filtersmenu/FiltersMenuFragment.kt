package com.passbolt.mobile.android.feature.home.filtersmenu

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.updatePadding
import androidx.core.widget.TextViewCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.passbolt.mobile.android.common.extension.setDebouncingOnClick
import com.passbolt.mobile.android.common.lifecycleawarelazy.lifecycleAwareLazy
import com.passbolt.mobile.android.core.extension.selectableBackgroundResourceId
import com.passbolt.mobile.android.feature.home.R
import com.passbolt.mobile.android.feature.home.databinding.FiletrsBottomsheetBinding
import com.passbolt.mobile.android.ui.FiltersMenuModel
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.scope.fragmentScope
import kotlin.math.ceil

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
            allItems.setDebouncingOnClick {
                listener?.menuAllItemsClick()
                dismiss()
            }
            favourites.setDebouncingOnClick {
                listener?.menuFavouritesClick()
                dismiss()
            }
            recentlyModified.setDebouncingOnClick {
                listener?.menuRecentlyModifiedClick()
                dismiss()
            }
            sharedWithMe.setDebouncingOnClick {
                listener?.menuSharedWithMeClick()
                dismiss()
            }
            ownedByMe.setDebouncingOnClick {
                listener?.menuOwnedByMeClick()
                dismiss()
            }
            close.setDebouncingOnClick {
                dismiss()
            }
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

    override fun selectSharedWithMeItem() {
        setBackgroundPrimaryColor(binding.sharedWithMe)
    }

    override fun selectOwnedByMeItem() {
        setBackgroundPrimaryColor(binding.ownedByMe)
    }

    override fun selectTagsMenuItem() {
        setBackgroundPrimaryColor(binding.root.findViewWithTag(TAG_TAGS))
    }

    override fun selectFoldersMenuItem() {
        setBackgroundPrimaryColor(binding.root.findViewWithTag(TAG_FOLDERS))
    }

    private fun setBackgroundPrimaryColor(view: View) {
        view.setBackgroundColor(requireContext().getColor(R.color.primary))
    }

    override fun addBottomSeparator() {
        val dp16 = resources.getDimension(R.dimen.dp_16).toInt()
        val separator = View(requireContext()).apply {
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                resources.getDimension(R.dimen.dp_1).toInt()
            ).apply {
                setMargins(dp16, 0, dp16, 0)
            }
            id = View.generateViewId()
            setBackgroundColor(requireContext().getColor(R.color.divider))
            tag = TAG_SEPARATOR
        }
        binding.root.addView(separator)
        constrainTopToBottomInRoot(separator.id, R.id.ownedByMe)
    }

    override fun addFoldersMenuItem() {
        val foldersMenuItem = createMenuItem(
            getString(R.string.filters_menu_folders),
            R.drawable.ic_folder,
            TAG_FOLDERS
        ) { listener?.menuFoldersClick() }
        binding.root.addView(foldersMenuItem)
        constrainTopToBottomInRoot(foldersMenuItem.id, binding.root.findViewWithTag<View>(TAG_SEPARATOR).id)
    }

    override fun addTagsMenuItem() {
        val tagsMenuItem = createMenuItem(
            getString(R.string.filters_menu_tags),
            R.drawable.ic_tag,
            TAG_TAGS
        ) { listener?.menuTagsClick() }
        binding.root.addView(tagsMenuItem)
        constrainTopToBottomInRoot(tagsMenuItem.id, binding.root.findViewWithTag<View>(TAG_FOLDERS).id)
    }

    private fun createMenuItem(
        label: String,
        @DrawableRes startDrawableResId: Int,
        viewTag: String,
        onClick: () -> Unit
    ): TextView {
        val dp16 = resources.getDimension(R.dimen.dp_16)
        return TextView(requireContext(), null, 0, R.style.PasswordMenuItem).apply {
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                resources.getDimension(R.dimen.dp_48).toInt()
            )
            id = View.generateViewId()
            tag = viewTag
            text = label
            TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(
                this,
                ContextCompat.getDrawable(requireContext(), startDrawableResId),
                null,
                null,
                null
            )
            TextViewCompat.setCompoundDrawableTintList(
                this,
                ColorStateList.valueOf(requireContext().getColor(R.color.icon_tint))
            )
            compoundDrawablePadding = dp16.toInt()
            updatePadding(left = ceil(dp16).toInt())
            setOnClickListener {
                onClick()
                dismiss()
            }
        }
    }

    private fun constrainTopToBottomInRoot(@IdRes what: Int, @IdRes toWhat: Int) {
        ConstraintSet().apply {
            clone(binding.root)
            connect(
                what,
                ConstraintSet.TOP,
                toWhat,
                ConstraintSet.BOTTOM,
                resources.getDimension(R.dimen.dp_8).toInt()
            )
            applyTo(binding.root)
        }
    }

    interface Listener {
        fun menuAllItemsClick()
        fun menuFavouritesClick()
        fun menuRecentlyModifiedClick()
        fun menuSharedWithMeClick()
        fun menuOwnedByMeClick()
        fun menuFoldersClick()
        fun menuTagsClick()
    }

    companion object {
        private const val EXTRA_FILTERS_MENU_MODEL = "FILTERS_MENU_MODEL"
        private const val TAG_SEPARATOR = "viewSeparator"
        private const val TAG_FOLDERS = "viewFolders"
        private const val TAG_TAGS = "viewTags"

        fun newInstance(model: FiltersMenuModel) =
            FiltersMenuFragment().apply {
                arguments = bundleOf(EXTRA_FILTERS_MENU_MODEL to model)
            }
    }
}
