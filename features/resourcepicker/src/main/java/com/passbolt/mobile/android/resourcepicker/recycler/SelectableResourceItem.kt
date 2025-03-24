package com.passbolt.mobile.android.resourcepicker.recycler

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import com.passbolt.mobile.android.common.extension.isInFuture
import com.passbolt.mobile.android.core.ui.initialsicon.InitialsIconGenerator
import com.passbolt.mobile.android.feature.resourcepicker.R
import com.passbolt.mobile.android.feature.resourcepicker.databinding.ItemSelectableResourceBinding
import com.passbolt.mobile.android.ui.ResourcePickerListItem
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

class SelectableResourceItem(
    val resourcePickerListItem: ResourcePickerListItem,
    private val initialsIconGenerator: InitialsIconGenerator
) : AbstractBindingItem<ItemSelectableResourceBinding>() {

    override val type: Int
        get() = R.id.itemSelectableResource

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?) =
        ItemSelectableResourceBinding.inflate(inflater, parent, false)

    override fun bindView(binding: ItemSelectableResourceBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)
        with(binding) {
            selectionRadioButton.isChecked = resourcePickerListItem.isSelected
            root.alpha = if (resourcePickerListItem.isSelectable) ALPHA_ENABLED else ALPHA_DISABLED
            setupTitleAndExpiry(this)
            setupUsername(this)
            setupInitialsIcon(this)
            setupSelection(this)
        }
    }

    private fun setupTitleAndExpiry(binding: ItemSelectableResourceBinding) {
        resourcePickerListItem.resourceModel.expiry.let { expiry ->
            if (expiry == null || expiry.isInFuture()) {
                binding.title.text = resourcePickerListItem.resourceModel.metadataJsonModel.name
                binding.indicatorIcon.setImageDrawable(null)
            } else {
                binding.title.text = binding.root.context.getString(
                    LocalizationR.string.name_expired, resourcePickerListItem.resourceModel.metadataJsonModel.name
                )
                binding.indicatorIcon.setImageDrawable(
                    ContextCompat.getDrawable(
                        binding.root.context,
                        CoreUiR.drawable.ic_excl_indicator
                    )
                )
            }
        }
    }

    private fun setupSelection(binding: ItemSelectableResourceBinding) {
        with(binding) {
            lock.isVisible = !resourcePickerListItem.isSelectable
            selectionRadioButton.isVisible = resourcePickerListItem.isSelectable
            selectionRadioButton.isChecked = resourcePickerListItem.isSelected
        }
    }

    private fun setupInitialsIcon(binding: ItemSelectableResourceBinding) {
        initialsIconGenerator.generate(
            resourcePickerListItem.resourceModel.metadataJsonModel.name,
            resourcePickerListItem.resourceModel.initials
        ).apply {
            binding.icon.setImageDrawable(this)
        }
    }

    private fun setupUsername(binding: ItemSelectableResourceBinding) = with(binding) {
        val fontFamily = ResourcesCompat.getFont(binding.root.context, CoreUiR.font.inter)

        if (resourcePickerListItem.resourceModel.metadataJsonModel.username.isNullOrBlank()) {
            subtitle.typeface = Typeface.create(fontFamily, FONT_WEIGHT, true)
            subtitle.text = binding.root.context.getString(LocalizationR.string.no_username)
        } else {
            subtitle.typeface = Typeface.create(fontFamily, FONT_WEIGHT, false)
            subtitle.text = resourcePickerListItem.resourceModel.metadataJsonModel.username
        }
    }

    companion object {
        private const val FONT_WEIGHT = 400
        private const val ALPHA_ENABLED = 1f
        private const val ALPHA_DISABLED = 0.5f
    }
}
