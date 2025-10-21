package com.passbolt.mobile.android.ui

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

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

@Serializable
sealed class HomeDisplayViewModel : Parcelable {
    @Serializable
    @Parcelize
    object AllItems : HomeDisplayViewModel(), Parcelable

    @Serializable
    @Parcelize
    object Favourites : HomeDisplayViewModel(), Parcelable

    @Serializable
    @Parcelize
    object RecentlyModified : HomeDisplayViewModel(), Parcelable

    @Serializable
    @Parcelize
    object SharedWithMe : HomeDisplayViewModel(), Parcelable

    @Serializable
    @Parcelize
    object OwnedByMe : HomeDisplayViewModel(), Parcelable

    @Serializable
    @Parcelize
    object Expiry : HomeDisplayViewModel(), Parcelable

    @Serializable
    @Parcelize
    object NotLoaded : HomeDisplayViewModel(), Parcelable

    @Serializable
    @Parcelize
    data class Folders(
        val activeFolder: Folder,
        val activeFolderName: String? = null,
        val isActiveFolderShared: Boolean? = null,
    ) : HomeDisplayViewModel(),
        Parcelable

    @Serializable
    @Parcelize
    data class Tags(
        val activeTagId: String? = null,
        val activeTagName: String? = null,
        val isActiveTagShared: Boolean? = null,
    ) : HomeDisplayViewModel(),
        Parcelable

    @Serializable
    @Parcelize
    data class Groups(
        val activeGroupId: String? = null,
        val activeGroupName: String? = null,
    ) : HomeDisplayViewModel(),
        Parcelable

    companion object {
        fun folderRoot() = Folders(Folder.Root)

        fun tagsRoot() = Tags()

        fun groupsRoot() = Groups()
    }
}
