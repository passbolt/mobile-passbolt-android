package com.passbolt.mobile.android.locationdetails

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.expandable.getExpandableExtension
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import com.passbolt.mobile.android.core.extension.initDefaultToolbar
import com.passbolt.mobile.android.core.ui.initialsicon.InitialsIconGenerator
import com.passbolt.mobile.android.feature.authentication.BindingScopedAuthenticatedFragment
import com.passbolt.mobile.android.locationdetails.databinding.FragmentFolderLocationDetailsBinding
import com.passbolt.mobile.android.locationdetails.recyclerview.ExpandableFolderDatasetCreator
import com.passbolt.mobile.android.ui.FolderModel
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

class LocationDetailsFragment :
    BindingScopedAuthenticatedFragment<FragmentFolderLocationDetailsBinding, LocationDetailsContract.View>(
        FragmentFolderLocationDetailsBinding::inflate
    ), LocationDetailsContract.View {

    override val presenter: LocationDetailsContract.Presenter by inject()
    private val args: LocationDetailsFragmentArgs by navArgs()
    private val fastAdapter: FastItemAdapter<GenericItem> by inject()
    private val expandableFolderDatasetCreator: ExpandableFolderDatasetCreator by inject()
    private val initialsIconGenerator: InitialsIconGenerator by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDefaultToolbar(binding.toolbar)
        initLocationDetailsRecycler(savedInstanceState)
        presenter.attach(this)
        presenter.argsRetrieved(args.locationItem, args.id)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val withSavedSelections = fastAdapter.saveInstanceState(outState)
        super.onSaveInstanceState(withSavedSelections)
    }

    private fun initLocationDetailsRecycler(savedInstanceState: Bundle?) {
        fastAdapter.withSavedInstanceState(savedInstanceState)
        with(binding.locationRecycler) {
            layoutManager = LinearLayoutManager(requireContext())
            itemAnimator = SlideDownAlphaAnimator()
            adapter = fastAdapter
        }
    }

    override fun showFolderName(name: String) {
        binding.name.text = name
    }

    override fun showFolderSharedIcon() {
        binding.icon.setImageResource(R.drawable.ic_filled_shared_folder_with_bg)
    }

    override fun showFolderIcon() {
        binding.icon.setImageResource(R.drawable.ic_filled_folder_with_bg)
    }

    override fun showFolderLocation(parentFolders: List<FolderModel>) {
        val expandableListModel = expandableFolderDatasetCreator.create(parentFolders)
        with(fastAdapter) {
            add(expandableListModel.dataset)
            getExpandableExtension().expandAllOnPath(expandableListModel.expandToItem)
        }
    }

    override fun displayInitialsIcon(name: String, initials: String) {
        binding.icon.setImageDrawable(
            initialsIconGenerator.generate(name, initials)
        )
    }
}
