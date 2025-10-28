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

package com.passbolt.mobile.android.feature.autofill.resources

import android.app.Activity
import android.app.assist.AssistStructure
import android.content.Intent
import android.os.Bundle
import android.view.autofill.AutofillManager.EXTRA_ASSIST_STRUCTURE
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.IntentCompat
import com.passbolt.mobile.android.common.lifecycleawarelazy.lifecycleAwareLazy
import com.passbolt.mobile.android.core.extension.findNavHostFragment
import com.passbolt.mobile.android.core.fulldatarefresh.service.DataRefreshService
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.core.navigation.AutofillMode
import com.passbolt.mobile.android.core.ui.progressdialog.hideProgressDialog
import com.passbolt.mobile.android.core.ui.progressdialog.showProgressDialog
import com.passbolt.mobile.android.feature.authentication.BindingScopedAuthenticatedActivity
import com.passbolt.mobile.android.feature.autofill.databinding.ActivityAutofillResourcesBinding
import com.passbolt.mobile.android.feature.autofill.resources.datasetstrategy.ReturnAutofillDatasetStrategy
import com.passbolt.mobile.android.feature.home.screen.ShowSuggestedModel
import com.passbolt.mobile.android.ui.ResourceModel
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import com.passbolt.mobile.android.core.localization.R as LocalizationR

// NOTE: When changing name or package read core/navigation/README.md
class AutofillResourcesActivity :
    BindingScopedAuthenticatedActivity<ActivityAutofillResourcesBinding, AutofillResourcesContract.View>(
        ActivityAutofillResourcesBinding::inflate,
    ),
    AutofillResourcesContract.View {
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
        presenter.argsReceived(bundledAutofillUri, isRecreated = savedInstanceState != null)
    }

    override fun onDestroy() {
        returnAutofillDatasetStrategy.detach()
        presenter.detach()
        super.onDestroy()
    }

    override fun navigateToAutofillHome() {
        findNavHostFragment(requiredBinding.fragmentContainer.id)
            .navController
            .setGraph(com.passbolt.mobile.android.feature.home.R.navigation.home)
    }

    override fun navigateToAuth() {
        initialAuthenticationResult.launch(
            ActivityIntents.authentication(
                this,
                ActivityIntents.AuthConfig.RefreshSession,
                appContext = AppContext.AUTOFILL,
            ),
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
        requireNotNull(
            IntentCompat.getParcelableExtra(intent, EXTRA_ASSIST_STRUCTURE, AssistStructure::class.java),
        )

    override fun autofillReturn(
        username: String,
        password: String,
        uri: String?,
    ) {
        returnAutofillDatasetStrategy.returnDataset(username, password, uri)
    }

    override fun setResultAndFinish(
        result: Int,
        resultIntent: Intent,
    ) {
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

    override fun showDecryptionFailure() {
        Toast
            .makeText(this, LocalizationR.string.common_decryption_failure, Toast.LENGTH_SHORT)
            .show()
    }

    override fun showFetchFailure() {
        Toast
            .makeText(this, LocalizationR.string.common_fetch_failure, Toast.LENGTH_SHORT)
            .show()
    }

    override fun performFullDataRefresh() {
        DataRefreshService.start(this)
    }
}
