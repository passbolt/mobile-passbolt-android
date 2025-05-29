package com.passbolt.mobile.android.resourcemoremenu

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import com.passbolt.mobile.android.common.lifecycleawarelazy.lifecycleAwareLazy
import com.passbolt.mobile.android.core.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.extension.setDebouncingOnClickAndDismiss
import com.passbolt.mobile.android.core.extension.showSnackbar
import com.passbolt.mobile.android.core.extension.visible
import com.passbolt.mobile.android.feature.authentication.BindingScopedAuthenticatedBottomSheetFragment
import com.passbolt.mobile.android.feature.resourcemoremenu.databinding.ViewPasswordBottomsheetBinding
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel
import org.koin.android.ext.android.inject
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

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

class ResourceMoreMenuFragment :
    BindingScopedAuthenticatedBottomSheetFragment<ViewPasswordBottomsheetBinding, ResourceMoreMenuContract.View>(
        ViewPasswordBottomsheetBinding::inflate,
    ),
    ResourceMoreMenuContract.View {
    override val presenter: ResourceMoreMenuContract.Presenter by inject()
    private var listener: Listener? = null
    private val resourceId: String by lifecycleAwareLazy {
        requireNotNull(requireArguments().getString(EXTRA_RESOURCE_ID))
    }
    private val initialResourceName: String by lifecycleAwareLazy {
        requireNotNull(requireArguments().getString(EXTRA_RESOURCE_NAME))
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        showTitle(initialResourceName)
        setListeners()
        presenter.attach(this)
        presenter.argsRetrieved(resourceId)
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

    override fun onResume() {
        super.onResume()
        presenter.resume(this)
    }

    override fun onPause() {
        presenter.pause()
        super.onPause()
    }

    override fun onDetach() {
        presenter.detach()
        listener?.resourceMoreMenuDismissed()
        listener = null
        super.onDetach()
    }

    private fun setListeners() {
        with(requiredBinding) {
            setDebouncingOnClickAndDismiss(copyPassword) { listener?.menuCopyPasswordClick() }
            setDebouncingOnClickAndDismiss(copyMetadataDescription) { listener?.menuCopyMetadataDescriptionClick() }
            setDebouncingOnClickAndDismiss(copyNote) { listener?.menuCopyNoteClick() }
            setDebouncingOnClickAndDismiss(copyUrl) { listener?.menuCopyUrlClick() }
            setDebouncingOnClickAndDismiss(copyUsername) { listener?.menuCopyUsernameClick() }
            setDebouncingOnClickAndDismiss(launchWebsite) { listener?.menuLaunchWebsiteClick() }
            setDebouncingOnClickAndDismiss(share) { listener?.menuShareClick() }
            setDebouncingOnClickAndDismiss(delete) { listener?.menuDeleteClick() }
            setDebouncingOnClickAndDismiss(edit) { listener?.menuEditClick() }
            setDebouncingOnClickAndDismiss(close)
            favourite.setDebouncingOnClick { presenter.menuFavouriteClick() }
        }
    }

    override fun notifyFavouriteClick(favouriteOption: ResourceMoreMenuModel.FavouriteOption) {
        listener?.menuFavouriteClick(favouriteOption)
        dismiss()
    }

    override fun showAddToFavouritesButton() {
        with(requiredBinding.favourite) {
            visible()
            text = getString(LocalizationR.string.more_add_to_favourite)
            setCompoundDrawablesRelativeWithIntrinsicBounds(
                ContextCompat.getDrawable(requireContext(), CoreUiR.drawable.ic_add_to_favourite),
                null,
                null,
                null,
            )
        }
    }

    override fun showRemoveFromFavouritesButton() {
        with(requiredBinding.favourite) {
            visible()
            text = getString(LocalizationR.string.more_remove_from_favourite)
            setCompoundDrawablesRelativeWithIntrinsicBounds(
                ContextCompat.getDrawable(requireContext(), CoreUiR.drawable.ic_remove_favourite),
                null,
                null,
                null,
            )
        }
    }

    override fun showTitle(title: String) {
        requiredBinding.title.text = title
    }

    override fun showSeparator() {
        requiredBinding.separator.visible()
    }

    override fun showDeleteButton() {
        requiredBinding.delete.visible()
    }

    override fun showEditButton() {
        requiredBinding.edit.visible()
    }

    override fun showShareButton() {
        requiredBinding.share.visible()
    }

    override fun showCopyButton() {
        requiredBinding.copyPassword.visible()
    }

    override fun showCopyNoteButton() {
        requiredBinding.copyNote.visible()
    }

    override fun showCopyMetadataDescriptionButton() {
        requiredBinding.copyMetadataDescription.visible()
    }

    override fun hideMenu() {
        dismiss()
    }

    override fun hideRefreshProgress() {
        // ignored - progress indicator should not be shown on the menu fragment
    }

    override fun showRefreshProgress() {
        // ignored - progress indicator should not be shown on the menu fragment
    }

    override fun showRefreshFailure() {
        showSnackbar(
            messageResId = LocalizationR.string.common_data_refresh_error,
            backgroundColor = CoreUiR.color.red,
        )
    }

    companion object {
        private const val EXTRA_RESOURCE_ID = "RESOURCE_ID"
        private const val EXTRA_RESOURCE_NAME = "RESOURCE_NAME"

        fun newInstance(
            resourceId: String,
            resourceName: String,
        ) = ResourceMoreMenuFragment().apply {
            arguments =
                bundleOf(
                    EXTRA_RESOURCE_ID to resourceId,
                    EXTRA_RESOURCE_NAME to resourceName,
                )
        }
    }

    interface Listener {
        fun menuCopyPasswordClick()

        fun menuCopyMetadataDescriptionClick()

        fun menuCopyNoteClick()

        fun menuCopyUrlClick()

        fun menuCopyUsernameClick()

        fun menuLaunchWebsiteClick()

        fun menuDeleteClick()

        fun menuEditClick()

        fun menuShareClick()

        fun menuFavouriteClick(option: ResourceMoreMenuModel.FavouriteOption)

        fun resourceMoreMenuDismissed()
    }
}
