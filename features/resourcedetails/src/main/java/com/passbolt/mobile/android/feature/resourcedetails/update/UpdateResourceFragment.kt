package com.passbolt.mobile.android.feature.resourcedetails.update

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.passbolt.mobile.android.common.dialogs.encryptionErrorAlertDialog
import com.passbolt.mobile.android.common.lifecycleawarelazy.lifecycleAwareLazy
import com.passbolt.mobile.android.core.extension.gone
import com.passbolt.mobile.android.core.extension.initDefaultToolbar
import com.passbolt.mobile.android.core.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.extension.showSnackbar
import com.passbolt.mobile.android.core.extension.visible
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.ActivityResults
import com.passbolt.mobile.android.core.ui.progressdialog.hideProgressDialog
import com.passbolt.mobile.android.core.ui.progressdialog.showProgressDialog
import com.passbolt.mobile.android.core.ui.textinputfield.PasswordGenerateInputView
import com.passbolt.mobile.android.core.ui.textinputfield.StatefulInput
import com.passbolt.mobile.android.core.ui.textinputfield.StatefulInput.State.Error
import com.passbolt.mobile.android.feature.authentication.BindingScopedAuthenticatedFragment
import com.passbolt.mobile.android.feature.resourcedetails.ResourceActivity
import com.passbolt.mobile.android.feature.resourcedetails.ResourceMode
import com.passbolt.mobile.android.feature.resources.databinding.FragmentUpdateResourceBinding
import com.passbolt.mobile.android.ui.ResourceModel
import org.koin.android.ext.android.inject
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

@Suppress("TooManyFunctions")
class UpdateResourceFragment :
    BindingScopedAuthenticatedFragment<FragmentUpdateResourceBinding, UpdateResourceContract.View>(
        FragmentUpdateResourceBinding::inflate
    ), UpdateResourceContract.View {

    override val presenter: UpdateResourceContract.Presenter by inject()
    private val viewProvider: ViewProvider by inject()
    private val bundledExistingResource by lifecycleAwareLazy {
        requireActivity().intent?.getParcelableExtra<ResourceModel>(ResourceActivity.EXTRA_RESOURCE_MODEL)
    }
    private val bundledMode by lifecycleAwareLazy {
        requireActivity().intent?.getSerializableExtra(ResourceActivity.EXTRA_RESOURCE_MODE)
                as ResourceMode
    }
    private val bundledResourceParentFolderId by lifecycleAwareLazy {
        requireActivity().intent?.getStringExtra(ResourceActivity.EXTRA_RESOURCE_PARENT_FOLDER_ID)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setListeners()
        presenter.attach(this)
        presenter.argsRetrieved(bundledMode, bundledExistingResource, bundledResourceParentFolderId)
        super.onViewCreated(view, savedInstanceState)
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

    private fun setListeners() {
        with(binding) {
            updateButton.setDebouncingOnClick {
                presenter.updateClick()
            }
            initDefaultToolbar(toolbar)
            toolbar.setNavigationOnClickListener {
                requireActivity().finish()
            }
        }
    }

    override fun clearInputsContainer() {
        binding.container.removeAllViews()
    }

    override fun addTextInput(
        name: String,
        isSecret: Boolean,
        uiTag: String,
        isRequired: Boolean,
        initialValue: String?
    ) {
        val (textInputView, params) = viewProvider.getTextInput(name, requireContext(), isSecret)
        with(textInputView) {
            tag = uiTag
            text = initialValue.orEmpty()
            this.isRequired = isRequired
            setTextChangeListener { presenter.textChanged(tag as String, it) }
        }
        binding.container.addView(textInputView, params)
    }

    override fun showProgress() {
        showProgressDialog(childFragmentManager)
    }

    override fun hideProgress() {
        hideProgressDialog(childFragmentManager)
    }

    override fun addPasswordInput(
        name: String,
        uiTag: String,
        isRequired: Boolean,
        initialPassword: String?,
        initialPasswordStrength: PasswordGenerateInputView.PasswordStrength
    ) {
        val (view, params) = viewProvider.getPasswordWithGeneratorInput(requireContext())
        view.apply {
            tag = uiTag
            this.isRequired = isRequired
            showPassword(initialPassword.orEmpty(), initialPasswordStrength)
            setGenerateClickListener { presenter.passwordGenerateClick(uiTag) }
            setPasswordChangeListener { presenter.passwordTextChanged(uiTag, it) }
        }
        binding.container.addView(view, params)
    }

    override fun showPasswordStrength(tag: String, strength: PasswordGenerateInputView.PasswordStrength) {
        (binding.container.findViewWithTag<View>(tag) as PasswordGenerateInputView).setPasswordStrength(strength)
    }

    override fun addDescriptionInput(
        name: String,
        isSecret: Boolean,
        uiTag: String,
        isRequired: Boolean,
        initialValue: String?
    ) {
        val (view, params) = viewProvider.getDescriptionInput(requireContext(), isSecret)
        with(view) {
            tag = uiTag
            text = initialValue.orEmpty()
            this.isRequired = isRequired
            setTextChangeListener { presenter.textChanged(tag as String, it) }
        }
        binding.container.addView(view, params)
    }

    override fun showEmptyValueError(tag: String) {
        (binding.container.findViewWithTag<View>(tag) as StatefulInput)
            .setState(Error(String.format(resources.getString(LocalizationR.string.resource_update_empty_error), tag)))
    }

    override fun showTooLongError(tag: String) {
        (binding.container.findViewWithTag<View>(tag) as StatefulInput)
            .setState(Error(resources.getString(LocalizationR.string.resource_update_too_long_error)))
    }

    override fun showPassword(
        tag: String,
        password: String?,
        passwordStrength: PasswordGenerateInputView.PasswordStrength
    ) {
        (binding.container.findViewWithTag<View>(tag) as PasswordGenerateInputView).showPassword(
            password.orEmpty(),
            passwordStrength
        )
    }

    override fun closeWithCreateSuccessResult(name: String, id: String) {
        closeWithOperationSuccess(
            ActivityResults.RESULT_RESOURCE_CREATED,
            ResourceActivity.resourceNameAndIdIntent(name, id)
        )
    }

    override fun closeWithEditSuccessResult(name: String) {
        closeWithOperationSuccess(
            ActivityResults.RESULT_RESOURCE_EDITED,
            ResourceActivity.resourceNameResultIntent(name)
        )
    }

    private fun closeWithOperationSuccess(operation: Int, result: Intent) {
        with(requireActivity()) {
            setResult(operation, result)
            finish()
        }
    }

    override fun showError() {
        showSnackbar(
            LocalizationR.string.common_failure,
            backgroundColor = CoreUiR.color.red
        )
    }

    override fun showEncryptionError(message: String) {
        encryptionErrorAlertDialog(requireContext(), message)
            .show()
    }

    override fun showCreateButton() {
        binding.updateButton.text = getString(LocalizationR.string.resource_update_create_button)
    }

    override fun showEditButton() {
        binding.updateButton.text = getString(LocalizationR.string.save)
    }

    override fun showCreateTitle() {
        binding.toolbar.toolbarTitle = getString(LocalizationR.string.resource_update_password_title)
    }

    override fun showEditTitle() {
        binding.toolbar.toolbarTitle = getString(LocalizationR.string.resource_update_edit_password_title)
    }

    override fun showShareSimulationFailure() {
        showSnackbar(
            LocalizationR.string.resource_permissions_share_simulation_failed,
            backgroundColor = CoreUiR.color.red
        )
    }

    override fun showShareFailure() {
        showSnackbar(
            LocalizationR.string.resource_permissions_share_failed,
            backgroundColor = CoreUiR.color.red
        )
    }

    override fun showSecretFetchFailure() {
        showSnackbar(
            LocalizationR.string.common_fetch_failure,
            backgroundColor = CoreUiR.color.red
        )
    }

    override fun showSecretEncryptFailure() {
        showSnackbar(
            LocalizationR.string.common_encryption_failure,
            backgroundColor = CoreUiR.color.red
        )
    }

    override fun showSecretDecryptFailure() {
        showSnackbar(
            LocalizationR.string.common_decryption_failure,
            backgroundColor = CoreUiR.color.red
        )
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

    override fun hideRefreshProgress() {
        binding.fullScreenProgressLayout.gone()
    }

    override fun showRefreshProgress() {
        binding.fullScreenProgressLayout.visible()
    }

    override fun showDataRefreshError() {
        showSnackbar(
            LocalizationR.string.common_data_refresh_error,
            backgroundColor = CoreUiR.color.red
        )
    }

    override fun showContentNotAvailable() {
        Toast.makeText(requireContext(), LocalizationR.string.content_not_available, Toast.LENGTH_SHORT).show()
    }

    override fun navigateHome() {
        requireActivity().startActivity(ActivityIntents.bringHome(requireContext()))
    }

    override fun showInvalidSecretDataAndNavigateBack() {
        Toast.makeText(requireContext(), LocalizationR.string.resource_update_invalid_secret_data, Toast.LENGTH_SHORT)
            .show()
        requireActivity().finish()
    }
}
