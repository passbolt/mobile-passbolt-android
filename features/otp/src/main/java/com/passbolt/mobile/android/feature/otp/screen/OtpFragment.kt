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
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import coil3.ImageLoader
import coil3.asDrawable
import coil3.request.ImageRequest
import coil3.request.placeholder
import coil3.request.transformations
import coil3.transform.CircleCropTransformation
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.passbolt.mobile.android.common.dialogs.confirmTotpDeletionAlertDialog
import com.passbolt.mobile.android.core.clipboard.ClipboardAccess
import com.passbolt.mobile.android.core.extension.gone
import com.passbolt.mobile.android.core.extension.px
import com.passbolt.mobile.android.core.extension.setSearchEndIconWithListener
import com.passbolt.mobile.android.core.extension.showSnackbar
import com.passbolt.mobile.android.core.extension.visible
import com.passbolt.mobile.android.core.localization.R
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.core.resources.resourceicon.ResourceIconProvider
import com.passbolt.mobile.android.core.ui.progressdialog.hideProgressDialog
import com.passbolt.mobile.android.core.ui.progressdialog.showProgressDialog
import com.passbolt.mobile.android.createresourcemenu.CreateResourceMenuFragment
import com.passbolt.mobile.android.feature.authentication.BindingScopedAuthenticatedFragment
import com.passbolt.mobile.android.feature.home.screen.HomeFragmentDirections
import com.passbolt.mobile.android.feature.home.switchaccount.SwitchAccountBottomSheetFragment
import com.passbolt.mobile.android.feature.metadatakeytrust.ui.NewMetadataKeyTrustDialog
import com.passbolt.mobile.android.feature.metadatakeytrust.ui.NewTrustedMetadataKeyDeletedDialog
import com.passbolt.mobile.android.feature.otp.databinding.FragmentOtpBinding
import com.passbolt.mobile.android.feature.otp.scanotp.ScanOtpFragment
import com.passbolt.mobile.android.feature.otp.scanotp.ScanOtpMode
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessFragment
import com.passbolt.mobile.android.feature.otp.screen.recycler.OtpItem
import com.passbolt.mobile.android.feature.otp.screen.recycler.OtpItemUpdatePayload
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormFragment
import com.passbolt.mobile.android.otpmoremenu.OtpMoreMenuFragment
import com.passbolt.mobile.android.ui.LeadingContentType
import com.passbolt.mobile.android.ui.NewMetadataKeyToTrustModel
import com.passbolt.mobile.android.ui.OtpItemWrapper
import com.passbolt.mobile.android.ui.ResourceFormMode
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.TrustedKeyDeletedModel
import org.koin.android.ext.android.inject
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Suppress("TooManyFunctions")
class OtpFragment :
    BindingScopedAuthenticatedFragment<FragmentOtpBinding, OtpContract.View>(FragmentOtpBinding::inflate),
    OtpContract.View,
    SwitchAccountBottomSheetFragment.Listener,
    OtpMoreMenuFragment.Listener,
    CreateResourceMenuFragment.Listener,
    NewMetadataKeyTrustDialog.Listener,
    NewTrustedMetadataKeyDeletedDialog.Listener {
    override val presenter: OtpContract.Presenter by inject()
    private val otpAdapter: ItemAdapter<OtpItem> by inject()
    private val fastAdapter: FastAdapter<GenericItem> by inject()
    private val imageLoader: ImageLoader by inject()
    private val clipboardAccess: ClipboardAccess by inject()
    private val resourceIconProvider: ResourceIconProvider by inject()

    private val authenticationResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                // reinitialize for the switched account
                presenter.detach()
                presenter.attach(this)
            }
        }

    private val otpScanQrReturned = { _: String, result: Bundle ->
        presenter.otpQrScanReturned(
            result.getBoolean(ScanOtpSuccessFragment.EXTRA_OTP_CREATED, false),
            result.getBoolean(ScanOtpFragment.EXTRA_MANUAL_CREATION_CHOSEN),
        )
    }

    private val resourceFormReturned = { _: String, result: Bundle ->
        presenter.resourceFormReturned(
            result.getBoolean(ResourceFormFragment.EXTRA_RESOURCE_CREATED, false),
            result.getBoolean(ResourceFormFragment.EXTRA_RESOURCE_EDITED, false),
            result.getString(ResourceFormFragment.EXTRA_RESOURCE_NAME),
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        setUpRecycler()
        setupListeners()
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
        presenter.detach()
        super.onDestroyView()
    }

    override fun createTotpClick() {
        navigateToScanOtpCodeForResult()
    }

    override fun createPasswordClick() {
        findNavController().navigate(
            OtpFragmentDirections.actionHomeToResourceForm(
                ResourceFormMode.Create(
                    LeadingContentType.PASSWORD,
                    parentFolderId = null,
                ),
            ),
        )
    }

    override fun navigateToCreateTotpManually() {
        findNavController().navigate(
            OtpFragmentDirections.actionHomeToResourceForm(
                ResourceFormMode.Create(
                    LeadingContentType.TOTP,
                    parentFolderId = null,
                ),
            ),
        )
    }

    private fun setUpRecycler() {
        fastAdapter.addEventHooks(
            listOf(
                OtpItem.ItemClick { presenter.otpItemClick(it) },
                OtpItem.ItemMoreClick { presenter.otpItemMoreClick(it) },
            ),
        )
        with(requiredBinding.recyclerView) {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = fastAdapter
            itemAnimator = null
        }
    }

    private fun setupListeners() {
        with(requiredBinding) {
            swipeRefresh.setOnRefreshListener {
                presenter.refreshClick()
            }
            searchEditText.doAfterTextChanged {
                presenter.searchTextChanged(it.toString())
            }
            createResourceFab.setOnClickListener {
                showCreateResourceMenu()
            }
        }
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

    private fun showCreateResourceMenu() {
        CreateResourceMenuFragment
            .newInstance(homeDisplayViewModel = null)
            .show(childFragmentManager, CreateResourceMenuFragment::class.java.name)
    }

    override fun hideRefreshProgress() {
        requiredBinding.swipeRefresh.isRefreshing = false
    }

    override fun showRefreshProgress() {
        requiredBinding.swipeRefresh.isRefreshing = true
    }

    override fun showOtpList(
        otpList: List<OtpItemWrapper>,
        otpItemUpdatePayload: OtpItemUpdatePayload,
    ) {
        val newItems = otpList.map { OtpItem(it, resourceIconProvider) }
        otpAdapter.set(newItems)
        for (i in 0 until fastAdapter.itemCount) {
            fastAdapter.notifyAdapterItemChanged(i, otpItemUpdatePayload)
        }
    }

    override fun showEmptyView() {
        requiredBinding.emptyListContainer.visible()
    }

    override fun hideEmptyView() {
        requiredBinding.emptyListContainer.gone()
    }

    override fun displaySearchAvatar(avatarUrl: String?) {
        val request =
            ImageRequest
                .Builder(requireContext())
                .data(avatarUrl)
                .transformations(CircleCropTransformation())
                .size(AVATAR_SIZE, AVATAR_SIZE)
                .placeholder(CoreUiR.drawable.ic_avatar_placeholder)
                .target(
                    onError = {
                        requiredBinding.searchTextInput.setSearchEndIconWithListener(
                            ContextCompat.getDrawable(requireContext(), CoreUiR.drawable.ic_avatar_placeholder)!!,
                            presenter::searchAvatarClick,
                        )
                    },
                    onSuccess = {
                        requiredBinding.searchTextInput.setSearchEndIconWithListener(it.asDrawable(resources), presenter::searchAvatarClick)
                    },
                ).build()
        imageLoader.enqueue(request)
    }

    override fun navigateToSwitchAccount(appContext: AppContext) {
        SwitchAccountBottomSheetFragment
            .newInstance(appContext)
            .show(childFragmentManager, SwitchAccountBottomSheetFragment::class.java.name)
    }

    override fun showOtmMoreMenu(
        resourceId: String,
        resourceName: String,
    ) {
        presenter.pause()
        OtpMoreMenuFragment
            .newInstance(resourceId, resourceName, true)
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
                ActivityIntents.AuthConfig.ManageAccount,
            ),
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
                appContext,
            ),
        )
    }

    override fun showPleaseWaitForDataRefresh() {
        Toast.makeText(requireContext(), LocalizationR.string.home_please_wait_for_refresh, Toast.LENGTH_SHORT).show()
    }

    override fun displaySearchClearIcon() {
        requiredBinding.searchTextInput.setSearchEndIconWithListener(
            ContextCompat.getDrawable(requireContext(), CoreUiR.drawable.ic_close)!!,
            presenter::searchClearClick,
        )
    }

    override fun clearSearchInput() {
        requiredBinding.searchEditText.setText("")
    }

    override fun menuCopyOtpClick() {
        presenter.menuCopyOtpClick()
    }

    override fun menuShowOtpClick() {
        presenter.menuShowOtpClick()
    }

    override fun showTotpDeleted() {
        showSnackbar(LocalizationR.string.otp_deleted)
    }

    override fun copySecretToClipBoard(
        label: String,
        value: String,
    ) {
        clipboardAccess.setPrimaryClip(requireContext(), label, value, isSensitive = true)
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

    override fun otpMenuDismissed() {
        presenter.resume(this)
    }

    override fun showProgress() {
        showProgressDialog(childFragmentManager)
    }

    override fun navigateToScanOtpCodeForResult() {
        findNavController().navigate(
            OtpFragmentDirections.actionOtpFragmentToScanOtpFragment(ScanOtpMode.SCAN_WITH_SUCCESS_SCREEN),
        )
    }

    override fun hideProgress() {
        hideProgressDialog(childFragmentManager)
    }

    override fun showEncryptionError(message: String) {
        showSnackbar(
            messageResId = LocalizationR.string.common_encryption_failure,
            backgroundColor = CoreUiR.color.red,
        )
    }

    override fun showGeneralError(message: String) {
        showSnackbar(
            getString(LocalizationR.string.common_failure_format, message),
            backgroundColor = CoreUiR.color.red,
        )
    }

    override fun showDataRefreshError() {
        showSnackbar(
            messageResId = R.string.common_data_refresh_error,
            backgroundColor = CoreUiR.color.red,
            length = Snackbar.LENGTH_LONG,
        )
    }

    override fun showCreateButton() {
        this@OtpFragment.requiredBinding.createResourceFab.visible()
    }

    override fun hideCreateButton() {
        requiredBinding.createResourceFab.gone()
    }

    override fun showJsonResourceSchemaValidationError() {
        showSnackbar(
            LocalizationR.string.common_json_schema_resource_validation_error,
            backgroundColor = CoreUiR.color.red,
        )
    }

    override fun showJsonSecretSchemaValidationError() {
        showSnackbar(
            LocalizationR.string.common_json_schema_secret_validation_error,
            backgroundColor = CoreUiR.color.red,
        )
    }

    override fun menuEditOtpClick() {
        presenter.menuEditOtpClick()
    }

    override fun navigateToEditResource(resourceModel: ResourceModel) {
        findNavController().navigate(
            HomeFragmentDirections.actionHomeToResourceForm(
                ResourceFormMode.Edit(
                    resourceModel.resourceId,
                    resourceModel.metadataJsonModel.name,
                ),
            ),
        )
    }

    override fun menuDeleteOtpClick() {
        presenter.menuDeleteOtpClick()
    }

    override fun showResourceCreatedSnackbar() {
        showSnackbar(
            LocalizationR.string.resource_form_create_success,
            backgroundColor = CoreUiR.color.green,
        )
    }

    override fun showResourceEditedSnackbar(resourceName: String) {
        showSnackbar(
            messageResId = LocalizationR.string.common_message_resource_edited,
            messageArgs = arrayOf(resourceName),
            backgroundColor = CoreUiR.color.green,
        )
    }

    override fun showCannotUpdateTotpWithCurrentConfig() {
        showSnackbar(
            messageResId = LocalizationR.string.common_cannot_create_resource_with_current_config,
            backgroundColor = CoreUiR.color.red,
        )
    }

    override fun showMetadataKeyModifiedDialog(model: NewMetadataKeyToTrustModel) {
        NewMetadataKeyTrustDialog
            .newInstance(model)
            .show(childFragmentManager, NewMetadataKeyTrustDialog::class.java.name)
    }

    override fun showMetadataKeyDeletedDialog(model: TrustedKeyDeletedModel) {
        NewTrustedMetadataKeyDeletedDialog
            .newInstance(model)
            .show(childFragmentManager, NewTrustedMetadataKeyDeletedDialog::class.java.name)
    }

    override fun showFailedToVerifyMetadataKey() {
        showSnackbar(
            messageResId = LocalizationR.string.common_metadata_key_verification_failure,
            backgroundColor = CoreUiR.color.red,
        )
    }

    override fun trustNewMetadataKeyClick(newKeyToTrust: NewMetadataKeyToTrustModel) {
        presenter.trustNewMetadataKey(newKeyToTrust)
    }

    override fun trustMetadataKeyDeletionClick(model: TrustedKeyDeletedModel) {
        presenter.trustedMetadataKeyDeleted(model)
    }

    override fun showNewMetadataKeyIsTrusted() {
        showSnackbar(
            messageResId = LocalizationR.string.common_metadata_key_is_trusted,
            backgroundColor = CoreUiR.color.green,
        )
    }

    override fun showFailedToTrustMetadataKey() {
        showSnackbar(
            messageResId = LocalizationR.string.common_metadata_key_trust_failed,
            backgroundColor = CoreUiR.color.red,
        )
    }

    private companion object {
        private val AVATAR_SIZE = 30.px
    }
}
