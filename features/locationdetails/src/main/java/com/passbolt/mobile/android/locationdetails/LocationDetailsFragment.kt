package com.passbolt.mobile.android.locationdetails

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.expandable.getExpandableExtension
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import com.passbolt.mobile.android.core.extension.initDefaultToolbar
import com.passbolt.mobile.android.core.extension.showSnackbar
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.resources.resourceicon.ResourceIconProvider
import com.passbolt.mobile.android.feature.authentication.BindingScopedAuthenticatedFragment
import com.passbolt.mobile.android.feature.locationdetails.databinding.FragmentFolderLocationDetailsBinding
import com.passbolt.mobile.android.locationdetails.recyclerview.ExpandableFolderDatasetCreator
import com.passbolt.mobile.android.ui.FolderModel
import com.passbolt.mobile.android.ui.ResourceModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
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

class LocationDetailsFragment :
    BindingScopedAuthenticatedFragment<FragmentFolderLocationDetailsBinding, LocationDetailsContract.View>(
        FragmentFolderLocationDetailsBinding::inflate,
    ),
    LocationDetailsContract.View {
    override val presenter: LocationDetailsContract.Presenter by inject()
    private val args: LocationDetailsFragmentArgs by navArgs()
    private val fastAdapter: FastItemAdapter<GenericItem> by inject()
    private val expandableFolderDatasetCreator: ExpandableFolderDatasetCreator by inject()
    private val coroutineLaunchContext: CoroutineLaunchContext by inject()
    private val job = SupervisorJob()
    private val coroutineUiScope = CoroutineScope(job + coroutineLaunchContext.ui)
    private val resourceIconProvider: ResourceIconProvider by inject()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        requiredBinding.swipeRefresh.isEnabled = false
        initDefaultToolbar(requiredBinding.toolbar)
        initLocationDetailsRecycler(savedInstanceState)
        presenter.attach(this)
        presenter.argsRetrieved(args.locationItem, args.id)
    }

    override fun onDestroyView() {
        coroutineUiScope.coroutineContext.cancelChildren()
        presenter.detach()
        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val withSavedSelections = fastAdapter.saveInstanceState(outState)
        super.onSaveInstanceState(withSavedSelections)
    }

    override fun onResume() {
        super.onResume()
        presenter.resume(this)
    }

    override fun onPause() {
        presenter.pause()
        super.onPause()
    }

    private fun initLocationDetailsRecycler(savedInstanceState: Bundle?) {
        fastAdapter.withSavedInstanceState(savedInstanceState)
        with(requiredBinding.locationRecycler) {
            layoutManager = LinearLayoutManager(requireContext())
            itemAnimator = SlideDownAlphaAnimator()
            adapter = fastAdapter
        }
    }

    override fun showFolderName(name: String) {
        requiredBinding.name.text = name
    }

    override fun showFolderSharedIcon() {
        requiredBinding.icon.setImageResource(CoreUiR.drawable.ic_filled_shared_folder_with_bg)
    }

    override fun showFolderIcon() {
        requiredBinding.icon.setImageResource(CoreUiR.drawable.ic_filled_folder_with_bg)
    }

    override fun showFolderLocation(parentFolders: List<FolderModel>) {
        val expandableListModel = expandableFolderDatasetCreator.create(parentFolders)
        with(fastAdapter) {
            clear()
            add(expandableListModel.dataset)
            getExpandableExtension().expandAllOnPath(expandableListModel.expandToItem)
        }
    }

    override fun displayInitialsIcon(resource: ResourceModel) {
        coroutineUiScope.launch {
            requiredBinding.icon.setImageDrawable(
                resourceIconProvider.getResourceIcon(requireContext(), resource),
            )
        }
    }

    override fun hideRefreshProgress() {
        requiredBinding.swipeRefresh.isRefreshing = false
    }

    override fun showRefreshProgress() {
        requiredBinding.swipeRefresh.isRefreshing = true
    }

    override fun showDataRefreshError() {
        showSnackbar(
            LocalizationR.string.common_data_refresh_error,
            backgroundColor = CoreUiR.color.red,
        )
    }

    override fun showContentNotAvailable() {
        Toast.makeText(requireContext(), LocalizationR.string.content_not_available, Toast.LENGTH_SHORT).show()
    }

    override fun navigateToHome() {
        requireActivity().startActivity(ActivityIntents.bringHome(requireContext()))
    }
}
