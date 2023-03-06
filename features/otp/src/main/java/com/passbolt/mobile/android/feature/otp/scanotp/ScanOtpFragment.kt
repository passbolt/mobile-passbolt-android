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

package com.passbolt.mobile.android.feature.otp.scanotp

import android.os.Bundle
import android.view.View

import androidx.navigation.fragment.findNavController
import com.passbolt.mobile.android.core.extension.initDefaultToolbar
import com.passbolt.mobile.android.core.mvp.scoped.BindingScopedFragment
import com.passbolt.mobile.android.core.qrscan.SCAN_MANAGER_SCOPE
import com.passbolt.mobile.android.core.qrscan.manager.ScanManager
import com.passbolt.mobile.android.core.security.flagsecure.FlagSecureSetter
import com.passbolt.mobile.android.feature.otp.R
import com.passbolt.mobile.android.feature.otp.databinding.FragmentScanOtpBinding
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope

class ScanOtpFragment : BindingScopedFragment<FragmentScanOtpBinding>(FragmentScanOtpBinding::inflate),
    ScanOtpContract.View {

    private val presenter: ScanOtpContract.Presenter by inject()
    private lateinit var scanManagerScope: Scope
    private lateinit var scanManager: ScanManager
    private val flagSecureSetter: FlagSecureSetter by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDefaultToolbar(binding.toolbar)
        scanManagerScope = getKoin().getOrCreateScope(SCAN_MANAGER_SCOPE, named(SCAN_MANAGER_SCOPE))
        scanManager = scanManagerScope.get()

        presenter.attach(this)
    }

    override fun onResume() {
        super.onResume()
        presenter.viewResumed()
    }

    override fun onPause() {
        presenter.viewPaused()
        super.onPause()
    }

    override fun onDestroyView() {
        scanManager.detach()
        scanManagerScope.close()
        presenter.detach()
        super.onDestroyView()
    }

    override fun scanResultChannel() = scanManager.barcodeScanPublisher

    override fun startAnalysis() {
        try {
            scanManager.attach(this, binding.cameraPreview)
        } catch (exception: Exception) {
            presenter.startCameraError(exception)
        }
    }

    override fun showStartCameraError() {
        binding.tooltip.text = getString(R.string.scan_qr_camera_error)
    }

    override fun showBarcodeScanError(message: String?) {
        val messageBuilder = StringBuilder(getString(R.string.scan_qr_scanning_error)).apply {
            if (!message.isNullOrBlank()) {
                append("(%s)".format(message))
            }
        }
        binding.tooltip.text = messageBuilder.toString()
    }

    override fun showMultipleCodesInRange() {
        binding.tooltip.text = getString(R.string.scan_qr_multiple_codes_in_range)
    }

    override fun showCenterCameraOnBarcode() {
        binding.tooltip.text = getString(R.string.scan_qr_aim_at_qr_code)
    }

    override fun showNotAnOtpBarcode() {
        binding.tooltip.text = getString(R.string.otp_scan_not_a_totp_qr)
    }

    override fun navigateToCreateOtpSuccess() {
        findNavController().navigate(
            ScanOtpFragmentDirections.actionScanOtpFragmentToCreateOtpSuccessFragment()
        )
    }

    override fun setFlagSecure() {
        flagSecureSetter.set(requireActivity())
    }

    override fun removeFlagSecure() {
        flagSecureSetter.remove(requireActivity())
    }
}
