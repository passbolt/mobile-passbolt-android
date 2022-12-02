package com.passbolt.mobile.android.core.ui.menu

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.switchmaterial.SwitchMaterial

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
class SwitchSettingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : SettingView(context, attrs, defStyle) {

    var onChanged: ((Boolean) -> Unit)? = null

    private val switch = SwitchMaterial(context).apply {
        tag = name
    }
    private var silentCheckChangedModeOn = false

    init {
        binding.root.addView(switch)
        switch.setOnCheckedChangeListener { _, isChecked ->
            if (!silentCheckChangedModeOn) {
                onChanged?.invoke(isChecked)
            }
        }
    }

    override fun onDetachedFromWindow() {
        switch.setOnCheckedChangeListener(null)
        super.onDetachedFromWindow()
    }

    fun turnOn(silently: Boolean) {
        executeCheckChange(silently, checkChangeValue = true)
    }

    fun turnOff(silently: Boolean) {
        executeCheckChange(silently, checkChangeValue = false)
    }

    private fun executeCheckChange(silently: Boolean, checkChangeValue: Boolean) {
        silentCheckChangedModeOn = silently
        switch.isChecked = checkChangeValue
        silentCheckChangedModeOn = false
    }
}
