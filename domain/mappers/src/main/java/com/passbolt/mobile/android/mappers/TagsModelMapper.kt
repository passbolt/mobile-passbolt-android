package com.passbolt.mobile.android.mappers

import com.passbolt.mobile.android.dto.response.TagDto
import com.passbolt.mobile.android.entity.resource.Tag
import com.passbolt.mobile.android.entity.resource.TagWithTaggedItemsCount
import com.passbolt.mobile.android.ui.TagModel
import com.passbolt.mobile.android.ui.TagWithCount

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
class TagsModelMapper {
    fun map(tag: TagDto): TagModel =
        TagModel(
            id = tag.id.toString(),
            slug = tag.slug,
            isShared = tag.isShared,
        )

    fun map(tagModels: List<TagModel>) =
        tagModels.map {
            Tag(
                id = it.id,
                slug = it.slug,
                isShared = it.isShared,
            )
        }

    fun map(tagEntity: Tag): TagModel =
        TagModel(
            id = tagEntity.id,
            slug = tagEntity.slug,
            isShared = tagEntity.isShared,
        )

    fun map(tag: TagWithTaggedItemsCount): TagWithCount =
        TagWithCount(
            tag.id,
            tag.slug,
            tag.isShared,
            tag.taggedItemsCount,
        )
}
