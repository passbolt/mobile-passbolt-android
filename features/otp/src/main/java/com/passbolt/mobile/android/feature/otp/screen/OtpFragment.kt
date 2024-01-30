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

package com.passbolt.mobile.android.feature.otp.screen

import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import coil.ImageLoader
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.google.android.material.snackbar.Snackbar
import com.leinardi.android.speeddial.SpeedDialView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.passbolt.mobile.android.common.dialogs.confirmTotpDeletionAlertDialog
import com.passbolt.mobile.android.core.extension.gone
import com.passbolt.mobile.android.core.extension.px
import com.passbolt.mobile.android.core.extension.setSearchEndIconWithListener
import com.passbolt.mobile.android.core.extension.showSnackbar
import com.passbolt.mobile.android.core.extension.visible
import com.passbolt.mobile.android.core.localization.R
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.core.navigation.deeplinks.NavDeepLinkProvider
import com.passbolt.mobile.android.core.ui.initialsicon.InitialsIconGenerator
import com.passbolt.mobile.android.core.ui.progressdialog.hideProgressDialog
import com.passbolt.mobile.android.core.ui.progressdialog.showProgressDialog
import com.passbolt.mobile.android.feature.authentication.BindingScopedAuthenticatedFragment
import com.passbolt.mobile.android.feature.home.switchaccount.SwitchAccountBottomSheetFragment
import com.passbolt.mobile.android.feature.otp.createotpmanually.CreateOtpFragment
import com.passbolt.mobile.android.feature.otp.databinding.FragmentOtpBinding
import com.passbolt.mobile.android.feature.otp.scanotp.ScanOtpFragment
import com.passbolt.mobile.android.feature.otp.scanotp.parser.OtpParseResult
import com.passbolt.mobile.android.feature.otp.scanotpsuccess.ScanOtpSuccessFragment
import com.passbolt.mobile.android.feature.otp.screen.recycler.OtpItem
import com.passbolt.mobile.android.otpeditmoremenu.OtpUpdateMoreMenuFragment
import com.passbolt.mobile.android.otpmoremenu.OtpMoreMenuFragment
import com.passbolt.mobile.android.ui.OtpItemWrapper
import org.koin.android.ext.android.inject
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Suppress("TooManyFunctions")
class OtpFragment :
    BindingScopedAuthenticatedFragment<FragmentOtpBinding, OtpContract.View>(FragmentOtpBinding::inflate),
    OtpContract.View, SwitchAccountBottomSheetFragment.Listener, OtpMoreMenuFragment.Listener,
    OtpUpdateMoreMenuFragment.Listener {

    override val presenter: OtpContract.Presenter by inject()
    private val otpAdapter: ItemAdapter<OtpItem> by inject()
    private val fastAdapter: FastAdapter<GenericItem> by inject()
    private val initialsIconGenerator: InitialsIconGenerator by inject()
    private val imageLoader: ImageLoader by inject()
    private val clipboardManager: ClipboardManager? by inject()
    private val speedDialFabFactory: OtpSpeedDialFabFactory by inject()
    private var speedDialView: SpeedDialView? = null

    private val authenticationResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                // reinitialize for the switched account
                presenter.detach()
                presenter.attach(this)
            }
        }

    private val otpCreatedResult = { _: String, result: Bundle ->
        if (result.containsKey(CreateOtpFragment.EXTRA_OTP_CREATED) &&
            result.getBoolean(CreateOtpFragment.EXTRA_OTP_CREATED)
        ) {
            presenter.otpCreated()
        }
    }

    private val otpUpdatedResult = { _: String, result: Bundle ->
        if (result.containsKey(CreateOtpFragment.EXTRA_OTP_UPDATED) &&
            result.getBoolean(CreateOtpFragment.EXTRA_OTP_UPDATED)
        ) {
            presenter.otpUpdated()
        }
    }

    private val otpQrScannedResult = { _: String, result: Bundle ->
        if (result.containsKey(ScanOtpFragment.EXTRA_SCANNED_OTP)) {
            presenter.otpQrScanned(
                result.getParcelable(ScanOtpFragment.EXTRA_SCANNED_OTP)
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpRecycler()
        setupListeners()
        initSpeedDialFab()
        presenter.attach(this)
    }

    override fun onResume() {
        super.onResume()
        presenter.resume(this)
    }

    override fun onPause() {
        presenter.pause()
        super.onPause()
    }

    override fun onDestroyView() {
        speedDialView = null
        presenter.detach()
        super.onDestroyView()
    }

    private fun initSpeedDialFab() {
        with(speedDialFabFactory) {
            scanQrCodeClick = { presenter.scanOtpQrCodeClick() }
            createManuallyClick = { presenter.createOtpManuallyClick() }

            speedDialView = getSpeedDialFab(requireContext(), binding.overlay)
            speedDialView?.gone()
            binding.otpRootLayout.addView(speedDialView)
        }
    }

    private fun setUpRecycler() {
        fastAdapter.addEventHooks(
            listOf(
                OtpItem.ItemClick { presenter.otpItemClick(it) },
                OtpItem.ItemMoreClick { presenter.otpItemMoreClick(it) }
            )
        )
        with(binding.recyclerView) {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = fastAdapter
            itemAnimator = null
        }
    }

    private fun setupListeners() {
        with(binding) {
            swipeRefresh.setOnRefreshListener {
                presenter.refreshClick()
            }
            searchEditText.doAfterTextChanged {
                presenter.searchTextChanged(it.toString())
            }
        }
    }

    override fun hideRefreshProgress() {
        binding.swipeRefresh.isRefreshing = false
    }

    override fun showRefreshProgress() {
        binding.swipeRefresh.isRefreshing = true
    }

    override fun showOtpList(otpList: List<OtpItemWrapper>) {
        otpAdapter.set(otpList.map { OtpItem(it, initialsIconGenerator) })
        fastAdapter.notifyAdapterDataSetChanged()
    }

    override fun showEmptyView() {
        binding.emptyListContainer.visible()
    }

    override fun hideEmptyView() {
        binding.emptyListContainer.gone()
    }

    override fun displaySearchAvatar(avatarUrl: String?) {
        val request = ImageRequest.Builder(requireContext())
            .data(avatarUrl)
            .transformations(CircleCropTransformation())
            .size(AVATAR_SIZE, AVATAR_SIZE)
            .placeholder(CoreUiR.drawable.ic_avatar_placeholder)
            .target(
                onError = {
                    binding.searchTextInput.setSearchEndIconWithListener(
                        ContextCompat.getDrawable(requireContext(), CoreUiR.drawable.ic_avatar_placeholder)!!,
                        presenter::searchAvatarClick
                    )
                },
                onSuccess = {
                    binding.searchTextInput.setSearchEndIconWithListener(it, presenter::searchAvatarClick)
                }
            )
            .build()
        imageLoader.enqueue(request)
    }

    override fun navigateToSwitchAccount(appContext: AppContext) {
        SwitchAccountBottomSheetFragment.newInstance(appContext)
            .show(childFragmentManager, SwitchAccountBottomSheetFragment::class.java.name)
    }

    override fun navigateToEditOtpMenu() {
        OtpUpdateMoreMenuFragment()
            .show(childFragmentManager, OtpUpdateMoreMenuFragment::class.java.name)
    }

    override fun showOtmMoreMenu(resourceId: String, resourceName: String) {
        presenter.pause()
        OtpMoreMenuFragment.newInstance(resourceId, resourceName, true)
            .show(childFragmentManager, OtpMoreMenuFragment::class.java.name)
    }

    override fun switchAccountManageAccountClick() {
        presenter.switchAccountManageAccountClick()
    }

    override fun switchAccountClick() {
        presenter.switchAccountClick()
    }

    override fun navigateToManageAccounts() {
        authenticationResult.launch(
            ActivityIntents.authentication(
                requireContext(),
                ActivityIntents.AuthConfig.ManageAccount
            )
        )
    }

    override fun navigateToSwitchedAccountAuth(appContext: AppContext) {
        if (appContext == AppContext.APP) {
            requireActivity().finishAffinity()
        }
        // TODO handle autofill
        authenticationResult.launch(
            ActivityIntents.authentication(
                requireContext(),
                ActivityIntents.AuthConfig.Startup,
                appContext
            )
        )
    }

    override fun showPleaseWaitForDataRefresh() {
        Toast.makeText(requireContext(), LocalizationR.string.home_please_wait_for_refresh, Toast.LENGTH_SHORT).show()
    }

    override fun displaySearchClearIcon() {
        binding.searchTextInput.setSearchEndIconWithListener(
            ContextCompat.getDrawable(requireContext(), CoreUiR.drawable.ic_close)!!,
            presenter::searchClearClick
        )
    }

    override fun clearSearchInput() {
        binding.searchEditText.setText("")
    }

    override fun menuCopyOtpClick() {
        presenter.menuCopyOtpClick()
    }

    override fun menuShowOtpClick() {
        presenter.menuShowOtpClick()
    }

    override fun menuEditOtpClick() {
        presenter.menuEditOtpClick()
    }

    override fun menuDeleteOtpClick() {
        presenter.menuDeleteOtpClick()
    }

    override fun showTotpDeleted() {
        showSnackbar(LocalizationR.string.otp_deleted)
    }

    override fun copySecretToClipBoard(label: String, value: String) {
        clipboardManager?.setPrimaryClip(
            ClipData.newPlainText(label, value).apply {
                description.extras = PersistableBundle().apply {
                    putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                }
            }
        )
        Toast.makeText(requireContext(), getString(LocalizationR.string.copied_info, label), Toast.LENGTH_SHORT).show()
    }

    override fun navigateToScanOtpSuccess(totpQr: OtpParseResult.OtpQr.TotpQr) {
        setFragmentResultListener(
            ScanOtpSuccessFragment.REQUEST_SCAN_OTP,
            otpCreatedResult
        )
        findNavController().navigate(
            OtpFragmentDirections.actionOtpFragmentToScanOtpSuccessFragment(totpQr)
        )
    }

    override fun navigateToCreateOtpManually() {
        setFragmentResultListener(
            CreateOtpFragment.REQUEST_CREATE_OTP,
            otpCreatedResult
        )
        findNavController().navigate(
            NavDeepLinkProvider.otpManualFormDeepLinkRequest(null)
        )
    }

    override fun showDecryptionFailure() {
        showSnackbar(LocalizationR.string.common_decryption_failure, backgroundColor = CoreUiR.color.red)
    }

    override fun showFetchFailure() {
        showSnackbar(LocalizationR.string.common_fetch_failure, backgroundColor = CoreUiR.color.red)
    }

    override fun showConfirmDeleteDialog() {
        confirmTotpDeletionAlertDialog(requireContext()) {
            presenter.totpDeletionConfirmed()
        }.show()
    }

    override fun showResourceDeleted() {
        showSnackbar(LocalizationR.string.otp_deleted, backgroundColor = CoreUiR.color.green)
    }

    override fun showFailedToDeleteResource() {
        showSnackbar(LocalizationR.string.otp_failed_to_delete, backgroundColor = CoreUiR.color.red)
    }

    override fun showNewOtpCreated() {
        showSnackbar(LocalizationR.string.otp_new_otp_created, backgroundColor = CoreUiR.color.green)
    }

    override fun showOtpUpdate() {
        showSnackbar(LocalizationR.string.otp_otp_updated, backgroundColor = CoreUiR.color.green)
    }

    override fun menuEditOtpManuallyClick() {
        presenter.menuEditOtpManuallyClick()
    }

    override fun menuEditByNewOtpScanClick() {
        presenter.menuEditByQrScanClick()
    }

    override fun otpMenuDismissed() {
        presenter.resume(this)
    }

    override fun showProgress() {
        showProgressDialog(childFragmentManager)
    }

    override fun navigateToEditOtpManually(resourceId: String) {
        setFragmentResultListener(
            CreateOtpFragment.REQUEST_UPDATE_OTP,
            otpUpdatedResult
        )
        findNavController().navigate(
            NavDeepLinkProvider.otpManualFormDeepLinkRequest(resourceId)
        )
    }

    override fun navigateToScanOtpCodeForResult() {
        setFragmentResultListener(
            ScanOtpFragment.REQUEST_SCAN_OTP_FOR_RESULT,
            otpQrScannedResult
        )
        findNavController().navigate(
            OtpFragmentDirections.actionOtpFragmentToScanOtpFragment()
        )
    }

    override fun showInvalidQrCodeDataScanned() {
        showSnackbar(
            messageResId = LocalizationR.string.otp_invalid_itp_data_scanned,
            backgroundColor = CoreUiR.color.red
        )
    }

    override fun hideProgress() {
        hideProgressDialog(childFragmentManager)
    }

    override fun showEncryptionError(message: String) {
        showSnackbar(
            messageResId = LocalizationR.string.common_encryption_failure,
            backgroundColor = CoreUiR.color.red
        )
    }

    override fun showError(message: String) {
        showSnackbar(
            getString(LocalizationR.string.common_failure_format, message),
            backgroundColor = CoreUiR.color.red
        )
    }

    override fun showDataRefreshError() {
        showSnackbar(
            messageResId = R.string.common_data_refresh_error,
            backgroundColor = CoreUiR.color.red,
            length = Snackbar.LENGTH_LONG
        )
    }

    override fun showCreateButton() {
        speedDialView?.visible()
    }

    override fun hideCreateButton() {
        speedDialView?.gone()
    }

    override fun showJsonResourceSchemaValidationError() {
        showSnackbar(
            LocalizationR.string.common_json_schema_resource_validation_error,
            backgroundColor = CoreUiR.color.red
        )
    }

    override fun showJsonSecretSchemaValidationError() {
        showSnackbar(
            LocalizationR.string.common_json_schema_secret_validation_error,
            backgroundColor = CoreUiR.color.red
        )
    }

    private companion object {
        private val AVATAR_SIZE = 30.px
    }
}
