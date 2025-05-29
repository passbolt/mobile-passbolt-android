package com.passbolt.mobile.android.feature.home.screen.recycler

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import com.mikepenz.fastadapter.listeners.ClickEventHook
import com.passbolt.mobile.android.core.extension.asBinding
import com.passbolt.mobile.android.feature.home.R
import com.passbolt.mobile.android.feature.home.databinding.ItemTagWithCountBinding
import com.passbolt.mobile.android.ui.TagWithCount
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
class TagWithCountItem(
    val tagWithCount: TagWithCount,
) : AbstractBindingItem<ItemTagWithCountBinding>() {
    override val type: Int
        get() = R.id.itemTagWithCount

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
    ): ItemTagWithCountBinding = ItemTagWithCountBinding.inflate(inflater, parent, false)

    override fun bindView(
        binding: ItemTagWithCountBinding,
        payloads: List<Any>,
    ) {
        super.bindView(binding, payloads)
        with(binding) {
            name.text = tagWithCount.slug
            taggedItemsCount.text = tagWithCount.taggedResourcesCount.toString()
            icon.setImageResource(
                if (tagWithCount.isShared) {
                    CoreUiR.drawable.ic_filled_shared_tag_with_bg
                } else {
                    CoreUiR.drawable.ic_filled_tag_with_bg
                },
            )
        }
    }

    class ItemClick(
        private val clickListener: (TagWithCount) -> Unit,
    ) : ClickEventHook<TagWithCountItem>() {
        override fun onBind(viewHolder: RecyclerView.ViewHolder): View? =
            viewHolder.asBinding<ItemTagWithCountBinding> {
                it.itemTagWithCount
            }

        override fun onClick(
            v: View,
            position: Int,
            fastAdapter: FastAdapter<TagWithCountItem>,
            item: TagWithCountItem,
        ) {
            clickListener.invoke(item.tagWithCount)
        }
    }
}
