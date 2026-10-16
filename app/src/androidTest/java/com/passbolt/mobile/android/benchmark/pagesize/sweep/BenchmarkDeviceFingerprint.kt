/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2026 Passbolt SA
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

package com.passbolt.mobile.android.benchmark.pagesize.sweep

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.passbolt.mobile.android.common.device.DevicePerformanceFingerprint
import com.passbolt.mobile.android.common.device.DevicePerformanceTier
import com.passbolt.mobile.android.common.device.classifyDevicePerformanceTier

data class BenchmarkDeviceFingerprint(
    val manufacturer: String,
    val model: String,
    val sdkInt: Int,
    val totalMemMb: Int,
    val availMemMb: Int,
    val memoryClassMb: Int,
    val largeMemoryClassMb: Int,
    val maxHeapMb: Int,
    val isLowRamDevice: Boolean,
    val availableProcessors: Int,
    val mediaPerformanceClass: Int,
    val primaryAbi: String,
) {
    val predictedTier: DevicePerformanceTier
        get() = classifyDevicePerformanceTier(toDevicePerformanceFingerprint())

    private fun toDevicePerformanceFingerprint() =
        DevicePerformanceFingerprint(
            isLowRamDevice = isLowRamDevice,
            totalMemoryMb = totalMemMb,
            memoryClassMb = memoryClassMb,
        )

    companion object {
        fun capture(context: Context): BenchmarkDeviceFingerprint {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo)
            val runtime = Runtime.getRuntime()
            return BenchmarkDeviceFingerprint(
                manufacturer = Build.MANUFACTURER,
                model = Build.MODEL,
                sdkInt = Build.VERSION.SDK_INT,
                totalMemMb = (memoryInfo.totalMem / BYTES_IN_MB).toInt(),
                availMemMb = (memoryInfo.availMem / BYTES_IN_MB).toInt(),
                memoryClassMb = activityManager.memoryClass,
                largeMemoryClassMb = activityManager.largeMemoryClass,
                maxHeapMb = (runtime.maxMemory() / BYTES_IN_MB).toInt(),
                isLowRamDevice = activityManager.isLowRamDevice,
                availableProcessors = runtime.availableProcessors(),
                mediaPerformanceClass = Build.VERSION.MEDIA_PERFORMANCE_CLASS,
                primaryAbi = Build.SUPPORTED_ABIS.firstOrNull().orEmpty(),
            )
        }

        private const val BYTES_IN_MB = 1024L * 1024L
    }
}
