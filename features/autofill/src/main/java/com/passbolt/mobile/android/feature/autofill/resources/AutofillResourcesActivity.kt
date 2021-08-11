package com.passbolt.mobile.android.feature.autofill.resources

import android.app.Activity
import android.app.assist.AssistStructure
import android.content.Intent
import android.os.Bundle
import android.service.autofill.Dataset
import android.view.autofill.AutofillManager
import android.view.autofill.AutofillManager.EXTRA_AUTHENTICATION_RESULT
import com.passbolt.mobile.android.common.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.mvp.scoped.BindingScopedActivity
import com.passbolt.mobile.android.feature.autofill.databinding.ActivityAutofillResourcesBinding
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
class AutofillResourcesActivity :
    BindingScopedActivity<ActivityAutofillResourcesBinding>(ActivityAutofillResourcesBinding::inflate),
    AutofillResourcesContract.View {
    private val presenter: AutofillResourcesContract.Presenter by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val structure = intent.getParcelableExtra<AssistStructure>(AutofillManager.EXTRA_ASSIST_STRUCTURE)

        if (structure == null) {
            navigateBack()
            return
        }
        presenter.attach(this)
        presenter.argsReceived(structure)
        setListeners()
    }

    private fun setListeners() {
        binding.returnButton.setDebouncingOnClick {
            presenter.returnClick()
        }
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
}
