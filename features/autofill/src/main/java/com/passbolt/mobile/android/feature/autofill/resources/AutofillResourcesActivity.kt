package com.passbolt.mobile.android.feature.autofill.resources

import android.app.Activity
import android.app.assist.AssistStructure
import android.content.Intent
import android.os.Bundle
import android.view.autofill.AutofillManager.EXTRA_ASSIST_STRUCTURE
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.snackbar.Snackbar
import com.passbolt.mobile.android.common.lifecycleawarelazy.lifecycleAwareLazy
import com.passbolt.mobile.android.core.extension.findNavHostFragment
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.core.navigation.AutofillMode
import com.passbolt.mobile.android.core.ui.progressdialog.hideProgressDialog
import com.passbolt.mobile.android.core.ui.progressdialog.showProgressDialog
import com.passbolt.mobile.android.feature.authentication.BindingScopedAuthenticatedActivity
import com.passbolt.mobile.android.feature.autofill.R
import com.passbolt.mobile.android.feature.autofill.databinding.ActivityAutofillResourcesBinding
import com.passbolt.mobile.android.feature.autofill.resources.datasetstrategy.ReturnAutofillDatasetStrategy
import com.passbolt.mobile.android.feature.home.screen.DataRefreshStatus
import com.passbolt.mobile.android.feature.home.screen.HomeDataRefreshExecutor
import com.passbolt.mobile.android.feature.home.screen.ShowSuggestedModel
import com.passbolt.mobile.android.ui.ResourceModel
import kotlinx.coroutines.flow.Flow
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

class AutofillResourcesActivity :
    BindingScopedAuthenticatedActivity<ActivityAutofillResourcesBinding, AutofillResourcesContract.View>(
        ActivityAutofillResourcesBinding::inflate
    ), AutofillResourcesContract.View, HomeDataRefreshExecutor {

    override val presenter: AutofillResourcesContract.Presenter by inject()
    override val appContext = AppContext.AUTOFILL

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        returnAutofillDatasetStrategy = scope.get(named(bundledAutofillMode)) { parametersOf(this) }
        presenter.attach(this)
        presenter.argsReceived(bundledAutofillUri)
    }

    override fun onDestroy() {
        returnAutofillDatasetStrategy.detach()
        presenter.detach()
        super.onDestroy()
    }

    override fun navigateToAutofillHome() {
        findNavHostFragment(binding.fragmentContainer.id).navController.setGraph(R.navigation.home)
    }

    override fun performFullDataRefresh() =
        presenter.performFullDataRefresh()

    override fun performLocalDataRefresh() =
        presenter.performLocalDataRefresh()

    override fun supplyFullDataRefreshStatusFlow(): Flow<DataRefreshStatus.Finished> =
        presenter.dataRefreshFinishedStatusFlow

    override fun navigateToAuth() {
        initialAuthenticationResult.launch(
            ActivityIntents.authentication(
                this,
                ActivityIntents.AuthConfig.RefreshSession,
                appContext = AppContext.AUTOFILL
            )
        )
    }

    override fun finishAutofill() {
        finishAffinity()
    }

    override fun navigateToSetup() {
        startActivity(ActivityIntents.start(this))
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

    override fun resourceItemClick(resourceModel: ResourceModel) {
        presenter.itemClick(resourceModel)
    }

    override fun shouldShowResourceMoreMenu() = false

    override fun shouldShowFolderMoreMenu() = false

    override fun shouldShowCloseButton() = true

    override fun showSuggestedModel() =
        bundledAutofillUri?.let {
            ShowSuggestedModel.Show(it)
        } ?: ShowSuggestedModel.DoNotShow

    override fun resourcePostCreateAction(resourceId: String) {
        presenter.newResourceCreated(resourceId)
    }

    override fun showProgress() {
        showProgressDialog(supportFragmentManager)
    }

    override fun hideProgress() {
        hideProgressDialog(supportFragmentManager)
    }

    override fun showError(message: String?) {
        Snackbar.make(binding.root, getString(R.string.common_failure_format, message), Snackbar.LENGTH_LONG)
            .show()
    }
}
