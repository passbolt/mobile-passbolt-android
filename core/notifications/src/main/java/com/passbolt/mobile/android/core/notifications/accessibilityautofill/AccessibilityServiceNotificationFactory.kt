package com.passbolt.mobile.android.core.notifications.accessibilityautofill

import android.app.Notification
import android.content.Context
import com.passbolt.mobile.android.core.notifications.NotificationChannelManager
import com.passbolt.mobile.android.notifications.R

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
class AccessibilityServiceNotificationFactory(
    private val notificationChannelManager: NotificationChannelManager
) {

    fun getNotification(context: Context): Notification {
        notificationChannelManager.createNotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.autofill_service_channel_name)
        )
        return Notification.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.autofill_service_title))
            .setContentText(context.getString(R.string.autofill_service_content))
            .setSmallIcon(R.drawable.ic_key)
            .build()
    }

    private companion object {
        private const val CHANNEL_ID = "ForegroundServiceChannel"
    }
}
