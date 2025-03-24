package com.passbolt.mobile.android.feature.resourceform.additionalsecrets.securenote

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.passbolt.mobile.android.core.extension.initDefaultToolbar
import com.passbolt.mobile.android.core.mvp.scoped.BindingScopedFragment
import com.passbolt.mobile.android.feature.resourceform.databinding.FragmentSecureNoteFormBinding
import org.koin.android.ext.android.inject
import androidx.navigation.fragment.navArgs
import com.passbolt.mobile.android.core.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.localization.R as LocalizationR

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
class SecureNoteFormFragment :
    BindingScopedFragment<FragmentSecureNoteFormBinding>(
        FragmentSecureNoteFormBinding::inflate
    ), SecureNoteFormContract.View {

    private val presenter: SecureNoteFormContract.Presenter by inject()
    private val navArgs: SecureNoteFormFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDefaultToolbar(binding.toolbar)
        setListeners()
        presenter.attach(this)
        presenter.argsRetrieved(navArgs.mode, navArgs.secureNote)
    }

    override fun showCreateTitle() {
        binding.toolbar.toolbarTitle = getString(LocalizationR.string.resource_form_create_secure_note)
    }

    private fun setListeners() {
        with(binding) {
            secureNoteSubformView.secureNoteInput.setTextChangeListener {
                presenter.secureNoteTextChanged(it)
            }
            apply.setDebouncingOnClick {
                presenter.applyClick()
            }
        }
    }

    override fun showSecureNote(secureNote: String) {
        binding.secureNoteSubformView.secureNoteInput.text = secureNote
    }

    override fun goBackWithResult(secureNote: String) {
        setFragmentResult(
            REQUEST_SECURE_NOTE,
            bundleOf(EXTRA_SECURE_NOTE to secureNote)
        )
        findNavController().popBackStack()
    }

    companion object {
        const val REQUEST_SECURE_NOTE = "SECURE_NOTE"

        const val EXTRA_SECURE_NOTE = "secure_note"
    }
}
