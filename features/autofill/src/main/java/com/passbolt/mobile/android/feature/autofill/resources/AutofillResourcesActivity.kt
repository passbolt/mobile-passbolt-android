package com.passbolt.mobile.android.feature.autofill.resources

import android.app.Activity
import android.app.assist.AssistStructure
import android.content.Intent
import android.os.Bundle
import android.service.autofill.Dataset
import android.view.autofill.AutofillManager
import android.view.autofill.AutofillManager.EXTRA_AUTHENTICATION_RESULT
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import com.passbolt.mobile.android.common.extension.setDebouncingOnClick
import com.passbolt.mobile.android.feature.autofill.databinding.ActivityAutofillResourcesBinding
import com.passbolt.mobile.android.core.commonresource.PasswordItem
import com.passbolt.mobile.android.core.commonresource.ResourceListUiModel
import com.passbolt.mobile.android.core.mvp.authentication.BindingScopedAuthenticatedActivity
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AuthenticationType
import com.passbolt.mobile.android.feature.autofill.R
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
class AutofillResourcesActivity :
    BindingScopedAuthenticatedActivity<ActivityAutofillResourcesBinding, AutofillResourcesContract.View>(
        ActivityAutofillResourcesBinding::inflate
    ),
    AutofillResourcesContract.View {

    override val presenter: AutofillResourcesContract.Presenter by inject()
    private val modelAdapter: ModelAdapter<ResourceListUiModel, GenericItem> by inject()
    private val fastAdapter: FastAdapter<GenericItem> by inject(named<ResourceListUiModel>())
    private val resourceUiItemsMapper: ResourceUiItemsMapper by inject()

    private val initialAuthenticationResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                presenter.userAuthenticated()
            } else {
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val structure = intent.getParcelableExtra<AssistStructure>(AutofillManager.EXTRA_ASSIST_STRUCTURE)

        if (structure == null) {
            Toast.makeText(this, R.string.autofill_error, Toast.LENGTH_SHORT).show()
            navigateBack()
            return
        }
        initAdapter()
        setListeners()
        presenter.attach(this)
        presenter.argsReceived(structure)
    }

    override fun startAuthActivity() {
        initialAuthenticationResult.launch(
            ActivityIntents.authentication(
                this,
                AuthenticationType.SignInForResult
            )
        )
    }

    private fun setListeners() = with(binding) {
        swiperefresh.setOnRefreshListener {
            presenter.refreshSwipe()
        }
        searchEditText.doAfterTextChanged {
            presenter.searchTextChange(it.toString())
        }
        closeButton.setDebouncingOnClick {
            finish()
        }
    }

    private fun initAdapter() {
        binding.recyclerView.apply {
            itemAnimator = null
            layoutManager = LinearLayoutManager(this@AutofillResourcesActivity)
            adapter = fastAdapter
        }
        fastAdapter.addEventHook(PasswordItem.ItemClick {
            presenter.itemClick(it)
        })
    }

    override fun showSearchEmptyList() {
        setState(State.SEARCH_EMPTY)
    }

    private fun setState(state: State) {
        with(binding) {
            recyclerView.isVisible = state.listVisible
            searchTextInput.isEnabled = state.searchEnabled
            emptyListContainer.isVisible = state.emptyVisible
            errorContainer.isVisible = state.errorVisible
            progress.isVisible = state.progressVisible
            binding.swiperefresh.isRefreshing = state.swipeProgressVisible
        }
    }

    override fun showEmptyList() {
        setState(State.EMPTY)
    }

    override fun showResources(resources: List<ResourceListUiModel>) {
        setState(State.SUCCESS)
        val uiItems = resources.map { resourceUiItemsMapper.mapModelToItem(it) }
        FastAdapterDiffUtil.calculateDiff(modelAdapter, uiItems)
        fastAdapter.notifyAdapterDataSetChanged()
    }

    override fun navigateBack() {
        setResult(RESULT_CANCELED)
        finish()
    }

    override fun returnData(dataset: Dataset) {
        val replyIntent = Intent().apply {
            putExtra(EXTRA_AUTHENTICATION_RESULT, dataset)
        }

        setResult(Activity.RESULT_OK, replyIntent)
        finish()
    }

    override fun showGeneralError() {
        binding.swiperefresh.isRefreshing = false
        Snackbar.make(binding.root, R.string.common_failure, Snackbar.LENGTH_LONG)
            .show()
    }

    override fun showFullScreenError() {
        setState(State.ERROR)
    }

    override fun showProgress() {
        setState(State.PROGRESS)
    }

    enum class State(
        val progressVisible: Boolean,
        val errorVisible: Boolean,
        val emptyVisible: Boolean,
        val listVisible: Boolean,
        val searchEnabled: Boolean,
        val swipeProgressVisible: Boolean
    ) {
        EMPTY(false, false, true, false, false, false),
        SEARCH_EMPTY(false, false, true, false, true, false),
        ERROR(false, false, false, false, false, false),
        PROGRESS(true, false, false, false, false, false),
        SUCCESS(false, false, false, true, true, false)
    }
}
