package com.passbolt.mobile.android.feature.resourcedetails.details

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.os.Bundle
import android.os.PersistableBundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.doOnLayout
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import com.passbolt.mobile.android.common.ExternalDeeplinkHandler
import com.passbolt.mobile.android.common.dialogs.confirmResourceDeletionAlertDialog
import com.passbolt.mobile.android.core.extension.gone
import com.passbolt.mobile.android.core.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.extension.showSnackbar
import com.passbolt.mobile.android.core.extension.visible
import com.passbolt.mobile.android.core.navigation.deeplinks.NavDeepLinkProvider
import com.passbolt.mobile.android.core.ui.controller.TotpViewController
import com.passbolt.mobile.android.core.ui.controller.TotpViewController.StateParameters
import com.passbolt.mobile.android.core.ui.controller.TotpViewController.TimeParameters
import com.passbolt.mobile.android.core.ui.controller.TotpViewController.ViewParameters
import com.passbolt.mobile.android.core.ui.initialsicon.InitialsIconGenerator
import com.passbolt.mobile.android.core.ui.itemwithheaderandaction.ActionIcon
import com.passbolt.mobile.android.core.ui.progressdialog.hideProgressDialog
import com.passbolt.mobile.android.core.ui.progressdialog.showProgressDialog
import com.passbolt.mobile.android.core.ui.recyclerview.OverlappingItemDecorator
import com.passbolt.mobile.android.core.ui.recyclerview.OverlappingItemDecorator.Overlap
import com.passbolt.mobile.android.core.ui.span.RoundedBackgroundSpan
import com.passbolt.mobile.android.feature.authentication.BindingScopedAuthenticatedFragment
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormFragment
import com.passbolt.mobile.android.feature.resources.ResourcesDetailsDirections
import com.passbolt.mobile.android.feature.resources.databinding.FragmentResourceDetailsBinding
import com.passbolt.mobile.android.locationdetails.LocationItem
import com.passbolt.mobile.android.permissions.permissions.PermissionsFragment
import com.passbolt.mobile.android.permissions.permissions.PermissionsItem
import com.passbolt.mobile.android.permissions.permissions.PermissionsMode
import com.passbolt.mobile.android.permissions.recycler.CounterItem
import com.passbolt.mobile.android.permissions.recycler.GroupItem
import com.passbolt.mobile.android.permissions.recycler.UserItem
import com.passbolt.mobile.android.resourcemoremenu.ResourceMoreMenuFragment
import com.passbolt.mobile.android.ui.OtpItemWrapper
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.ResourceFormMode
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named
import java.time.ZonedDateTime
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
class ResourceDetailsFragment :
    BindingScopedAuthenticatedFragment<FragmentResourceDetailsBinding, ResourceDetailsContract.View>(
        FragmentResourceDetailsBinding::inflate
    ), ResourceDetailsContract.View, ResourceMoreMenuFragment.Listener {

    override val presenter: ResourceDetailsContract.Presenter by inject()
    private val clipboardManager: ClipboardManager? by inject()
    private val initialsIconGenerator: InitialsIconGenerator by inject()
    private val navArgs: ResourceDetailsFragmentArgs by navArgs()

    private val sharedWithFields
        get() = listOf(
            binding.sharedWithLabel,
            binding.sharedWithRecycler,
            binding.sharedWithRecyclerClickableArea,
            binding.sharedWithNavIcon
        )
    private val sharedWithDecorator: OverlappingItemDecorator by inject()

    private val totpFields
        get() = listOf(binding.totpSectionTitle, binding.totpContainer)

    private val tagsFields
        get() = listOf(binding.tagsHeader, binding.tagsValue, binding.tagsClickableArea, binding.tagsNavIcon)

    private val locationFields
        get() = listOf(binding.locationHeader, binding.locationValue, binding.locationNavIcon)

    private val externalDeeplinkHandler: ExternalDeeplinkHandler by inject()
    private val groupPermissionsItemAdapter: ItemAdapter<GroupItem> by inject(named(GROUP_ITEM_ADAPTER))
    private val userPermissionsItemAdapter: ItemAdapter<UserItem> by inject(named(USER_ITEM_ADAPTER))
    private val permissionsCounterItemAdapter: ItemAdapter<CounterItem> by inject(named(COUNTER_ITEM_ADAPTER))

    private val fastAdapter: FastAdapter<GenericItem> by inject()

    private val resourceEditResult = { _: String, result: Bundle ->
        if (result.containsKey(ResourceFormFragment.EXTRA_RESOURCE_EDITED)) {
            presenter.resourceEdited(result.getString(ResourceFormFragment.EXTRA_RESOURCE_NAME))
        }
    }

    private val resourceShareResult = { _: String, _: Bundle ->
        presenter.resourceShared()
    }

    private val totpViewController: TotpViewController by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.swipeRefresh.isEnabled = false
        setListeners()
        setUpPermissionsRecycler()
        presenter.attach(this)
        binding.sharedWithRecycler.doOnLayout {
            binding.sharedWithRecycler.addItemDecoration(sharedWithDecorator)
            presenter.argsReceived(
                navArgs.resourceModel,
                it.width,
                resources.getDimension(CoreUiR.dimen.dp_40)
            )
        }
        binding.secureNoteItem.conceal()
    }

    override fun onResume() {
        super.onResume()
        // has to be invoked using post to make sure binding.sharedWithRecycler.doOnLayout has finished
        binding.sharedWithRecycler.post {
            presenter.resume(this)
        }
    }

    override fun onPause() {
        presenter.pause()
        super.onPause()
    }

    override fun onStop() {
        presenter.viewStopped()
        super.onStop()
    }

    override fun onDestroyView() {
        binding.sharedWithRecycler.adapter = null
        presenter.detach()
        super.onDestroyView()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setListeners() {
        with(binding) {
            with(usernameItem) {
                val usernameCopyAction = presenter::usernameCopyClick
                setDebouncingOnClick(action = usernameCopyAction)
                actionClickListener = usernameCopyAction
            }
            with(passwordItem) {
                setDebouncingOnClick { presenter.copyPasswordClick() }
                actionClickListener = { presenter.passwordActionClick() }
            }
            with(urlItem) {
                val urlCopyAction = presenter::urlCopyClick
                setDebouncingOnClick(action = urlCopyAction)
                actionClickListener = urlCopyAction
            }
            with(metadataDescriptionItem) {
                setDebouncingOnClick { presenter.copyMetadataDescriptionClick() }
                actionClickListener = { presenter.metadataDescriptionActionClick() }
            }
            with(secureNoteItem) {
                setDebouncingOnClick { presenter.copySecureNoteClick() }
                actionClickListener = { presenter.secureNoteActionClick() }
            }
            backArrow.setDebouncingOnClick { presenter.backArrowClick() }
            moreIcon.setDebouncingOnClick { presenter.moreClick() }
            sharedWithFields.forEach { it.setDebouncingOnClick { presenter.sharedWithClick() } }
            tagsFields.forEach { it.setDebouncingOnClick { presenter.tagsClick() } }
            locationFields.forEach { it.setDebouncingOnClick { presenter.locationClick() } }
            totpIcon.setDebouncingOnClick { presenter.totpIconClick() }
            totpFields.forEach { it.setDebouncingOnClick { presenter.copyTotpClick() } }
            fastAdapter.onClickListener = { _, _, _, _ ->
                presenter.sharedWithClick()
                true
            }
        }
    }

    private fun setUpPermissionsRecycler() {
        binding.sharedWithRecycler.apply {
            layoutManager = object : LinearLayoutManager(context, HORIZONTAL, false) {
                override fun canScrollHorizontally() = false
            }
            adapter = fastAdapter
        }
    }

    override fun displayTitle(title: String) {
        binding.name.text = title
    }

    override fun displayExpiryTitle(name: String) {
        binding.name.text = getString(LocalizationR.string.name_expired, name)
    }

    override fun showExpiryIndicator() {
        binding.indicatorIcon.setImageResource(CoreUiR.drawable.ic_excl_indicator)
    }

    override fun displayExpirySection(expiry: ZonedDateTime) {
        with(binding) {
            expiryItem.visible()
            expiryItem.textValue = DateUtils.getRelativeTimeSpanString(
                expiry.toInstant().toEpochMilli(),
                ZonedDateTime.now().toInstant().toEpochMilli(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
            ).toString()
        }
    }

    override fun hideExpirySection() {
        binding.expiryItem.gone()
    }

    override fun displayUsername(username: String) {
        binding.usernameItem.textValue = username
    }

    override fun showTotpSection() {
        totpFields.forEach { it.visible() }
    }

    override fun hideTotpSection() {
        totpFields.forEach { it.gone() }
    }

    override fun hidePasswordSection() {
        with(binding) {
            passwordSectionTitle.gone()
            passwordContainer.gone()
        }
    }

    override fun showPasswordSection() {
        with(binding) {
            passwordSectionTitle.visible()
            passwordContainer.visible()
        }
    }

    override fun navigateBack() {
        findNavController().popBackStack()
    }

    override fun navigateToMore(resourceId: String, resourceName: String) {
        presenter.pause()
        ResourceMoreMenuFragment.newInstance(resourceId, resourceName)
            .show(childFragmentManager, ResourceMoreMenuFragment::class.java.name)
    }

    override fun displayUrl(url: String) {
        with(binding) {
            urlItem.textValue = url
        }
    }

    override fun displayInitialsIcon(name: String, initials: String) {
        binding.icon.setImageDrawable(
            initialsIconGenerator.generate(name, initials)
        )
    }

    override fun addToClipboard(label: String, value: String, isSecret: Boolean) {
        clipboardManager?.setPrimaryClip(
            ClipData.newPlainText(label, value).apply {
                description.extras = PersistableBundle().apply {
                    putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, isSecret)
                }
            }
        )
        Toast.makeText(requireContext(), getString(LocalizationR.string.copied_info, label), Toast.LENGTH_SHORT).show()
    }

    override fun showProgress() {
        showProgressDialog(childFragmentManager)
    }

    override fun hideProgress() {
        hideProgressDialog(childFragmentManager)
    }

    override fun showDecryptionFailure() {
        Toast.makeText(requireContext(), LocalizationR.string.common_decryption_failure, Toast.LENGTH_SHORT)
            .show()
    }

    override fun showFetchFailure() {
        Toast.makeText(requireContext(), LocalizationR.string.common_fetch_failure, Toast.LENGTH_SHORT)
            .show()
    }

    override fun showPassword(decryptedSecret: String) {
        with(binding.passwordItem) {
            actionIcon = ActionIcon.HIDE
            textValue = decryptedSecret
        }
    }

    override fun hidePassword() {
        with(binding.passwordItem) {
            actionIcon = ActionIcon.VIEW
            textValue = getString(LocalizationR.string.hidden_secret)
        }
    }

    override fun clearPasswordInput() {
        binding.passwordItem.textValue = ""
    }

    override fun clearSecureNoteInput() {
        binding.secureNoteItem.textValue = ""
    }

    override fun showSecureNote(secureNote: String) {
        with(binding) {
            secureNoteItem.show()
            secureNoteItem.isValueSecret = true
            secureNoteItem.textValue = secureNote
            secureNoteItem.actionIcon = ActionIcon.HIDE
            secureNoteItem.setTextIsSelectable(true)
            secureNoteSectionTitle.visible()
            secureNoteContainer.visible()
        }
    }

    override fun hideSecureNote() {
        with(binding.secureNoteItem) {
            conceal()
            actionIcon = ActionIcon.VIEW
        }
    }

    override fun disableSecureNote() {
        with(binding) {
            secureNoteSectionTitle.gone()
            secureNoteContainer.gone()
        }
    }

    override fun showMetadataDescription(description: String) {
        with(binding.metadataDescriptionItem) {
            visible()
            textValue = description
            setTextIsSelectable(true)
        }
    }

    override fun disableMetadataDescription() {
        binding.metadataDescriptionItem.gone()
    }

    override fun menuCopyPasswordClick() {
        presenter.copyPasswordClick()
    }

    override fun menuCopyMetadataDescriptionClick() {
        presenter.copyMetadataDescriptionClick()
    }

    override fun menuCopySecureNoteClick() {
        presenter.copySecureNoteClick()
    }

    override fun menuCopyUrlClick() {
        presenter.urlCopyClick()
    }

    override fun menuCopyUsernameClick() {
        presenter.usernameCopyClick()
    }

    override fun menuLaunchWebsiteClick() {
        presenter.launchWebsiteClick()
    }

    override fun menuDeleteClick() {
        presenter.deleteClick()
    }

    override fun menuEditClick() {
        presenter.editClick()
    }

    override fun menuFavouriteClick(option: ResourceMoreMenuModel.FavouriteOption) {
        presenter.favouriteClick(option)
    }

    override fun showToggleFavouriteFailure() {
        showSnackbar(
            LocalizationR.string.favourites_failure,
            backgroundColor = CoreUiR.color.red
        )
    }

    override fun showTotpDeleted() {
        showSnackbar(LocalizationR.string.otp_deleted)
    }

    override fun showFavouriteStar() {
        binding.favouriteIcon.visible()
    }

    override fun hideFavouriteStar() {
        binding.favouriteIcon.gone()
    }

    override fun menuShareClick() {
        presenter.shareClick()
    }

    override fun openWebsite(url: String) {
        externalDeeplinkHandler.openWebsite(requireContext(), url)
    }

    override fun showPasswordEyeIcon() {
        binding.passwordItem.actionIcon = ActionIcon.VIEW
    }

    override fun hidePasswordEyeIcon() {
        binding.passwordItem.actionIcon = ActionIcon.NONE
    }

    override fun closeWithDeleteSuccessResult(name: String) {
        setFragmentResult(
            REQUEST_RESOURCE_DETAILS,
            bundleOf(
                EXTRA_RESOURCE_DELETED to true,
                EXTRA_RESOURCE_NAME to name
            )
        )
        findNavController().popBackStack()
    }

    override fun setResourceEditedResult(resourceName: String) {
        setFragmentResult(
            REQUEST_RESOURCE_DETAILS,
            bundleOf(
                EXTRA_RESOURCE_EDITED to true,
                EXTRA_RESOURCE_NAME to resourceName
            )
        )
    }

    override fun showGeneralError(errorMessage: String?) {
        showSnackbar(
            LocalizationR.string.common_failure_format,
            backgroundColor = CoreUiR.color.red,
            messageArgs = arrayOf(errorMessage.orEmpty())
        )
    }

    override fun showEncryptionError(message: String) {
        showSnackbar(
            LocalizationR.string.common_encryption_failure,
            backgroundColor = CoreUiR.color.red
        )
    }

    override fun showInvalidTotpScanned() {
        showSnackbar(
            LocalizationR.string.resource_details_invalid_totp_scanned,
            backgroundColor = CoreUiR.color.red
        )
    }

    override fun navigateToEditResource(resourceModel: ResourceModel) {
        setFragmentResultListener(ResourceFormFragment.REQUEST_RESOURCE_FORM, resourceEditResult)
        findNavController().navigate(
            ResourceDetailsFragmentDirections.actionResourceDetailsToResourceForm(
                ResourceFormMode.Edit(
                    resourceModel.resourceId,
                    resourceModel.metadataJsonModel.name
                )
            )
        )
    }

    override fun navigateToResourceTags(resourceId: String, mode: PermissionsMode) {
        findNavController().navigate(
            NavDeepLinkProvider.resourceTagsDeepLinkRequest(resourceId, mode.name)
        )
    }

    override fun showResourceEditedSnackbar(resourceName: String) {
        showSnackbar(
            messageResId = LocalizationR.string.common_message_resource_edited,
            messageArgs = arrayOf(resourceName),
            backgroundColor = CoreUiR.color.green
        )
    }

    override fun showDeleteConfirmationDialog() {
        confirmResourceDeletionAlertDialog(requireContext()) {
            presenter.deleteResourceConfirmed()
        }
            .show()
    }

    override fun showPermissions(
        groupPermissions: List<PermissionModelUi.GroupPermissionModel>,
        userPermissions: List<PermissionModelUi.UserPermissionModel>,
        counterValue: List<String>,
        overlapOffset: Int
    ) {
        sharedWithFields.forEach { it.visible() }
        sharedWithDecorator.overlap = Overlap(left = overlapOffset)
        FastAdapterDiffUtil.calculateDiff(groupPermissionsItemAdapter, groupPermissions.map { GroupItem(it) })
        FastAdapterDiffUtil.calculateDiff(userPermissionsItemAdapter, userPermissions.map { UserItem(it) })
        FastAdapterDiffUtil.calculateDiff(permissionsCounterItemAdapter, counterValue.map { CounterItem(it) })
        fastAdapter.notifyAdapterDataSetChanged()
    }

    override fun navigateToResourcePermissions(resourceId: String, mode: PermissionsMode) {
        setFragmentResultListener(
            PermissionsFragment.REQUEST_UPDATE_PERMISSIONS,
            resourceShareResult
        )

        findNavController().navigate(
            ResourcesDetailsDirections.actionResourceDetailsToResourcePermissions(
                resourceId,
                mode,
                PermissionsItem.RESOURCE
            )
        )
    }

    override fun showResourceSharedSnackbar() {
        showSnackbar(
            LocalizationR.string.common_message_resource_shared,
            backgroundColor = CoreUiR.color.green
        )
    }

    override fun showTags(tags: List<String>) {
        val builder = SpannableStringBuilder()
        tags.forEach {
            builder.append(it)
            builder.setSpan(
                RoundedBackgroundSpan(
                    ContextCompat.getColor(requireContext(), CoreUiR.color.divider),
                    ContextCompat.getColor(requireContext(), CoreUiR.color.text_primary)
                ),
                builder.length - it.length,
                builder.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        binding.tagsValue.text = builder
        tagsFields.forEach { it.visible() }
    }

    override fun showFolderLocation(locationPathSegments: List<String>) {
        locationFields.forEach { it.visible() }
        binding.locationValue.text = locationPathSegments.let {
            val mutable = it.toMutableList()
            mutable.add(0, getString(LocalizationR.string.folder_root))
            mutable.joinToString(
                separator = " %s ".format(getString(LocalizationR.string.folder_details_location_separator))
            )
        }
    }

    override fun navigateToResourceLocation(resourceId: String) {
        val request = NavDeepLinkProvider.locationDetailsDeepLinkRequest(
            locationDetailsItemName = LocationItem.RESOURCE.name,
            locationDetailsItemId = resourceId
        )
        findNavController().navigate(request)
    }

    override fun hideRefreshProgress() {
        binding.swipeRefresh.isRefreshing = false
    }

    override fun showRefreshProgress() {
        binding.swipeRefresh.isRefreshing = true
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

    override fun showTotp(otpWrapper: OtpItemWrapper?) {
        otpWrapper?.let { otpModel ->
            totpViewController.updateView(
                ViewParameters(binding.totpProgress, binding.totpValue, binding.generationInProgress),
                StateParameters(otpModel.isRefreshing, otpModel.isVisible, otpModel.otpValue),
                TimeParameters(otpModel.otpExpirySeconds, otpModel.remainingSecondsCounter)
            )

            binding.totpIcon.setImageResource(
                if (otpModel.isVisible) CoreUiR.drawable.ic_eye_invisible else CoreUiR.drawable.ic_eye_visible
            )
        }
    }

    override fun resourceMoreMenuDismissed() {
        presenter.resume(this)
    }

    companion object {
        const val REQUEST_RESOURCE_DETAILS = "RESOURCE_DETAILS"

        const val EXTRA_RESOURCE_EDITED = "RESOURCE_EDITED"
        const val EXTRA_RESOURCE_DELETED = "RESOURCE_DELETED"
        const val EXTRA_RESOURCE_NAME = "RESOURCE_NAME"
    }
}
