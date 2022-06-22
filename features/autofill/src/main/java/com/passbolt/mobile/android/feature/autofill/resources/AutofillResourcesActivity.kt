package com.passbolt.mobile.android.feature.autofill.resources

import android.app.Activity
import android.app.assist.AssistStructure
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.autofill.AutofillManager.EXTRA_ASSIST_STRUCTURE
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import coil.ImageLoader
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import com.passbolt.mobile.android.common.extension.setDebouncingOnClick
import com.passbolt.mobile.android.common.lifecycleawarelazy.lifecycleAwareLazy
import com.passbolt.mobile.android.common.px
import com.passbolt.mobile.android.core.commonresource.PasswordItem
import com.passbolt.mobile.android.core.commonresource.ResourceListUiModel
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.core.navigation.AutofillMode
import com.passbolt.mobile.android.feature.authentication.BindingScopedAuthenticatedActivity
import com.passbolt.mobile.android.feature.autofill.R
import com.passbolt.mobile.android.feature.autofill.databinding.ActivityAutofillResourcesBinding
import com.passbolt.mobile.android.feature.autofill.resources.datasetstrategy.ReturnAutofillDatasetStrategy
import com.passbolt.mobile.android.feature.resources.ResourceActivity
import com.passbolt.mobile.android.feature.resources.ResourceMode
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf
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

// TODO Refactor to use home fragment + strategy pattern - PAS-390
class AutofillResourcesActivity :
    BindingScopedAuthenticatedActivity<ActivityAutofillResourcesBinding, AutofillResourcesContract.View>(
        ActivityAutofillResourcesBinding::inflate
    ), AutofillResourcesContract.View {

    override val presenter: AutofillResourcesContract.Presenter by inject()
    private val modelAdapter: ModelAdapter<ResourceListUiModel, GenericItem> by inject()
    private val fastAdapter: FastAdapter<GenericItem> by inject(named<ResourceListUiModel>())
    private val resourceUiItemsMapper: ResourceUiItemsMapper by inject()
    private val imageLoader: ImageLoader by inject()
    private val bundledAutofillUri by lifecycleAwareLazy {
        intent.getStringExtra(ActivityIntents.EXTRA_AUTOFILL_URI)
    }
    private val bundledAutofillMode by lifecycleAwareLazy {
        intent.getStringExtra(ActivityIntents.EXTRA_AUTOFILL_MODE_NAME).let {
            AutofillMode.valueOf(requireNotNull(it))
        }
    }
    private lateinit var returnAutofillDatasetStrategy: ReturnAutofillDatasetStrategy

    private val initialAuthenticationResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                presenter.userAuthenticated()
            } else {
                finish()
            }
        }

    private val resourceDetailsResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == ResourceActivity.RESULT_RESOURCE_CREATED) {
                val newResourceId = it.data?.getStringExtra(ResourceActivity.EXTRA_RESOURCE_ID)
                presenter.newResourceCreated(newResourceId)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        returnAutofillDatasetStrategy = scope.get(named(bundledAutofillMode)) { parametersOf(this) }
        initAdapter()
        setListeners()
        presenter.attach(this)
        presenter.argsReceived(bundledAutofillUri)
    }

    override fun onDestroy() {
        returnAutofillDatasetStrategy.detach()
        super.onDestroy()
    }

    override fun displaySearchAvatar(url: String?) {
        val request = ImageRequest.Builder(this)
            .data(url)
            .transformations(CircleCropTransformation())
            .size(30.px, 30.px)
            .placeholder(R.drawable.ic_avatar_placeholder)
            .target(
                onError = {
                    setSearchEndIconWithListener(
                        ContextCompat.getDrawable(this, R.drawable.ic_avatar_placeholder)!!,
                        presenter::searchAvatarClick
                    )
                },
                onSuccess = {
                    setSearchEndIconWithListener(it, presenter::searchAvatarClick)
                }
            )
            .build()
        imageLoader.enqueue(request)
    }

    private fun setSearchEndIconWithListener(icon: Drawable, listener: () -> Unit) {
        with(binding.searchTextInput) {
            endIconMode = TextInputLayout.END_ICON_CUSTOM
            endIconDrawable = icon
            setEndIconOnClickListener {
                listener.invoke()
            }
        }
    }

    override fun displaySearchClearIcon() {
        setSearchEndIconWithListener(
            ContextCompat.getDrawable(this, R.drawable.ic_close)!!,
            presenter::searchClearClick
        )
    }

    override fun clearSearchInput() {
        binding.searchEditText.setText("")
    }

    override fun navigateToAuth() {
        initialAuthenticationResult.launch(
            ActivityIntents.authentication(
                this,
                ActivityIntents.AuthConfig.SignIn,
                appContext = AppContext.AUTOFILL
            )
        )
    }

    private fun setListeners() = with(binding) {
        swipeRefresh.setOnRefreshListener {
            presenter.refreshSwipe()
        }
        searchEditText.doAfterTextChanged {
            presenter.searchTextChange(it.toString())
        }
        closeButton.setDebouncingOnClick {
            presenter.closeClick()
        }
        createButton.setDebouncingOnClick {
            resourceDetailsResult.launch(
                ResourceActivity.newInstance(
                    this@AutofillResourcesActivity,
                    ResourceMode.NEW
                )
            )
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
        binding.appBar.setExpanded(true)
    }

    private fun setState(state: State) {
        with(binding) {
            recyclerView.isVisible = state.listVisible
            emptyListContainer.isVisible = state.emptyVisible
            errorContainer.isVisible = state.errorVisible
            progress.isVisible = state.progressVisible
            binding.swipeRefresh.isRefreshing = state.swipeProgressVisible
        }
    }

    override fun showEmptyList() {
        setState(State.EMPTY)
        binding.appBar.setExpanded(true)
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

    override fun finishAutofill() {
        finishAffinity()
    }

    override fun showGeneralError() {
        binding.swipeRefresh.isRefreshing = false
        Snackbar.make(binding.root, R.string.common_failure, Snackbar.LENGTH_LONG)
            .setAnchorView(binding.createButton)
            .show()
    }

    override fun showResourceAddedSnackbar() {
        Snackbar.make(
            binding.root,
            R.string.resource_update_create_success,
            Snackbar.LENGTH_SHORT
        )
            .setAnchorView(binding.createButton)
            .show()
    }

    override fun showFullScreenError() {
        setState(State.ERROR)
        binding.appBar.setExpanded(true)
    }

    override fun showProgress() {
        setState(State.PROGRESS)
    }

    override fun navigateToManageAccount() {
        initialAuthenticationResult.launch(
            ActivityIntents.authentication(
                this,
                ActivityIntents.AuthConfig.ManageAccount,
                appContext = AppContext.AUTOFILL
            )
        )
    }

    override fun navigateToSetup() {
        startActivity(ActivityIntents.start(this))
        finish()
    }

    override fun navigateToHome() {
        startActivity(ActivityIntents.home(this))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    override fun getAutofillStructure() =
        intent!!.getParcelableExtra<AssistStructure>(EXTRA_ASSIST_STRUCTURE)!!

    override fun autofillReturn(username: String, password: String, uri: String?) {
        returnAutofillDatasetStrategy.returnDataset(username, password, uri)
    }

    override fun setResultAndFinish(result: Int, resultIntent: Intent) {
        setResult(result, resultIntent)
        finish()
    }

    override fun hideUpdateButton() {
        binding.createButton.hide()
    }

    override fun showUpdateButton() {
        binding.createButton.show()
    }

    override fun scrollResourcesToPosition(index: Int) {
        binding.recyclerView.scrollToPosition(index)
    }

    enum class State(
        val progressVisible: Boolean,
        val errorVisible: Boolean,
        val emptyVisible: Boolean,
        val listVisible: Boolean,
        val swipeProgressVisible: Boolean
    ) {
        EMPTY(false, false, true, false, false),
        SEARCH_EMPTY(false, false, true, false, false),
        ERROR(false, false, false, false, false),
        PROGRESS(true, false, false, false, false),
        SUCCESS(false, false, false, true, false)
    }
}
