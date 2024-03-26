package com.passbolt.mobile.android.feature.home.screen.recycler

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import com.mikepenz.fastadapter.listeners.ClickEventHook
import com.passbolt.mobile.android.common.extension.isInFuture
import com.passbolt.mobile.android.core.extension.DebounceClickEventHook
import com.passbolt.mobile.android.core.extension.asBinding
import com.passbolt.mobile.android.core.ui.initialsicon.InitialsIconGenerator
import com.passbolt.mobile.android.feature.home.R
import com.passbolt.mobile.android.feature.home.databinding.ItemPasswordBinding
import com.passbolt.mobile.android.ui.ResourceItemWrapper
import com.passbolt.mobile.android.ui.ResourceModel
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
class PasswordItem(
    private val resourceWrapper: ResourceItemWrapper,
    private val initialsIconGenerator: InitialsIconGenerator,
    private val dotsVisible: Boolean = true
) : AbstractBindingItem<ItemPasswordBinding>() {

    override val type: Int
        get() = R.id.itemPassword

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemPasswordBinding {
        return ItemPasswordBinding.inflate(inflater, parent, false)
    }

    override fun bindView(binding: ItemPasswordBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)
        with(binding) {
            setupUsername(this)
            setupTitleAndExpiry(this)
            more.isVisible = dotsVisible
            loader.isVisible = resourceWrapper.loaderVisible
            itemPassword.isEnabled = resourceWrapper.clickable
            initialsIconGenerator.generate(
                resourceWrapper.resourceModel.name,
                resourceWrapper.resourceModel.initials
            ).apply {
                icon.setImageDrawable(this)
            }
        }
    }

    private fun setupTitleAndExpiry(binding: ItemPasswordBinding) {
        resourceWrapper.resourceModel.expiry.let { expiry ->
            if (expiry == null || expiry.isInFuture()) {
                binding.title.text = resourceWrapper.resourceModel.name
                binding.indicatorIcon.setImageDrawable(null)
            } else {
                binding.title.text = binding.root.context.getString(
                    LocalizationR.string.name_expired, resourceWrapper.resourceModel.name
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

    private fun setupUsername(binding: ItemPasswordBinding) = with(binding) {
        val fontFamily = ResourcesCompat.getFont(binding.root.context, CoreUiR.font.inter)

        if (resourceWrapper.resourceModel.username.isNullOrBlank()) {
            subtitle.typeface = Typeface.create(fontFamily, FONT_WEIGHT, true)
            subtitle.text = binding.root.context.getString(LocalizationR.string.no_username)
        } else {
            subtitle.typeface = Typeface.create(fontFamily, FONT_WEIGHT, false)
            subtitle.text = resourceWrapper.resourceModel.username
        }
    }

    class MoreClick(
        private val clickListener: (ResourceModel) -> Unit
    ) : DebounceClickEventHook<PasswordItem>() {
        override fun onBind(viewHolder: RecyclerView.ViewHolder): View? {
            return viewHolder.asBinding<ItemPasswordBinding> {
                it.more
            }
        }

        override fun onDebounceClick(
            v: View,
            position: Int,
            fastAdapter: FastAdapter<PasswordItem>,
            item: PasswordItem
        ) {
            clickListener.invoke(item.resourceWrapper.resourceModel)
        }
    }

    class ItemClick(
        private val clickListener: (ResourceModel) -> Unit
    ) : ClickEventHook<PasswordItem>() {
        override fun onBind(viewHolder: RecyclerView.ViewHolder): View? {
            return viewHolder.asBinding<ItemPasswordBinding> {
                it.itemPassword
            }
        }

        override fun onClick(
            v: View,
            position: Int,
            fastAdapter: FastAdapter<PasswordItem>,
            item: PasswordItem
        ) {
            clickListener.invoke(item.resourceWrapper.resourceModel)
        }
    }

    companion object {
        private const val FONT_WEIGHT = 400
    }
}
