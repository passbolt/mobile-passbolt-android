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

package com.passbolt.mobile.android.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.passbolt.mobile.android.database.migrations.Migration10to11
import com.passbolt.mobile.android.database.migrations.Migration11to12
import com.passbolt.mobile.android.database.migrations.Migration12to13
import com.passbolt.mobile.android.database.migrations.Migration1to2
import com.passbolt.mobile.android.database.migrations.Migration2to3
import com.passbolt.mobile.android.database.migrations.Migration3to4
import com.passbolt.mobile.android.database.migrations.Migration4to5
import com.passbolt.mobile.android.database.migrations.Migration5to6
import com.passbolt.mobile.android.database.migrations.Migration6to7
import com.passbolt.mobile.android.database.migrations.Migration7to8
import com.passbolt.mobile.android.database.migrations.Migration8to9
import com.passbolt.mobile.android.database.migrations.Migration9to10
import org.junit.Rule
import org.junit.Test

class DatabaseMigrationsTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ResourceDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate1To2() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL("INSERT INTO ResourceType VALUES(1, 'resourceTypeName')")
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 2, true, Migration1to2)
            .apply {
                execSQL("INSERT INTO ResourceType VALUES(2, 'resourceTypeName', 'resourceTypeSlug')")
                close()
            }
    }

    @Test
    fun migrate2To3() {
        helper.createDatabase(TEST_DB, 2).apply {
            execSQL("INSERT INTO Resource VALUES('id','name','READ','url','username','desc','typeId','1','folderName','READ','1')")
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 3, true, Migration2to3)
            .apply {
                execSQL("INSERT INTO Resource VALUES('id2','name','READ','url','username','desc','typeId', '1','1','folderName','READ','1')")
                close()
            }
    }

    @Test
    fun migrate3To4() {
        helper.createDatabase(TEST_DB, 3).apply {
            execSQL("INSERT INTO Resource VALUES('id1','name','READ','url','username','desc','typeId', '1','1','folderName','READ','1')")
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 4, true, Migration3to4)
            .apply {
                execSQL(
                    "INSERT INTO Resource VALUES('id2','name','READ','url','username','desc','typeId', '1'," +
                            "1644909225833, '1','folderName','READ','1')"
                )
                close()
            }
    }

    @Test
    fun migrate4To5() {
        helper.createDatabase(TEST_DB, 4).apply {
            execSQL(
                "INSERT INTO Resource VALUES('id1','name','READ','url','username','desc','typeId', '1'," +
                        "1644909225833, '1','folderName','READ','1')"
            )
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 5, true, Migration4to5)
            .apply {
                execSQL(
                    "INSERT INTO Resource VALUES('id2','folderid','name','READ','url','username','desc'," +
                            "'typeId', '1',1644909225833)"
                )
                close()
            }
    }

    @Test
    fun migrate5To6() {
        helper.createDatabase(TEST_DB, 5).apply {
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 6, true, Migration5to6)
            .apply {
                execSQL("INSERT INTO Tag VALUES('id1','tagSlug',0)")
                execSQL("INSERT INTO ResourceAndTagsCrossRef VALUES('id1','id2')")
                close()
            }
    }

    @Test
    fun migrate6To7() {
        helper.createDatabase(TEST_DB, 6).apply {
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 7, true, Migration6to7)
            .apply {
                execSQL("INSERT INTO UsersGroup VALUES('id1','name')")
                execSQL("INSERT INTO ResourceAndGroupsCrossRef VALUES('resId1','id1')")
                close()
            }
    }

    @Test
    fun migrate7To8() {
        helper.createDatabase(TEST_DB, 7).apply {
            execSQL("INSERT INTO ResourceAndGroupsCrossRef VALUES('resId1','id1')")
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 8, true, Migration7to8)
            .apply {
                execSQL(
                    "INSERT INTO User VALUES('id','username','fName','lName','avatar','armoredKey'," +
                            "4096,'uid','keyId','fingerprint','type',1644909225833)"
                )
                execSQL(
                    "INSERT INTO ResourceAndGroupsCrossRef VALUES('resourceId','groupId','READ')"
                )
                execSQL(
                    "INSERT INTO UsersAndGroupCrossRef VALUES('userId','groupId')"
                )
                execSQL(
                    "INSERT INTO ResourceAndUsersCrossRef VALUES('resourceId','userId', 'OWNER')"
                )
                close()
            }
    }

    @Test
    fun migrate8To9() {
        helper.createDatabase(TEST_DB, 8).apply {
            execSQL(
                "INSERT INTO ResourceAndGroupsCrossRef VALUES('resourceId','groupId','READ')"
            )
            execSQL(
                "INSERT INTO ResourceAndUsersCrossRef VALUES('resourceId','userId', 'OWNER')"
            )
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 9, true, Migration8to9)
            .apply {
                execSQL(
                    "INSERT INTO ResourceAndGroupsCrossRef VALUES('resourceId9','groupId9','READ','permissionId')"
                )
                execSQL(
                    "INSERT INTO ResourceAndUsersCrossRef VALUES('resourceId9','userId9', 'OWNER','permissionId')"
                )
                close()
            }
    }

    @Test
    fun migrate9To10() {
        helper.createDatabase(TEST_DB, 9).apply {
            execSQL(
                "INSERT INTO Resource VALUES('id2','folderid','name','READ','url','username','desc'," +
                        "'typeId', '1',1644909225833)"
            )
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 10, true, Migration9to10)
            .apply {
                execSQL(
                    "INSERT INTO Resource VALUES('id2','folderid','name','READ','url','username','desc'," +
                            "'typeId', 'favouriteId',1644909225833)"
                )
                close()
            }
    }

    @Test
    fun migrate10To11() {
        helper.createDatabase(TEST_DB, 10).apply {
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 11, true, Migration10to11)
            .apply {
                execSQL("INSERT INTO FolderAndUsersCrossRef VALUES('folderId','userId','READ','permId1')")
                execSQL("INSERT INTO FolderAndGroupsCrossRef VALUES('folderId','groupId','READ','permId2')")
                close()
            }
    }

    @Test
    fun migrate11To12() {
        helper.createDatabase(TEST_DB, 11)
            .apply {
                close()
            }

        helper.runMigrationsAndValidate(TEST_DB, 12, true, Migration11to12)
            .apply {
                // foreign key constraints added - nothing to test here
                close()
            }
    }

    @Test
    fun migrate12To13() {
        helper.createDatabase(TEST_DB, 12)
            .apply {
                execSQL(
                    "INSERT INTO User VALUES('id','username','fName','lName','avatar','armoredKey'," +
                            "4096,'uid','keyId','fingerprint','type',1644909225833)"
                )
                close()
            }

        helper.runMigrationsAndValidate(TEST_DB, 13, true, Migration12to13)
            .apply {
                execSQL(
                    "INSERT INTO User VALUES('id2','username','fName','lName','avatar','armoredKey'," +
                            "4096,'uid','keyId','fingerprint','type',1644909225833, 1644909225830)"
                )
                close()
            }
    }

    @Test
    fun migrateAll() {
        helper.createDatabase(TEST_DB, 1).apply {
            close()
        }

        Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            ResourceDatabase::class.java,
            TEST_DB
        )
            .addMigrations(
                Migration1to2, Migration2to3, Migration3to4, Migration4to5, Migration5to6,
                Migration6to7, Migration7to8, Migration8to9, Migration9to10, Migration10to11,
                Migration11to12, Migration12to13
            )
            .build().apply {
                openHelper.writableDatabase
                close()
            }
    }

    private companion object {
        private const val TEST_DB = "migration-test"
    }
}
