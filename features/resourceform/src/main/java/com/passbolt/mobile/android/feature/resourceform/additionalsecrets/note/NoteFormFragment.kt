package com.passbolt.mobile.android.feature.resourceform.additionalsecrets.note

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.passbolt.mobile.android.core.extension.initDefaultToolbar
import com.passbolt.mobile.android.core.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.mvp.scoped.BindingScopedFragment
import com.passbolt.mobile.android.feature.resourceform.databinding.FragmentNoteFormBinding
import org.koin.android.ext.android.inject
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
class NoteFormFragment :
    BindingScopedFragment<FragmentNoteFormBinding>(
        FragmentNoteFormBinding::inflate,
    ),
    NoteFormContract.View {
    private val presenter: NoteFormContract.Presenter by inject()
    private val navArgs: NoteFormFragmentArgs by navArgs()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initDefaultToolbar(requiredBinding.toolbar)
        setListeners()
        presenter.attach(this)
        presenter.argsRetrieved(navArgs.mode, navArgs.note)
    }

    override fun showCreateTitle() {
        requiredBinding.toolbar.toolbarTitle = getString(LocalizationR.string.resource_form_create_note)
    }

    override fun showEditTitle(resourceName: String) {
        requiredBinding.toolbar.toolbarTitle = getString(LocalizationR.string.resource_form_edit_resource, resourceName)
    }

    private fun setListeners() {
        with(requiredBinding) {
            noteSubformView.noteInput.setTextChangeListener {
                presenter.noteTextChanged(it)
            }
            removeNote.setDebouncingOnClick {
                presenter.removeNoteClick()
            }
            apply.setDebouncingOnClick {
                presenter.applyClick()
            }
        }
    }

    override fun showNote(note: String) {
        requiredBinding.noteSubformView.noteInput.text = note
    }

    override fun goBackWithResult(note: String?) {
        setFragmentResult(
            REQUEST_NOTE,
            bundleOf(EXTRA_NOTE to note),
        )
        findNavController().popBackStack()
    }

    companion object {
        const val REQUEST_NOTE = "NOTE"

        const val EXTRA_NOTE = "note"
    }
}
