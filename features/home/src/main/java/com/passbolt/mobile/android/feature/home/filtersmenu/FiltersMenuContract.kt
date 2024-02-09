package com.passbolt.mobile.android.feature.home.filtersmenu

import com.passbolt.mobile.android.core.mvp.BaseContract
import com.passbolt.mobile.android.ui.FiltersMenuModel

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

/**
 * MVP contract for the home screen filters menu.
 */
interface FiltersMenuContract {

    /**
     * Home menu filters view
     */
    interface View : BaseContract.View {
        /**
         * Unselects all items in the menu
         */
        fun unselectAll()

        /**
         * Selects all items item in the menu
         */
        fun selectAllItemsItem()

        /**
         * Selects favourites item in the menu
         */
        fun selectFavouritesItem()

        /**
         * Selects recently modified item in the menu
         */
        fun selectRecentlyModifiedItem()

        /**
         * Selects shared with me item in the menu
         */
        fun selectSharedWithMeItem()

        /**
         * Selects owned by me item in the menu
         */
        fun selectOwnedByMeItem()

        /**
         * Selects expiry item in the menu
         */
        fun selectExpiryMenuItem()

        /**
         * Selects folders item in the menu
         */
        fun selectFoldersMenuItem()

        /**
         * Selects tags item in the menu
         */
        fun selectTagsMenuItem()

        /**
         * Selects groups menu item
         */
        fun selectGroupsMenuItem()

        /**
         * Shows folders item that is hidden by default (available under feature flag)
         */
        fun showFoldersMenuItem()

        /**
         * Shows tags item that is hidden by default (available under feature flag)
         */
        fun showTagsMenuItem()
    }

    /**
     * Home menu filters presenter
     */
    interface Presenter : BaseContract.Presenter<View> {
        /**
         * Processes the arguments retrieved from bundle
         * @param menuModel Home filters menu model containing the currently active filter
         */
        fun argsRetrieved(menuModel: FiltersMenuModel)

        /**
         * Callback when view is being created. Can be used to process visibility of extra views (i.e. folders)
         * as soon as possible
         */
        fun creatingView()

        fun allItemsClick()
        fun favouritesClick()
        fun recentlyModifiedClick()
        fun sharedWithMeClick()
        fun ownedByMeClick()
        fun foldersClick()
        fun tagsClick()
        fun groupsClick()
        fun expiryClick()
    }
}
