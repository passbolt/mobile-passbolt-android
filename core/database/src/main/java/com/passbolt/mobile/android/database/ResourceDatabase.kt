package com.passbolt.mobile.android.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.passbolt.mobile.android.database.dao.FoldersDao
import com.passbolt.mobile.android.database.dao.GroupsDao
import com.passbolt.mobile.android.database.dao.ResourceAndGroupsCrossRefDao
import com.passbolt.mobile.android.database.dao.ResourceFieldsDao
import com.passbolt.mobile.android.database.dao.ResourceTypesAndFieldsCrossRefDao
import com.passbolt.mobile.android.database.dao.ResourceTypesDao
import com.passbolt.mobile.android.database.dao.ResourcesAndTagsCrossRefDao
import com.passbolt.mobile.android.database.dao.ResourcesDao
import com.passbolt.mobile.android.database.dao.TagsDao
import com.passbolt.mobile.android.database.typeconverters.Converters
import com.passbolt.mobile.android.entity.group.ResourceAndGroupsCrossRef
import com.passbolt.mobile.android.entity.group.UsersGroup
import com.passbolt.mobile.android.entity.resource.Folder
import com.passbolt.mobile.android.entity.resource.Resource
import com.passbolt.mobile.android.entity.resource.ResourceAndTagsCrossRef
import com.passbolt.mobile.android.entity.resource.ResourceField
import com.passbolt.mobile.android.entity.resource.ResourceType
import com.passbolt.mobile.android.entity.resource.ResourceTypesAndFieldsCrossRef
import com.passbolt.mobile.android.entity.resource.Tag

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

@Database(
    entities = [
        Resource::class,
        Folder::class,
        ResourceType::class,
        ResourceField::class,
        ResourceTypesAndFieldsCrossRef::class,
        Tag::class,
        ResourceAndTagsCrossRef::class,
        UsersGroup::class,
        ResourceAndGroupsCrossRef::class
    ],
    version = 7
)
@TypeConverters(Converters::class)
abstract class ResourceDatabase : RoomDatabase() {

    abstract fun resourcesDao(): ResourcesDao

    abstract fun resourceTypesDao(): ResourceTypesDao

    abstract fun resourceFieldsDao(): ResourceFieldsDao

    abstract fun resourceTypesAndFieldsCrossRefDao(): ResourceTypesAndFieldsCrossRefDao

    abstract fun foldersDao(): FoldersDao

    abstract fun tagsDao(): TagsDao

    abstract fun resourcesAndTagsCrossRefDao(): ResourcesAndTagsCrossRefDao

    abstract fun groupsDao(): GroupsDao

    abstract fun resourcesAndGroupsCrossRefDao(): ResourceAndGroupsCrossRefDao
}
