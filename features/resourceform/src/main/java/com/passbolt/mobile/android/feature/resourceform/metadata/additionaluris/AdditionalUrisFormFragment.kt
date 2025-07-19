package com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import com.passbolt.mobile.android.core.extension.initDefaultToolbar
import com.passbolt.mobile.android.core.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.extension.showSnackbar
import com.passbolt.mobile.android.core.mvp.scoped.BindingScopedFragment
import com.passbolt.mobile.android.feature.resourceform.databinding.FragmentAdditionalUrisFormBinding
import com.passbolt.mobile.android.ui.AdditionalUrisUiModel
import org.koin.android.ext.android.inject
import java.util.UUID
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
class AdditionalUrisFormFragment :
    BindingScopedFragment<FragmentAdditionalUrisFormBinding>(
        FragmentAdditionalUrisFormBinding::inflate,
    ),
    AdditionalUrisFormContract.View {
    private val presenter: AdditionalUrisFormContract.Presenter by inject()
    private val navArgs: AdditionalUrisFormFragmentArgs by navArgs()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initDefaultToolbar(requiredBinding.toolbar)
        setListeners()
        presenter.attach(this)
        presenter.argsRetrieved(navArgs.mode, navArgs.additionalUris)
    }

    private fun setListeners() {
        with(requiredBinding) {
            additionalUrisSubformView.mainUriInput.setTextChangeListener {
                presenter.mainUriChanged(it)
            }
            additionalUrisSubformView.additionalUriChanged = { tag, uri ->
                presenter.additionalUriChanged(tag, uri)
            }
            additionalUrisSubformView.additionalUriRemoved = { tag ->
                presenter.additionalUriRemoved(tag)
            }
            additionalUrisSubformView.addAdditionalUriClick = {
                presenter.addAdditionalUriClick()
            }
            apply.setDebouncingOnClick {
                presenter.applyClick()
            }
        }
    }

    override fun showCreateTitle() {
        requiredBinding.toolbar.toolbarTitle = getString(LocalizationR.string.resource_form_create_additional_uris)
    }

    override fun showEditTitle(resourceName: String) {
        requiredBinding.toolbar.toolbarTitle = getString(LocalizationR.string.resource_form_edit_resource, resourceName)
    }

    override fun goBackWithResult(additionalUris: AdditionalUrisUiModel) {
        setFragmentResult(
            REQUEST_ADDITIONAL_URIS,
            bundleOf(EXTRA_ADDITIONAL_URIS to additionalUris),
        )
        findNavController().popBackStack()
    }

    override fun showMainUri(mainUri: String) {
        requiredBinding.additionalUrisSubformView.mainUriInput.text = mainUri
    }

    override fun showAdditionalUris(uiTagToModel: LinkedHashMap<UUID, String>) {
        requiredBinding.additionalUrisSubformView.showAdditionalUris(uiTagToModel)
    }

    override fun showMaxUriLimitExceeded(maxUriLimit: Int) {
        showSnackbar(
            getString(LocalizationR.string.resource_form_max_additional_uris_limit, maxUriLimit),
            length = Snackbar.LENGTH_SHORT,
            backgroundColor = CoreUiR.color.red,
        )
    }

    companion object {
        const val REQUEST_ADDITIONAL_URIS = "ADDITIONAL_URIS"

        const val EXTRA_ADDITIONAL_URIS = "additional_uris"
    }
}
