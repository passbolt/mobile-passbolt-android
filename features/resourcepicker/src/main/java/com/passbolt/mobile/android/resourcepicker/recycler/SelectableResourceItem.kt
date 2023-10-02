package com.passbolt.mobile.android.resourcepicker.recycler

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import coil.load
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import com.passbolt.mobile.android.core.ui.initialsicon.InitialsIconGenerator
import com.passbolt.mobile.android.feature.resourcepicker.R
import com.passbolt.mobile.android.feature.resourcepicker.databinding.ItemSelectableResourceBinding
import com.passbolt.mobile.android.ui.SelectableResourceModelWrapper
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

class SelectableResourceItem(
    val selectableResourceModel: SelectableResourceModelWrapper,
    private val initialsIconGenerator: InitialsIconGenerator
) : AbstractBindingItem<ItemSelectableResourceBinding>() {

    override val type: Int
        get() = R.id.itemSelectableResource

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?) =
        ItemSelectableResourceBinding.inflate(inflater, parent, false)

    override fun bindView(binding: ItemSelectableResourceBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)
        with(binding) {
            title.text = selectableResourceModel.resourceModel.name
            itemSelectableResource.isEnabled = selectableResourceModel.isSelectable
            selectionRadioButton.isChecked = selectableResourceModel.isSelected
            root.alpha = if (selectableResourceModel.isSelectable) ALPHA_ENABLED else ALPHA_DISABLED
            setupUsername(this)
            setupInitialsIcon(this)
            setupSelection(this)
        }
    }

    private fun setupSelection(binding: ItemSelectableResourceBinding) {
        with(binding) {
            lock.isVisible = !selectableResourceModel.isSelectable
            selectionRadioButton.isVisible = selectableResourceModel.isSelectable
            selectionRadioButton.isChecked = selectableResourceModel.isSelected
        }
    }

    private fun setupInitialsIcon(binding: ItemSelectableResourceBinding) {
        val initialsIcons = initialsIconGenerator.generate(
            selectableResourceModel.resourceModel.name,
            selectableResourceModel.resourceModel.initials
        )
        with(binding) {
            icon.setImageDrawable(initialsIcons)
            selectableResourceModel.resourceModel.icon?.let {
                icon.load(it) {
                    placeholder(initialsIcons)
                }
            }
        }
    }

    private fun setupUsername(binding: ItemSelectableResourceBinding) = with(binding) {
        val fontFamily = ResourcesCompat.getFont(binding.root.context, CoreUiR.font.inter)

        if (selectableResourceModel.resourceModel.username.isNullOrBlank()) {
            subtitle.typeface = Typeface.create(fontFamily, FONT_WEIGHT, true)
            subtitle.text = binding.root.context.getString(LocalizationR.string.no_username)
        } else {
            subtitle.typeface = Typeface.create(fontFamily, FONT_WEIGHT, false)
            subtitle.text = selectableResourceModel.resourceModel.username
        }
    }

    companion object {
        private const val FONT_WEIGHT = 400
        private const val ALPHA_ENABLED = 1f
        private const val ALPHA_DISABLED = 0.5f
    }
}
