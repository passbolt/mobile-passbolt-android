package com.passbolt.mobile.android.feature.resources.details

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import androidx.navigation.fragment.findNavController
import com.amulyakhare.textdrawable.TextDrawable
import com.amulyakhare.textdrawable.util.ColorGenerator
import com.google.android.material.snackbar.Snackbar
import com.passbolt.mobile.android.common.extension.setDebouncingOnClick
import com.passbolt.mobile.android.common.extension.visible
import com.passbolt.mobile.android.common.lifecycleawarelazy.lifecycleAwareLazy
import com.passbolt.mobile.android.core.mvp.authentication.BindingScopedAuthenticatedFragment
import com.passbolt.mobile.android.core.ui.progressdialog.ProgressDialog
import com.passbolt.mobile.android.feature.resources.R
import com.passbolt.mobile.android.feature.resources.ResourcesActivity
import com.passbolt.mobile.android.feature.resources.databinding.FragmentResourceDetailsBinding
import com.passbolt.mobile.android.feature.resources.details.more.ResourceDetailsMenuModel
import com.passbolt.mobile.android.ui.ResourceModel
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
    ), ResourceDetailsContract.View {

    override val presenter: ResourceDetailsContract.Presenter by inject()
    private val clipboardManager: ClipboardManager? by inject()
    private val bundledResourceModel: ResourceModel by lifecycleAwareLazy {
        requireNotNull(
            requireActivity().intent.getParcelableExtra(ResourcesActivity.RESOURCE_MODEL_KEY)
        )
    }
    private var progressDialog: ProgressDialog? = ProgressDialog()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setListeners()
        presenter.attach(this)
        presenter.argsReceived(bundledResourceModel)
    }

    override fun onStop() {
        presenter.viewStopped()
        super.onStop()
    }

    override fun onDestroyView() {
        progressDialog = null
        presenter.detach()
        super.onDestroyView()
    }

    private fun setListeners() {
        with(binding) {
            usernameIcon.setDebouncingOnClick { presenter.usernameCopyClick() }
            urlIcon.setDebouncingOnClick { presenter.urlCopyClick() }
            backArrow.setDebouncingOnClick { presenter.backArrowClick() }
            moreIcon.setDebouncingOnClick { presenter.moreClick() }
            passwordIcon.setDebouncingOnClick { presenter.secretIconClick() }
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

    override fun navigateToMore(model: ResourceDetailsMenuModel) {
        findNavController().navigate(ResourceDetailsFragmentDirections.actionResourceDetailsToMore(model))
    }

    override fun displayUrl(url: String) {
        with(binding) {
            urlValue.text = url
            urlValue.visible()
            urlHeader.visible()
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
        progressDialog?.show(childFragmentManager, ProgressDialog::class.java.name)
    }

    override fun hideProgress() {
        progressDialog?.dismiss()
    }

    override fun showPasswordVisibleIcon() {
        binding.passwordIcon.setImageResource(R.drawable.ic_eye_invisible)
    }

    override fun showPasswordHiddenIcon() {
        binding.passwordIcon.setImageResource(R.drawable.ic_eye_visible)
    }

    override fun showDecryptionFailure() {
        Snackbar.make(requireView(), R.string.resource_details_decryption_failure, Snackbar.LENGTH_LONG)
            .show()
    }

    override fun showFetchFailure() {
        Snackbar.make(requireView(), R.string.resource_details_fetch_failure, Snackbar.LENGTH_LONG)
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

    companion object {
        private const val LIGHT_RATIO = 0.5f
        private const val DARK_RATIO = 0.88f
        private const val ICON_RADIUS = 4
    }
}
