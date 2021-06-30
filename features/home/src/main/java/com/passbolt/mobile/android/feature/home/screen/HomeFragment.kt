package com.passbolt.mobile.android.feature.home.screen

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.passbolt.mobile.android.core.mvp.scoped.BindingScopedFragment
import com.passbolt.mobile.android.feature.home.databinding.FragmentHomeBinding
import com.passbolt.mobile.android.feature.home.screen.adapter.PasswordItem
import com.passbolt.mobile.android.feature.home.screen.adapter.PasswordModel
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
class HomeFragment : BindingScopedFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate), HomeContract.View {

    private val itemAdapter: ItemAdapter<PasswordItem> by inject()
    private val fastAdapter: FastAdapter<PasswordItem> by inject()
    private val presenter: HomeContract.Presenter by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initAdapter()
        presenter.attach(this)
    }

    private fun initAdapter() {
        binding.recyclerView.apply {
            itemAnimator = null
            layoutManager = LinearLayoutManager(requireContext())
            adapter = fastAdapter
        }
        fastAdapter.addEventHooks(listOf(
            PasswordItem.ItemClick {
                presenter.itemClick()
            },
            PasswordItem.MoreClick {
                presenter.moreClick()
            }
        ))
    }

    override fun showPasswords(list: List<PasswordModel>) {
        itemAdapter.add(list.map { PasswordItem(it) })
    }

    override fun navigateToMore() {
        Toast.makeText(requireContext(), "More clicked!", Toast.LENGTH_SHORT).show()
    }

    override fun navigateToDetails() {
        Toast.makeText(requireContext(), "Details clicked!", Toast.LENGTH_SHORT).show()
    }
}
