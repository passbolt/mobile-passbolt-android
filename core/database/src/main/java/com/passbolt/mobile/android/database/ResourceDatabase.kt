package com.passbolt.mobile.android.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.passbolt.mobile.android.database.impl.folderandgroupscrossref.FolderAndGroupsCrossRefDao
import com.passbolt.mobile.android.database.impl.folderanduserscrossref.FoldersAndUsersCrossRefDao
import com.passbolt.mobile.android.database.impl.folders.FoldersDao
import com.passbolt.mobile.android.database.impl.groups.GroupsDao
import com.passbolt.mobile.android.database.impl.metadata.MetadataKeysDao
import com.passbolt.mobile.android.database.impl.metadata.MetadataPrivateKeysDao
import com.passbolt.mobile.android.database.impl.metadata.ResourceMetadataDao
import com.passbolt.mobile.android.database.impl.metadata.ResourceUriDao
import com.passbolt.mobile.android.database.impl.resourceandgroupscrossref.ResourceAndGroupsCrossRefDao
import com.passbolt.mobile.android.database.impl.resourceandtagcrossref.ResourcesAndTagsCrossRefDao
import com.passbolt.mobile.android.database.impl.resourceanduserscrossref.ResourcesAndUsersCrossRefDao
import com.passbolt.mobile.android.database.impl.resources.ResourcesDao
import com.passbolt.mobile.android.database.impl.resourcetypes.ResourceTypesDao
import com.passbolt.mobile.android.database.impl.tags.TagsDao
import com.passbolt.mobile.android.database.impl.users.UsersDao
import com.passbolt.mobile.android.database.impl.usersandgroupscrossref.UsersAndGroupsCrossRefDao
import com.passbolt.mobile.android.database.typeconverters.Converters
import com.passbolt.mobile.android.entity.folder.Folder
import com.passbolt.mobile.android.entity.folder.FolderAndUsersCrossRef
import com.passbolt.mobile.android.entity.group.FolderAndGroupsCrossRef
import com.passbolt.mobile.android.entity.group.ResourceAndGroupsCrossRef
import com.passbolt.mobile.android.entity.group.UsersAndGroupCrossRef
import com.passbolt.mobile.android.entity.group.UsersGroup
import com.passbolt.mobile.android.entity.metadata.MetadataKey
import com.passbolt.mobile.android.entity.metadata.MetadataPrivateKey
import com.passbolt.mobile.android.entity.resource.Resource
import com.passbolt.mobile.android.entity.resource.ResourceAndTagsCrossRef
import com.passbolt.mobile.android.entity.resource.ResourceMetadata
import com.passbolt.mobile.android.entity.resource.ResourceType
import com.passbolt.mobile.android.entity.resource.ResourceUri
import com.passbolt.mobile.android.entity.resource.Tag
import com.passbolt.mobile.android.entity.user.ResourceAndUsersCrossRef
import com.passbolt.mobile.android.entity.user.User

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
        Tag::class,
        ResourceAndTagsCrossRef::class,
        UsersGroup::class,
        ResourceAndGroupsCrossRef::class,
        User::class,
        UsersAndGroupCrossRef::class,
        ResourceAndUsersCrossRef::class,
        FolderAndUsersCrossRef::class,
        FolderAndGroupsCrossRef::class,
        ResourceMetadata::class,
        ResourceUri::class,
        MetadataKey::class,
        MetadataPrivateKey::class,
    ],
    version = 21,
)
@TypeConverters(Converters::class)
abstract class ResourceDatabase : RoomDatabase() {
    abstract fun resourcesDao(): ResourcesDao

    abstract fun resourceMetadataDao(): ResourceMetadataDao

    abstract fun resourceUriDao(): ResourceUriDao

    abstract fun resourceTypesDao(): ResourceTypesDao

    abstract fun foldersDao(): FoldersDao

    abstract fun tagsDao(): TagsDao

    abstract fun resourcesAndTagsCrossRefDao(): ResourcesAndTagsCrossRefDao

    abstract fun groupsDao(): GroupsDao

    abstract fun resourcesAndGroupsCrossRefDao(): ResourceAndGroupsCrossRefDao

    abstract fun usersDao(): UsersDao

    abstract fun usersAndGroupsCrossRefDao(): UsersAndGroupsCrossRefDao

    abstract fun resourcesAndUsersCrossRefDao(): ResourcesAndUsersCrossRefDao

    abstract fun folderAndGroupsCrossRefDao(): FolderAndGroupsCrossRefDao

    abstract fun folderAndUsersCrossRefDao(): FoldersAndUsersCrossRefDao

    abstract fun metadataKeysDao(): MetadataKeysDao

    abstract fun metadataPrivateKeysDao(): MetadataPrivateKeysDao
}
