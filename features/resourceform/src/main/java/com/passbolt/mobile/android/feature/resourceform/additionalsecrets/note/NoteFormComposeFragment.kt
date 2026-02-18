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

package com.passbolt.mobile.android.feature.resourceform.additionalsecrets.note

import PassboltTheme
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

class NoteFormComposeFragment :
    Fragment(),
    NoteFormNavigation {
    private val navArgs: NoteFormComposeFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setContent {
                koinViewModel<NoteFormViewModel>(
                    parameters = {
                        parametersOf(navArgs.mode, navArgs.mode)
                    },
                )

                PassboltTheme {
                    NoteFormScreen(
                        navigation = this@NoteFormComposeFragment,
                    )
                }
            }
        }

    override fun navigateBack() {
        findNavController().popBackStack()
    }

    override fun navigateBackWithResult(note: String?) {
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
