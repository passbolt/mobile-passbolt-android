package com.passbolt.mobile.android.feature.home.screen

import com.passbolt.mobile.android.feature.home.screen.adapter.PasswordModel

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
class HomePresenter : HomeContract.Presenter {

    override var view: HomeContract.View? = null

    override fun attach(view: HomeContract.View) {
        super.attach(view)
        getPasswords()
    }

    private fun getPasswords() {
        val list = listOf(
            PasswordModel(
                "Adobe Photoshop",
                "mail",
                "https://www.mcicon.com/wp-content/uploads/2021/03/Cat-18.jpg",
                "AP"
            ),
            PasswordModel("Figma", "john.doe@email.com", null, "F"),
            PasswordModel("Facebook", "john.doe@email.com", null, "F"),
            PasswordModel("Facebook", "john.doe@email.com", null, "F"),
            PasswordModel(
                "Facebook",
                "john.doe@email.com",
                "https://image.flaticon.com/icons/png/512/616/616408.png",
                "F"
            ),
            PasswordModel("Instagram", "john.doe@email.com", null, "I"),
            PasswordModel("Tiktok", "john.doe@email.com", null, "T"),
            PasswordModel("Miquido", "john.doe@email.com", null, "M"),
            PasswordModel("Facebook", "john.doe@email.com", null, "AB"),
            PasswordModel("Facebook", "john.doe@email.com", null, "CD"),
            PasswordModel("Facebook", "john.doe@email.com", null, "GH"),
            PasswordModel(
                "Facebook",
                "john.doe@email.com",
                "https://cdn.icon-icons.com/icons2/1446/PNG/512/22252hamsterface_98824.png",
                "HH"
            ),
            PasswordModel("Facebook", "john.doe@email.com", null, "AA"),
            PasswordModel("Workspace", "john.doe@email.com", null, "BW")
        )
        view?.showPasswords(list)
    }

    override fun moreClick() {
        view?.navigateToMore()
    }

    override fun itemClick() {
        view?.navigateToDetails()
    }
}
