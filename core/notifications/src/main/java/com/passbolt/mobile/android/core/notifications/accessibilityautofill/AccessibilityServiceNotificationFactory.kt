package com.passbolt.mobile.android.core.notifications.accessibilityautofill

import android.app.Notification
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.app.NotificationManager.IMPORTANCE_LOW
import android.content.Context
import com.passbolt.mobile.android.core.notifications.NotificationChannelManager
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

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
    private val notificationChannelManager: NotificationChannelManager,
) {
    fun getAccessibilityServiceNotification(context: Context): Notification {
        notificationChannelManager.createNotificationChannel(
            channelId = AUTOFILL_SERVICES_CHANNEL_ID,
            name = context.getString(LocalizationR.string.autofill_service_channel_name),
            importance = IMPORTANCE_HIGH,
        )
        return Notification
            .Builder(context, AUTOFILL_SERVICES_CHANNEL_ID)
            .setContentTitle(context.getString(LocalizationR.string.autofill_service_title))
            .setContentText(context.getString(LocalizationR.string.autofill_service_content))
            .setSmallIcon(CoreUiR.drawable.ic_key)
            .build()
    }

    fun getDataServiceNotification(context: Context): Notification {
        notificationChannelManager.createNotificationChannel(
            channelId = DATA_SYNC_SERVICES_CHANNEL_ID,
            name = context.getString(LocalizationR.string.data_sync_service_channel_name),
            importance = IMPORTANCE_LOW,
        )
        return Notification
            .Builder(context, DATA_SYNC_SERVICES_CHANNEL_ID)
            .setContentTitle(context.getString(LocalizationR.string.autofill_service_title))
            .setContentText(context.getString(LocalizationR.string.data_sync_service_content))
            .setSmallIcon(CoreUiR.drawable.ic_key)
            .setProgress(0, 0, true)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val ACCESSIBILITY_SERVICE_NOTIFICATION_ID = 1000
        const val DATA_SYNC_SERVICE_NOTIFICATION_ID = 1001

        private const val AUTOFILL_SERVICES_CHANNEL_ID = "ForegroundServiceChannel"
        private const val DATA_SYNC_SERVICES_CHANNEL_ID = "DataSyncForegroundServiceChannel"
    }
}
