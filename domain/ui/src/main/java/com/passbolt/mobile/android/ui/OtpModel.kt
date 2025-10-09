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

package com.passbolt.mobile.android.ui

import android.os.Parcelable
import com.passbolt.mobile.android.common.search.Searchable
import kotlinx.parcelize.Parcelize

@Parcelize
data class OtpResourceModel(
    val resourceId: String,
    val parentFolderId: String?,
    val label: String,
    val secret: String,
    val issuer: String?,
    val algorithm: String,
    val digits: Int,
    val period: Long,
) : Parcelable

data class OtpItemWrapper(
    val resource: ResourceModel,
    val isVisible: Boolean,
    val isRefreshing: Boolean,
    val otpExpirySeconds: Long?,
    val otpValue: String?,
    val remainingSecondsCounter: Long? = otpExpirySeconds,
) : Searchable by resource

@Parcelize
data class OtpAdvancedSettingsModel(
    val period: Long,
    val algorithm: String,
    val digits: Int,
) : Parcelable

fun List<OtpItemWrapper>.refreshingOnly(resourceId: String) =
    map { otp ->
        otp.copy(isRefreshing = otp.resource.resourceId == resourceId)
    }

fun List<OtpItemWrapper>.refreshingNone() =
    map { otp ->
        otp.copy(isRefreshing = false)
    }

fun List<OtpItemWrapper>.allReset() =
    map {
        it.copy(
            isVisible = false,
            isRefreshing = false,
            otpValue = null,
            otpExpirySeconds = null,
            remainingSecondsCounter = null,
        )
    }

fun List<OtpItemWrapper>.revealed(
    resourceId: String,
    otpValue: String?,
    otpPeriod: Long?,
    otpSecondsValid: Long?,
) = map { otp ->
    if (otp.resource.resourceId == resourceId) {
        otp.copy(
            isVisible = true,
            isRefreshing = false,
            otpValue = otpValue,
            otpExpirySeconds = otpPeriod,
            remainingSecondsCounter = otpSecondsValid,
        )
    } else {
        otp.copy(
            isVisible = false,
            isRefreshing = false,
            otpValue = null,
            otpExpirySeconds = null,
            remainingSecondsCounter = null,
        )
    }
}

fun List<OtpItemWrapper>.replaceOnId(updated: OtpItemWrapper) =
    map {
        if (it.resource.resourceId == updated.resource.resourceId) updated else it
    }

fun List<OtpItemWrapper>.findVisible() = find { it.isVisible }

fun OtpItemWrapper.isExpired(): Boolean = remainingSecondsCounter!! < 0
