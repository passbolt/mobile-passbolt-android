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

package com.passbolt.mobile.android.resourcepicker

import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.passbolt.mobile.android.resourcepicker.recycler.HeaderItem
import com.passbolt.mobile.android.resourcepicker.recycler.SelectableResourceItem
import org.koin.core.module.Module
import org.koin.core.module.dsl.scopedOf
import org.koin.core.qualifier.named
import org.koin.dsl.ScopeDSL
import org.koin.dsl.bind

internal const val SUGGESTED_HEADER_ITEM_ADAPTER = "SUGGESTED_HEADER_ITEM_ADAPTER"
internal const val SUGGESTED_ITEMS_ITEM_ADAPTER = "SUGGESTED_ITEMS_ITEM_ADAPTER"
internal const val OTHER_ITEMS_HEADER_ITEM_ADAPTER = "OTHER_ITEMS_HEADER_ITEM_ADAPTER"
internal const val RESOURCE_ITEM_ADAPTER = "RESOURCE_ITEM_ADAPTER"

fun Module.resourcePickerScreenModule() {
    scope<ResourcePickerFragment> {
        scopedOf(::ResourcePickerPresenter) bind ResourcePickerContract.Presenter::class
        declareResourcePickerRecyclerDependencies()
    }
}

private fun ScopeDSL.declareResourcePickerRecyclerDependencies() {
    scoped<ItemAdapter<SelectableResourceItem>>(named(RESOURCE_ITEM_ADAPTER)) {
        ItemAdapter.items()
    }
    scoped<ItemAdapter<SelectableResourceItem>>(named(SUGGESTED_ITEMS_ITEM_ADAPTER)) {
        ItemAdapter.items()
    }
    scoped<ItemAdapter<HeaderItem>>(named(SUGGESTED_HEADER_ITEM_ADAPTER)) {
        ItemAdapter.items()
    }
    scoped<ItemAdapter<HeaderItem>>(named(OTHER_ITEMS_HEADER_ITEM_ADAPTER)) {
        ItemAdapter.items()
    }
    scoped {
        FastAdapter.with(
            listOf(
                get<ItemAdapter<HeaderItem>>(named(SUGGESTED_HEADER_ITEM_ADAPTER)),
                get<ItemAdapter<SelectableResourceItem>>(named(SUGGESTED_ITEMS_ITEM_ADAPTER)),
                get<ItemAdapter<HeaderItem>>(named(OTHER_ITEMS_HEADER_ITEM_ADAPTER)),
                get<ItemAdapter<SelectableResourceItem>>(named(RESOURCE_ITEM_ADAPTER))
            )
        )
    }
}
