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

package com.passbolt.mobile.android.core.extension

import android.app.Activity
import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.snackbar.Snackbar
import com.passbolt.mobile.android.core.ui.R

// TODO remove after main activity migration
fun AppCompatActivity.findNavHostFragment(
    @IdRes id: Int,
) = (supportFragmentManager.findFragmentById(id) as NavHostFragment)

fun Activity.getRootView(): View = findViewById(android.R.id.content)

// TODO remove after main activity migration
fun Activity.showSnackbar(
    message: String,
    length: Int = Snackbar.LENGTH_SHORT,
    @ColorRes backgroundColor: Int = R.color.background_gray_dark,
    vararg messageArgs: String,
) {
    snackbar(message, length, backgroundColor, *messageArgs)
        .show()
}

// TODO remove after main activity migration
fun Activity.snackbar(
    message: String,
    length: Int = Snackbar.LENGTH_SHORT,
    @ColorRes backgroundColor: Int = R.color.background_gray_dark,
    vararg messageArgs: String,
) = Snackbar
    .make(getRootView(), message.format(messageArgs), length)
    .apply {
        view.setBackgroundColor(context.getColor(backgroundColor))
        anchorView?.let { setAnchorView(it) }
    }
