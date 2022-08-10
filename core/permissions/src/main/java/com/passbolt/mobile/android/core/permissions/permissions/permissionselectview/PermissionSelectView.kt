package com.passbolt.mobile.android.core.permissions.permissions.permissionselectview

import android.content.Context
import android.util.AttributeSet
import android.widget.RadioGroup
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.updatePadding
import com.google.android.material.radiobutton.MaterialRadioButton
import com.passbolt.mobile.android.permissions.R
import com.passbolt.mobile.android.ui.ResourcePermission

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
class PermissionSelectView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : RadioGroup(context, attrs) {

    var onPermissionSelectedListener: ((ResourcePermission) -> Unit)? = null

    private val buttons = hashMapOf<ResourcePermission, Int>()
    private var selectedPermission: ResourcePermission? = null

    init {
        createChoices(context)
        setChangeListener()
    }

    private fun createChoices(context: Context) {
        ResourcePermission.values()
            .forEach {
                addView(createRadioButton(context, it))
            }
    }

    private fun createRadioButton(
        context: Context,
        permission: ResourcePermission
    ) =
        MaterialRadioButton(context)
            .apply {
                id = generateViewId()
                text = ResourcePermission.getPermissionTextValue(context, permission)
                setCompoundDrawablesWithIntrinsicBounds(
                    ResourcePermission.getPermissionIcon(context, permission),
                    null,
                    ContextCompat.getDrawable(context, R.drawable.radio_button_selector),
                    null
                )
                layoutParams = LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT
                )
                updatePadding(right = resources.getDimension(R.dimen.dp_16).toInt())
                compoundDrawablePadding = context.resources.getDimension(R.dimen.dp_12).toInt()
                typeface = ResourcesCompat.getFont(context, R.font.inter_semi_bold)
                buttonDrawable = null
                setBackgroundColor(ContextCompat.getColor(context, R.color.background))
                buttons[permission] = id
            }

    private fun setChangeListener() {
        setOnCheckedChangeListener { _, buttonId ->
            val permission = buttons.keys.first { buttons[it] == buttonId }
            selectPermission(permission)
        }
    }

    fun selectPermission(resourcePermission: ResourcePermission, silently: Boolean = false) {
        val buttonId = requireNotNull(buttons[resourcePermission])
        findViewById<MaterialRadioButton>(buttonId).isChecked = true
        selectedPermission = resourcePermission
        if (!silently) {
            onPermissionSelectedListener?.invoke(resourcePermission)
        }
    }
}
