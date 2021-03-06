package com.passbolt.mobile.android.feature.resources.details

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import com.amulyakhare.textdrawable.TextDrawable
import com.amulyakhare.textdrawable.util.ColorGenerator
import com.google.android.material.snackbar.Snackbar
import com.passbolt.mobile.android.common.WebsiteOpener
import com.passbolt.mobile.android.common.extension.gone
import com.passbolt.mobile.android.common.extension.setDebouncingOnClick
import com.passbolt.mobile.android.common.extension.visible
import com.passbolt.mobile.android.common.lifecycleawarelazy.lifecycleAwareLazy
import com.passbolt.mobile.android.core.commonresource.moremenu.ResourceMoreMenuFragment
import com.passbolt.mobile.android.core.ui.progressdialog.hideProgressDialog
import com.passbolt.mobile.android.core.ui.progressdialog.showProgressDialog
import com.passbolt.mobile.android.feature.authentication.BindingScopedAuthenticatedFragment
import com.passbolt.mobile.android.feature.resources.R
import com.passbolt.mobile.android.feature.resources.ResourceActivity
import com.passbolt.mobile.android.feature.resources.ResourceMode
import com.passbolt.mobile.android.feature.resources.databinding.FragmentResourceDetailsBinding
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel
import org.koin.android.ext.android.inject

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
class ResourceDetailsFragment :
    BindingScopedAuthenticatedFragment<FragmentResourceDetailsBinding, ResourceDetailsContract.View>(
        FragmentResourceDetailsBinding::inflate
    ), ResourceDetailsContract.View, ResourceMoreMenuFragment.Listener {

    override val presenter: ResourceDetailsContract.Presenter by inject()
    private val clipboardManager: ClipboardManager? by inject()
    private val bundledResourceModel: ResourceModel by lifecycleAwareLazy {
        requireNotNull(
            requireActivity().intent.getParcelableExtra(ResourceActivity.EXTRA_RESOURCE_MODEL)
        )
    }
    private val regularFont by lifecycleAwareLazy {
        ResourcesCompat.getFont(requireContext(), R.font.inter)
    }
    private val secretFont by lifecycleAwareLazy {
        ResourcesCompat.getFont(requireContext(), R.font.inconsolata)
    }
    private val usernameCopyFields
        get() = listOf(binding.usernameHeader, binding.usernameValue, binding.usernameIcon)

    private val passwordCopyField
        get() = listOf(binding.passwordHeader, binding.passwordValue)

    private val urlCopyFields
        get() = listOf(binding.urlHeader, binding.urlIcon)

    private val websiteOpener: WebsiteOpener by inject()

    private val resourceDetailsResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == ResourceActivity.RESULT_RESOURCE_EDITED) {
                val name = it.data?.getStringExtra(ResourceActivity.EXTRA_RESOURCE_NAME)
                presenter.resourceEdited(name.orEmpty())
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setListeners()
        presenter.attach(this)
        presenter.argsReceived(bundledResourceModel.resourceId)
    }

    override fun onStop() {
        presenter.viewStopped()
        super.onStop()
    }

    override fun onDestroyView() {
        presenter.detach()
        super.onDestroyView()
    }

    private fun setListeners() {
        with(binding) {
            usernameCopyFields.forEach { it.setDebouncingOnClick { presenter.usernameCopyClick() } }
            passwordIcon.setDebouncingOnClick { presenter.secretIconClick() }
            passwordCopyField.forEach { it.setDebouncingOnClick { presenter.menuCopyPasswordClick() } }
            urlCopyFields.forEach { it.setDebouncingOnClick { presenter.urlCopyClick() } }
            backArrow.setDebouncingOnClick { presenter.backArrowClick() }
            moreIcon.setDebouncingOnClick { presenter.moreClick() }
            seeDescriptionButton.setDebouncingOnClick { presenter.seeDescriptionButtonClick() }
            descriptionHeader.setDebouncingOnClick { presenter.menuCopyDescriptionClick() }
        }
    }

    override fun displayTitle(title: String) {
        binding.name.text = title
    }

    override fun displayUsername(username: String) {
        binding.usernameValue.text = username
    }

    override fun navigateBack() {
        requireActivity().finish()
    }

    override fun navigateToMore(menuModel: ResourceMoreMenuModel) {
        ResourceMoreMenuFragment.newInstance(menuModel)
            .show(childFragmentManager, ResourceMoreMenuFragment::class.java.name)
    }

    override fun displayUrl(url: String) {
        with(binding) {
            urlValue.text = url
            urlValue.visible()
            urlHeader.visible()
            urlIcon.visible()
        }
    }

    override fun displayInitialsIcon(name: String, initials: String) {
        val generator = ColorGenerator.MATERIAL
        val generatedColor = generator.getColor(name)
        val color = ColorUtils.blendARGB(generatedColor, Color.WHITE, LIGHT_RATIO)
        binding.icon.setImageDrawable(
            TextDrawable.builder()
                .beginConfig()
                .textColor(ColorUtils.blendARGB(color, generatedColor, DARK_RATIO))
                .useFont(ResourcesCompat.getFont(requireContext(), R.font.inter_medium))
                .endConfig()
                .buildRoundRect(initials, color, ICON_RADIUS)
        )
    }

    override fun addToClipboard(label: String, value: String) {
        clipboardManager?.setPrimaryClip(
            ClipData.newPlainText(label, value)
        )
        Toast.makeText(requireContext(), getString(R.string.copied_info, label), Toast.LENGTH_SHORT).show()
    }

    override fun showProgress() {
        showProgressDialog(childFragmentManager)
    }

    override fun hideProgress() {
        hideProgressDialog(childFragmentManager)
    }

    override fun showPasswordVisibleIcon() {
        binding.passwordIcon.setImageResource(R.drawable.ic_eye_invisible)
    }

    override fun showPasswordHiddenIcon() {
        binding.passwordIcon.setImageResource(R.drawable.ic_eye_visible)
    }

    override fun showDecryptionFailure() {
        Toast.makeText(requireContext(), R.string.resource_details_decryption_failure, Toast.LENGTH_SHORT)
            .show()
    }

    override fun showFetchFailure() {
        Toast.makeText(requireContext(), R.string.resource_details_fetch_failure, Toast.LENGTH_SHORT)
            .show()
    }

    override fun showPasswordHidden() {
        binding.passwordValue.text = getString(R.string.resource_details_hide_password)
    }

    override fun showPassword(decryptedSecret: String) {
        binding.passwordValue.text = decryptedSecret
    }

    override fun clearPasswordInput() {
        binding.passwordValue.text = ""
    }

    override fun showDescription(description: String, useSecretFont: Boolean) {
        with(binding) {
            descriptionValue.apply {
                typeface = if (useSecretFont) secretFont else regularFont
                setTextIsSelectable(true)
                text = description
            }
            seeDescriptionButton.gone()
        }
    }

    override fun showDescriptionIsEncrypted() {
        with(binding) {
            descriptionValue.apply {
                typeface = regularFont
                setTextIsSelectable(false)
                text = getString(R.string.resource_details_encrypted_description)
            }
            seeDescriptionButton.visible()
        }
    }

    override fun menuCopyPasswordClick() {
        presenter.menuCopyPasswordClick()
    }

    override fun menuCopyDescriptionClick() {
        presenter.menuCopyDescriptionClick()
    }

    override fun menuCopyUrlClick() {
        presenter.menuCopyUrlClick()
    }

    override fun menuCopyUsernameClick() {
        presenter.menuCopyUsernameClick()
    }

    override fun menuLaunchWebsiteClick() {
        presenter.menuLaunchWebsiteClick()
    }

    override fun menuDeleteClick() {
        presenter.menuDeleteClick()
    }

    override fun menuEditClick() {
        presenter.menuEditClick()
    }

    override fun openWebsite(url: String) {
        websiteOpener.open(requireContext(), url)
    }

    override fun hidePasswordEyeIcon() {
        binding.passwordIcon.gone()
    }

    override fun closeWithDeleteSuccessResult(name: String) {
        with(requireActivity()) {
            setResult(
                ResourceActivity.RESULT_RESOURCE_DELETED,
                ResourceActivity.resourceNameResultIntent(name)
            )
            finish()
        }
    }

    override fun showGeneralError() {
        Snackbar.make(requireView(), R.string.common_failure, Snackbar.LENGTH_SHORT)
            .show()
    }

    override fun navigateToEditResource(resourceModel: ResourceModel) {
        resourceDetailsResult.launch(
            ResourceActivity.newInstance(
                requireContext(),
                ResourceMode.EDIT,
                resourceModel.folderId,
                resourceModel
            )
        )
    }

    override fun showResourceEditedSnackbar(resourceName: String) {
        Snackbar.make(
            requireView(),
            getString(R.string.resource_details_resource_edited_format, resourceName),
            Snackbar.LENGTH_LONG
        )
            .show()
    }

    override fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.are_you_sure)
            .setMessage(R.string.resource_will_be_deleted)
            .setPositiveButton(R.string.delete) { _, _ -> presenter.deleteResourceConfirmed() }
            .setNegativeButton(R.string.cancel) { _, _ -> }
            .setCancelable(false)
            .show()
    }

    companion object {
        private const val LIGHT_RATIO = 0.5f
        private const val DARK_RATIO = 0.88f
        private const val ICON_RADIUS = 4
    }
}
