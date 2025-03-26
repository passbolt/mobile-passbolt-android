package com.passbolt.mobile.android.core.ui.progressdialog

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.passbolt.mobile.android.core.ui.databinding.DialogProgressBinding
import com.passbolt.mobile.android.core.ui.dialog.hideDialog
import com.passbolt.mobile.android.core.ui.dialog.showDialog

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
class ProgressDialog(private val dismissOnStop: Boolean = true) : DialogFragment(), DefaultLifecycleObserver {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        DialogProgressBinding.inflate(inflater).root

    override fun onAttach(context: Context) {
        super.onAttach(context)
        isCancelable = false
        (activity as LifecycleOwner).lifecycle.addObserver(this)
    }

    override fun onDetach() {
        (activity as LifecycleOwner).lifecycle.removeObserver(this)
        super.onDetach()
    }

    // hide dialog to prevent leaked widows after navigating back or parent recreation
    override fun onStop(owner: LifecycleOwner) {
        if (isAdded) {
            (activity as LifecycleOwner).lifecycle.removeObserver(this)
            if (dismissOnStop) {
                dismiss()
            }
        }
    }
}

fun showProgressDialog(fragmentManager: FragmentManager, dismissOnStop: Boolean = true) {
    showDialog(fragmentManager, ProgressDialog(dismissOnStop), ProgressDialog::class.java.name)
}

fun hideProgressDialog(fragmentManager: FragmentManager) {
    hideDialog(fragmentManager, ProgressDialog::class.java.name)
}
