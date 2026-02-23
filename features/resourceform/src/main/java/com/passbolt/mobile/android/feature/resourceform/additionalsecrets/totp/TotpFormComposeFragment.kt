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

package com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp

import PassboltTheme
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.passbolt.mobile.android.feature.otp.scanotp.ScanOtpFragment
import com.passbolt.mobile.android.feature.otp.scanotp.ScanOtpMode
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpFormIntent.AdvancedSettingsChanged
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpFormIntent.TotpScanned
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.advanced.TotpAdvancedSettingsFormComposeFragment
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormFragmentDirections
import com.passbolt.mobile.android.ui.OtpParseResult
import com.passbolt.mobile.android.ui.ResourceFormMode
import com.passbolt.mobile.android.ui.TotpUiModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class TotpFormComposeFragment :
    Fragment(),
    TotpFormNavigation {
    private val navArgs: TotpFormComposeFragmentArgs by navArgs()
    private val viewModel: TotpFormViewModel by viewModel { parametersOf(navArgs.mode, navArgs.totpUiModel) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setContent {
                PassboltTheme {
                    TotpFormScreen(
                        navigation = this@TotpFormComposeFragment,
                        viewModel = viewModel,
                    )
                }
            }
        }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        // TODO: replace this result navigation with Nav3/EventBus once it's merged
        setFragmentResultListener(TotpAdvancedSettingsFormComposeFragment.REQUEST_TOTP_ADVANCED) { _, result ->
            viewModel.onIntent(
                AdvancedSettingsChanged(
                    BundleCompat.getParcelable(
                        result,
                        TotpAdvancedSettingsFormComposeFragment.EXTRA_TOTP_ADVANCED,
                        TotpUiModel::class.java,
                    ),
                ),
            )
        }
        // TODO: replace this result navigation with Nav3/EventBus once it's merged
        setFragmentResultListener(ScanOtpFragment.REQUEST_SCAN_OTP_FOR_RESULT) { _, result ->
            viewModel.onIntent(
                TotpScanned(
                    isManualCreationChosen = result.getBoolean(ScanOtpFragment.EXTRA_MANUAL_CREATION_CHOSEN),
                    totpQr =
                        BundleCompat.getParcelable(
                            result,
                            ScanOtpFragment.EXTRA_SCANNED_OTP,
                            OtpParseResult.OtpQr.TotpQr::class.java,
                        ),
                ),
            )
        }
    }

    override fun navigateBack() {
        findNavController().popBackStack()
    }

    // TODO: replace this result navigation with Nav3/EventBus once it's merged
    override fun navigateBackWithResult(totpUiModel: TotpUiModel?) {
        setFragmentResult(
            REQUEST_TOTP,
            bundleOf(EXTRA_TOTP to totpUiModel),
        )
        findNavController().popBackStack()
    }

    override fun navigateToAdvancedSettings(
        mode: ResourceFormMode,
        totpUiModel: TotpUiModel,
    ) {
        findNavController().navigate(
            TotpFormComposeFragmentDirections.actionTotpFormFragmentToTotpAdvancedSettingsFormFragment(
                mode,
                totpUiModel,
            ),
        )
    }

    override fun navigateToScanTotp() {
        findNavController().navigate(
            ResourceFormFragmentDirections.actionResourceFormFragmentToScanOtp(ScanOtpMode.SCAN_FOR_RESULT),
        )
    }

    companion object {
        const val REQUEST_TOTP = "TOTP"

        const val EXTRA_TOTP = "totp"
    }
}
