package com.passbolt.mobile.android.logs

import PassboltTheme
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.FileProvider.getUriForFile
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import java.io.File
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

@Deprecated("Use to integrate with legacy navigation only, use LogsScreen for Compose")
class LogsComposeFragment :
    Fragment(),
    LogsNavigation {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setContent {
                PassboltTheme {
                    LogsScreenCompat(
                        navigation = this@LogsComposeFragment,
                    )
                }
            }
        }

    override fun navigateUp() {
        findNavController().popBackStack()
    }

    override fun navigateToLogsShareSheet(logFilePath: String) {
        val contentUri =
            getUriForFile(
                requireContext(),
                getLogsFileProviderAuthority(requireContext()),
                File(logFilePath),
            )

        val sendIntent =
            Intent().apply {
                action = Intent.ACTION_SEND
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                type = LOGS_MIME_TYPE

                putExtra(Intent.EXTRA_STREAM, contentUri)
            }

        startActivity(
            Intent.createChooser(
                sendIntent,
                getString(LocalizationR.string.logs_share_title),
            ),
        )
    }

    companion object {
        const val LOGS_MIME_TYPE = "text/plain"

        fun getLogsFileProviderAuthority(context: Context): String = "${context.packageName}.core.logger.logsfileprovider"
    }
}
