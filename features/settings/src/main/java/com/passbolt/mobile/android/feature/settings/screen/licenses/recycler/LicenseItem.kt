package com.passbolt.mobile.android.feature.settings.screen.licenses.recycler

import android.view.LayoutInflater
import android.view.ViewGroup
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import com.passbolt.mobile.android.feature.settings.R
import com.passbolt.mobile.android.feature.settings.databinding.ItemLicenseBinding
import com.passbolt.mobile.android.ui.LicenseModelItem

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
class LicenseItem(
    private val resourceModel: LicenseModelItem
) : AbstractBindingItem<ItemLicenseBinding>() {

    override val type: Int
        get() = R.id.itemLicense

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemLicenseBinding {
        return ItemLicenseBinding.inflate(inflater, parent, false)
    }

    override fun bindView(binding: ItemLicenseBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)
        with(binding) {
            artifactLabel.text =
                "%s:%s:%s".format(
                    resourceModel.groupId.orEmpty(),
                    resourceModel.artifactId.orEmpty(),
                    resourceModel.version.orEmpty()
                )
            nameLabel.text = resourceModel.name.orEmpty()
            licenseIdLabel.text = resourceModel.spdxLicenses
                ?.mapNotNull { it.name }
                ?.joinToString(separator = System.lineSeparator()) { "$BULLET $it" }
            urlsLabel.text = resourceModel.spdxLicenses
                ?.map { it.url }
                ?.plus(resourceModel.scm?.url)
                ?.joinToString(separator = System.lineSeparator()) { "$BULLET $it" }
        }
    }

    private companion object {
        private const val BULLET = "\u2022"
    }
}
