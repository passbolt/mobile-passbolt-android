package com.passbolt.mobile.android.scenarios.resourcescreation

import androidx.annotation.IdRes
import com.passbolt.mobile.android.feature.setup.R

/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2023 Passbolt SA
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

enum class VisibleCreateButton(@IdRes val filterId: Int){
    ALL_ITEMS(R.id.allItems),
    FAVOURITES(R.id.favourites),
    RECENTLY_MODIFIED(R.id.recentlyModified),
    SHARED_WITH_ME(R.id.sharedWithMe),
    OWNED_BY_ME(R.id.ownedByMe),
    FOLDERS(R.id.folders)
}

enum class HiddenCreateButton(@IdRes val filterId: Int){
    TAGS(R.id.tags),
    GROUPS(R.id.groups)
}