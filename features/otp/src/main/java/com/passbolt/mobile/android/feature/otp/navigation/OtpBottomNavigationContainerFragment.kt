package com.passbolt.mobile.android.feature.otp.navigation

import PassboltTheme
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation.fragment.findNavController
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
import com.passbolt.mobile.android.feature.otp.scanotp.ScanOtpFragment
import com.passbolt.mobile.android.feature.otp.scanotp.ScanOtpMode
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessFragment
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent
import com.passbolt.mobile.android.feature.otp.screen.OtpNavigation
import com.passbolt.mobile.android.feature.otp.screen.OtpScreen
import com.passbolt.mobile.android.feature.otp.screen.OtpViewModel
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormFragment
import com.passbolt.mobile.android.ui.LeadingContentType
import com.passbolt.mobile.android.ui.ResourceFormMode
import org.koin.androidx.compose.koinViewModel

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
class OtpBottomNavigationContainerFragment :
    Fragment(),
    OtpNavigation {
    private val otpScanQrReturned = { _: String, result: Bundle ->
        viewModel.onIntent(
            OtpIntent.OtpQRScanReturned(
                otpCreated = result.getBoolean(ScanOtpSuccessFragment.EXTRA_OTP_CREATED, false),
                otpManualCreationChosen = result.getBoolean(ScanOtpFragment.EXTRA_MANUAL_CREATION_CHOSEN),
            ),
        )
    }

    private val resourceFormReturned = { _: String, result: Bundle ->
        viewModel.onIntent(
            OtpIntent.ResourceFormReturned(
                result.getBoolean(ResourceFormFragment.EXTRA_RESOURCE_CREATED, false),
                result.getBoolean(ResourceFormFragment.EXTRA_RESOURCE_EDITED, false),
                result.getString(ResourceFormFragment.EXTRA_RESOURCE_NAME),
            ),
        )
    }

    private lateinit var viewModel: OtpViewModel
    private lateinit var backstackList: NavBackStack

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setContent {
                val backStack =
                    rememberNavBackStack<NavKey>(Otp).apply {
                        backstackList = this
                    }

                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    entryDecorators =
                        listOf(
                            rememberSceneSetupNavEntryDecorator(),
                            rememberSavedStateNavEntryDecorator(),
                            rememberViewModelStoreNavEntryDecorator(),
                        ),
                    entryProvider = { key ->
                        when (key) {
                            is Otp ->
                                NavEntry(key) {
                                    viewModel = koinViewModel()
                                    PassboltTheme {
                                        PassboltTheme {
                                            OtpScreen(
                                                navigation = this@OtpBottomNavigationContainerFragment,
                                                viewModel = viewModel,
                                            )
                                        }
                                    }
                                }
                            else -> error("Unsupported home key: $key")
                        }
                    },
                )
            }
        }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        setFragmentResultListeners()
    }

    private fun setFragmentResultListeners() {
        setFragmentResultListener(
            ScanOtpFragment.REQUEST_SCAN_OTP_FOR_RESULT,
            otpScanQrReturned,
        )
        setFragmentResultListener(
            ResourceFormFragment.REQUEST_RESOURCE_FORM,
            resourceFormReturned,
        )
    }

    override fun navigateToCreateResourceForm(leadingContentType: LeadingContentType) {
        findNavController().navigate(
            OtpBottomNavigationContainerFragmentDirections.actionOtpToResourceForm(
                ResourceFormMode.Create(
                    leadingContentType,
                    parentFolderId = null,
                ),
            ),
        )
    }

    override fun navigateToEditResourceForm(
        resourceId: String,
        resourceName: String,
    ) {
        findNavController().navigate(
            OtpBottomNavigationContainerFragmentDirections.actionOtpToResourceForm(
                ResourceFormMode.Edit(
                    resourceId = resourceId,
                    resourceName = resourceName,
                ),
            ),
        )
    }

    override fun navigateToScanOtpCodeForResult() {
        findNavController().navigate(
            OtpBottomNavigationContainerFragmentDirections.actionOtpComposeFragmentToScanOtpFragment(ScanOtpMode.SCAN_WITH_SUCCESS_SCREEN),
        )
    }
}
