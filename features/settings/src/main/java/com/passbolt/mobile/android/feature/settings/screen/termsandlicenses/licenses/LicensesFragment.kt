package com.passbolt.mobile.android.feature.settings.screen.termsandlicenses.licenses

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.passbolt.mobile.android.core.extension.initDefaultToolbar
import com.passbolt.mobile.android.core.mvp.scoped.BindingScopedFragment
import com.passbolt.mobile.android.feature.settings.databinding.FragmentLicensesBinding
import com.passbolt.mobile.android.feature.settings.screen.termsandlicenses.licenses.recycler.LicenseItem
import com.passbolt.mobile.android.ui.OpenSourceLicensesModel
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named

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
class LicensesFragment :
    BindingScopedFragment<FragmentLicensesBinding>(FragmentLicensesBinding::inflate),
    LicensesContract.View {
    private val presenter: LicensesContract.Presenter by inject()
    private val modelAdapter: ItemAdapter<LicenseItem> by inject()
    private val fastAdapter: FastAdapter<GenericItem> by inject(named<LicenseItem>())

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        setListeners()
        presenter.attach(this)
        requireContext()
            .assets
            .open(LICENSES_ASSET)
            .bufferedReader()
            .readText()
            .let {
                presenter.argsRetrieved(it)
            }
    }

    private fun setListeners() {
        with(requiredBinding) {
            initDefaultToolbar((toolbar))
            licensesRecycler.apply {
                itemAnimator = null
                layoutManager = LinearLayoutManager(requireContext())
                adapter = fastAdapter
                addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
            }
        }
    }

    override fun showLicenses(licensesData: OpenSourceLicensesModel) {
        modelAdapter.set(licensesData.map { LicenseItem(it) })
    }

    private companion object {
        private const val LICENSES_ASSET = "licenses.json"
    }
}
