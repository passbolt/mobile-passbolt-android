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
package com.passbolt.mobile.android.core.clipboard

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipDescription.MIMETYPE_TEXT_PLAIN
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PersistableBundle
import android.os.SystemClock
import android.widget.Toast
import com.passbolt.mobile.android.core.localization.R as LocalizationR

class ClipboardAccess(
    private val clipboardManager: ClipboardManager?,
    private val alarmManager: AlarmManager,
) {
    private var currentClearClipboardPendingIntent: PendingIntent? = null

    fun setPrimaryClip(
        context: Context,
        label: String,
        value: String,
        isSensitive: Boolean,
    ) {
        clipboardManager?.setPrimaryClip(
            ClipData.newPlainText(label, value).apply {
                description.extras =
                    PersistableBundle().apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, isSensitive)
                        }
                    }
            },
        )
        showToastForOlderAndroidVersions(context, label)
        scheduleClearPrimaryClip(context)
    }

    private fun showToastForOlderAndroidVersions(
        context: Context,
        label: String,
    ) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S) {
            Toast.makeText(context, context.getString(LocalizationR.string.copied_info, label), Toast.LENGTH_SHORT).show()
        }
    }

    private fun scheduleClearPrimaryClip(context: Context) {
        currentClearClipboardPendingIntent?.let { alarmManager.cancel(it) }

        val intent = Intent(context, ClearClipboardReceiver::class.java)
        currentClearClipboardPendingIntent =
            PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE,
            )

        val triggerAt = SystemClock.elapsedRealtime() + CLIPBOARD_CLEAR_DELAY_MILLIS
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, requireNotNull(currentClearClipboardPendingIntent))
    }

    fun getPrimaryClipTextOrNull(): CharSequence? =
        if (clipboardManager?.hasPrimaryClip() == true &&
            clipboardManager.primaryClipDescription?.hasMimeType(MIMETYPE_TEXT_PLAIN) == true
        ) {
            clipboardManager.primaryClip?.getItemAt(0)?.text
        } else {
            null
        }

    fun clearPrimaryClip() {
        clipboardManager?.setPrimaryClip(ClipData.newPlainText("", ""))
    }

    private companion object {
        private const val CLIPBOARD_CLEAR_DELAY_MILLIS = 30_000L
    }
}
