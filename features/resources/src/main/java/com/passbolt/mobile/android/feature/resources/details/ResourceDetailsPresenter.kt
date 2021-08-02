package com.passbolt.mobile.android.feature.resources.details

import com.passbolt.mobile.android.feature.resources.details.more.ResourceDetailsMoreModel
import com.passbolt.mobile.android.ui.PasswordModel

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
class ResourceDetailsPresenter : ResourceDetailsContract.Presenter {

    override var view: ResourceDetailsContract.View? = null
    private lateinit var passwordModel: PasswordModel

    override fun argsReceived(passwordModel: PasswordModel) {
        this.passwordModel = passwordModel
        view?.displayTitle(passwordModel.name)
        view?.displayUsername(passwordModel.username)
        view?.displayInitialsIcon(passwordModel.name, passwordModel.initials)
        if (passwordModel.url.isNotEmpty()) {
            view?.displayUrl(passwordModel.url)
        }
    }

    override fun usernameCopyClick() {
        view?.addToClipboard(USERNAME_LABEL, passwordModel.username)
    }

    override fun urlCopyClick() {
        view?.addToClipboard(WEBSITE_LABEL, passwordModel.url)
    }

    override fun moreClick() {
        view?.navigateToMore(ResourceDetailsMoreModel(passwordModel.name))
    }

    override fun backArrowClick() {
        view?.navigateBack()
    }

    companion object {
        private const val WEBSITE_LABEL = "Website"
        private const val USERNAME_LABEL = "Username"
    }
}
