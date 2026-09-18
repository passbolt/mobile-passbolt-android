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

import androidx.room3.Room
import androidx.room3.testing.MigrationTestHelper
import androidx.room3.useWriterConnection
import androidx.sqlite.execSQL
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.database.helpers.testSqlCipherDriver
import com.passbolt.mobile.android.database.migrations.Migration10to11
import com.passbolt.mobile.android.database.migrations.Migration11to12
import com.passbolt.mobile.android.database.migrations.Migration12to13
import com.passbolt.mobile.android.database.migrations.Migration13to14
import com.passbolt.mobile.android.database.migrations.Migration14to15
import com.passbolt.mobile.android.database.migrations.Migration15to16
import com.passbolt.mobile.android.database.migrations.Migration16to17
import com.passbolt.mobile.android.database.migrations.Migration17to18
import com.passbolt.mobile.android.database.migrations.Migration18to19
import com.passbolt.mobile.android.database.migrations.Migration19to20
import com.passbolt.mobile.android.database.migrations.Migration1to2
import com.passbolt.mobile.android.database.migrations.Migration20to21
import com.passbolt.mobile.android.database.migrations.Migration21to22
import com.passbolt.mobile.android.database.migrations.Migration22to23
import com.passbolt.mobile.android.database.migrations.Migration23to24
import com.passbolt.mobile.android.database.migrations.Migration24to25
import com.passbolt.mobile.android.database.migrations.Migration25to26
import com.passbolt.mobile.android.database.migrations.Migration26to27
import com.passbolt.mobile.android.database.migrations.Migration27to28
import com.passbolt.mobile.android.database.migrations.Migration28to29
import com.passbolt.mobile.android.database.migrations.Migration2to3
import com.passbolt.mobile.android.database.migrations.Migration3to4
import com.passbolt.mobile.android.database.migrations.Migration4to5
import com.passbolt.mobile.android.database.migrations.Migration5to6
import com.passbolt.mobile.android.database.migrations.Migration6to7
import com.passbolt.mobile.android.database.migrations.Migration7to8
import com.passbolt.mobile.android.database.migrations.Migration8to9
import com.passbolt.mobile.android.database.migrations.Migration9to10
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DatabaseMigrationsTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val databaseFile = instrumentation.targetContext.getDatabasePath(TEST_DB)
    private val driver = testSqlCipherDriver()

    @get:Rule
    val helper: MigrationTestHelper =
        MigrationTestHelper(
            instrumentation = instrumentation,
            file = databaseFile,
            driver = driver,
            databaseClass = ResourceDatabase::class,
        )

    @Before
    fun setUp() {
        instrumentation.targetContext.deleteDatabase(TEST_DB)
    }

    @Test
    fun migrate1To2() =
        runTest {
            helper.createDatabase(1).apply {
                execSQL("INSERT INTO ResourceType VALUES(1, 'resourceTypeName')")
                close()
            }

            helper
                .runMigrationsAndValidate(2, listOf(Migration1to2))
                .apply {
                    execSQL("INSERT INTO ResourceType VALUES(2, 'resourceTypeName', 'resourceTypeSlug')")
                    close()
                }
        }

    @Test
    fun migrate2To3() =
        runTest {
            helper.createDatabase(2).apply {
                execSQL(
                    "INSERT INTO Resource VALUES('id','name','READ','url','username','desc','typeId','1'," +
                        "'folderName','READ','1')",
                )
                close()
            }

            helper
                .runMigrationsAndValidate(3, listOf(Migration2to3))
                .apply {
                    execSQL(
                        "INSERT INTO Resource VALUES('id2','name','READ','url','username','desc','typeId', '1','1'," +
                            "'folderName','READ','1')",
                    )
                    close()
                }
        }

    @Test
    fun migrate3To4() =
        runTest {
            helper.createDatabase(3).apply {
                execSQL(
                    "INSERT INTO Resource VALUES('id1','name','READ','url','username','desc','typeId', " +
                        "'1','1','folderName','READ','1')",
                )
                close()
            }

            helper
                .runMigrationsAndValidate(4, listOf(Migration3to4))
                .apply {
                    execSQL(
                        "INSERT INTO Resource VALUES('id2','name','READ','url','username','desc','typeId', '1'," +
                            "1644909225833, '1','folderName','READ','1')",
                    )
                    close()
                }
        }

    @Test
    fun migrate4To5() =
        runTest {
            helper.createDatabase(4).apply {
                execSQL(
                    "INSERT INTO Resource VALUES('id1','name','READ','url','username','desc','typeId', '1'," +
                        "1644909225833, '1','folderName','READ','1')",
                )
                close()
            }

            helper
                .runMigrationsAndValidate(5, listOf(Migration4to5))
                .apply {
                    execSQL(
                        "INSERT INTO Resource VALUES('id2','folderid','name','READ','url','username','desc'," +
                            "'typeId', '1',1644909225833)",
                    )
                    close()
                }
        }

    @Test
    fun migrate5To6() =
        runTest {
            helper.createDatabase(5).apply {
                close()
            }

            helper
                .runMigrationsAndValidate(6, listOf(Migration5to6))
                .apply {
                    execSQL("INSERT INTO Tag VALUES('id1','tagSlug',0)")
                    execSQL("INSERT INTO ResourceAndTagsCrossRef VALUES('id1','id2')")
                    close()
                }
        }

    @Test
    fun migrate6To7() =
        runTest {
            helper.createDatabase(6).apply {
                close()
            }

            helper
                .runMigrationsAndValidate(7, listOf(Migration6to7))
                .apply {
                    execSQL("INSERT INTO UsersGroup VALUES('id1','name')")
                    execSQL("INSERT INTO ResourceAndGroupsCrossRef VALUES('resId1','id1')")
                    close()
                }
        }

    @Test
    fun migrate7To8() =
        runTest {
            helper.createDatabase(7).apply {
                execSQL("INSERT INTO ResourceAndGroupsCrossRef VALUES('resId1','id1')")
                close()
            }

            helper
                .runMigrationsAndValidate(8, listOf(Migration7to8))
                .apply {
                    execSQL(
                        "INSERT INTO User VALUES('id','username','fName','lName','avatar','armoredKey'," +
                            "4096,'uid','keyId','fingerprint','type',1644909225833)",
                    )
                    execSQL(
                        "INSERT INTO ResourceAndGroupsCrossRef VALUES('resourceId','groupId','READ')",
                    )
                    execSQL(
                        "INSERT INTO UsersAndGroupCrossRef VALUES('userId','groupId')",
                    )
                    execSQL(
                        "INSERT INTO ResourceAndUsersCrossRef VALUES('resourceId','userId', 'OWNER')",
                    )
                    close()
                }
        }

    @Test
    fun migrate8To9() =
        runTest {
            helper.createDatabase(8).apply {
                execSQL(
                    "INSERT INTO ResourceAndGroupsCrossRef VALUES('resourceId','groupId','READ')",
                )
                execSQL(
                    "INSERT INTO ResourceAndUsersCrossRef VALUES('resourceId','userId', 'OWNER')",
                )
                close()
            }

            helper
                .runMigrationsAndValidate(9, listOf(Migration8to9))
                .apply {
                    execSQL(
                        "INSERT INTO ResourceAndGroupsCrossRef VALUES('resourceId9','groupId9','READ','permissionId')",
                    )
                    execSQL(
                        "INSERT INTO ResourceAndUsersCrossRef VALUES('resourceId9','userId9', 'OWNER','permissionId')",
                    )
                    close()
                }
        }

    @Test
    fun migrate9To10() =
        runTest {
            helper.createDatabase(9).apply {
                execSQL(
                    "INSERT INTO Resource VALUES('id2','folderid','name','READ','url','username','desc'," +
                        "'typeId', '1',1644909225833)",
                )
                close()
            }

            helper
                .runMigrationsAndValidate(10, listOf(Migration9to10))
                .apply {
                    execSQL(
                        "INSERT INTO Resource VALUES('id2','folderid','name','READ','url','username','desc'," +
                            "'typeId', 'favouriteId',1644909225833)",
                    )
                    close()
                }
        }

    @Test
    fun migrate10To11() =
        runTest {
            helper.createDatabase(10).apply {
                close()
            }

            helper
                .runMigrationsAndValidate(11, listOf(Migration10to11))
                .apply {
                    execSQL("INSERT INTO FolderAndUsersCrossRef VALUES('folderId','userId','READ','permId1')")
                    execSQL("INSERT INTO FolderAndGroupsCrossRef VALUES('folderId','groupId','READ','permId2')")
                    close()
                }
        }

    @Test
    fun migrate11To12() =
        runTest {
            helper
                .createDatabase(11)
                .apply {
                    close()
                }

            helper
                .runMigrationsAndValidate(12, listOf(Migration11to12))
                .apply {
                    // foreign key constraints added - nothing to test here
                    close()
                }
        }

    @Test
    fun migrate12To13() =
        runTest {
            helper
                .createDatabase(12)
                .apply {
                    execSQL(
                        "INSERT INTO User VALUES('id','username','fName','lName','avatar','armoredKey'," +
                            "4096,'uid','keyId','fingerprint','type',1644909225833)",
                    )
                    close()
                }

            helper
                .runMigrationsAndValidate(13, listOf(Migration12to13))
                .apply {
                    execSQL(
                        "INSERT INTO User VALUES('id2','username','fName','lName','avatar','armoredKey'," +
                            "4096,'uid','keyId','fingerprint','type',1644909225833, 1644909225830)",
                    )
                    close()
                }
        }

    @Test
    fun migrate13To14() =
        runTest {
            helper
                .createDatabase(13)
                .apply {
                    execSQL(
                        "INSERT INTO User VALUES('id','username','fName','lName','avatar','armoredKey'," +
                            "4096,'uid','keyId','fingerprint','type',1644909225833, 1644909225830)",
                    )
                    close()
                }

            helper
                .runMigrationsAndValidate(14, listOf(Migration13to14))
                .apply {
                    execSQL(
                        "INSERT INTO User VALUES('id2','username',1,'fName','lName','avatar','armoredKey'," +
                            "4096,'uid','keyId','fingerprint','type',1644909225833, 1644909225830)",
                    )
                    close()
                }
        }

    @Test
    fun migrate14To15() =
        runTest {
            helper
                .createDatabase(14)
                .apply {
                    execSQL("INSERT INTO ResourceType VALUES('1', 'resourceTypeName', 'resourceTypeSlug')")
                    execSQL(
                        "INSERT INTO Resource VALUES('id1','folderid','name','READ','url','username','desc'," +
                            "'1', 'favouriteId',1644909225833)",
                    )
                    close()
                }

            helper
                .runMigrationsAndValidate(15, listOf(Migration14to15))
                .apply {
                    execSQL(
                        "INSERT INTO ResourceType VALUES('2', 'resourceTypeName', 'resourceTypeSlug', " +
                            "'resourceSchemaJson', 'secretSchemaJson')",
                    )

                    execSQL(
                        "INSERT INTO Resource VALUES('id3','folderid','name','READ','url','username','desc'," +
                            "'typeId', 'favouriteId', 1644909225833, null,'resourceJson')",
                    )
                    close()
                }
        }

    @Test
    fun migrate15To16() =
        runTest {
            helper
                .createDatabase(15)
                .apply {
                    execSQL(
                        "INSERT INTO ResourceType VALUES('2', 'resourceTypeName', 'resourceTypeSlug', " +
                            "'resourceSchemaJson', 'secretSchemaJson')",
                    )

                    execSQL(
                        "INSERT INTO Resource VALUES('id2','folderid','name','READ','url','username','desc'," +
                            "'2', 'favouriteId',1644909225833,1644909225833, 'resourceJson')",
                    )
                    close()
                }

            helper
                .runMigrationsAndValidate(16, listOf(Migration15to16))
                .apply {
                    execSQL(
                        "INSERT INTO ResourceType VALUES('3', 'resourceTypeName', 'resourceTypeSlug', " +
                            "'resourceSchemaJson', 'secretSchemaJson', 1644909225833)",
                    )
                    execSQL(
                        "INSERT INTO ResourceType VALUES('4', 'resourceTypeName', 'resourceTypeSlug', " +
                            "'resourceSchemaJson', 'secretSchemaJson', null)",
                    )
                    execSQL(
                        "INSERT INTO Resource VALUES('id2','folderid','READ', '2'," +
                            " 'favouriteId', 1644909225833, 1644909225833)",
                    )
                    execSQL(
                        "INSERT INTO ResourceMetadata VALUES(1, 'id2', 'metadataJson', 'name', 'username', 'desc')",
                    )
                    execSQL("INSERT INTO ResourceUri VALUES(1, 'id2', 'uri1')")
                    execSQL("INSERT INTO ResourceUri VALUES(2, 'id2', 'uri2')")
                    close()
                }
        }

    @Test
    fun migrate16To17() =
        runTest {
            helper
                .createDatabase(16)
                .apply {
                    execSQL(
                        "INSERT INTO ResourceType VALUES('1', 'resourceTypeName', 'resourceTypeSlug', " +
                            "'resourceSchemaJson', 'secretSchemaJson', 1644909225833)",
                    )
                    execSQL(
                        "INSERT INTO Resource VALUES('id1','folderid','READ', '1'," +
                            " 'favouriteId', 1644909225833, 1644909225833)",
                    )
                    execSQL(
                        "INSERT INTO User VALUES('id2','username',1,'fName','lName','avatar','armoredKey'," +
                            "4096,'uid','keyId','fingerprint','type',1644909225833, 1644909225830)",
                    )
                    close()
                }

            helper
                .runMigrationsAndValidate(17, listOf(Migration16to17))
                .apply {
                    execSQL(
                        "INSERT INTO ResourceType VALUES('1', 'resourceTypeName', " +
                            "'resourceTypeSlug', 1644909225833)",
                    )

                    execSQL(
                        "INSERT INTO MetadataKey VALUES('id', 'fingerprint', 'armoredKey', " +
                            "1644909225833, 1644909225833)",
                    )
                    execSQL("INSERT INTO MetadataKey VALUES('id2', 'fingerprint', 'armoredKey', null, null)")
                    execSQL(
                        "INSERT INTO MetadataPrivateKey VALUES(0,'id', 'userId', 'data')",
                    )
                    execSQL(
                        "INSERT INTO Resource VALUES('id2','folderid','READ', '1'," +
                            " 'favouriteId', 1644909225833, 1644909225833, null, 'SHARED')",
                    )
                    execSQL(
                        "INSERT INTO Resource VALUES('id3','folderid','READ', '1'," +
                            " 'favouriteId', 1644909225833, 1644909225833, 'id', 'SHARED')",
                    )
                    execSQL(
                        "INSERT INTO User VALUES('id3','username',1,'fName','lName','avatar','userKeyId', 'armoredKey'," +
                            "4096,'uid','keyId','fingerprint','type',1644909225833, 1644909225830)",
                    )
                    close()
                }
        }

    @Test
    fun migrate17To18() =
        runTest {
            helper
                .createDatabase(17)
                .apply {
                    execSQL(
                        "INSERT INTO MetadataKey VALUES('id', 'fingerprint', 'armoredKey', " +
                            "1644909225833, 1644909225833)",
                    )
                    execSQL("INSERT INTO MetadataKey VALUES('id2', 'fingerprint', 'armoredKey', null, null)")
                    execSQL("INSERT INTO MetadataPrivateKey VALUES(0,'id', 'userId', 'data')")
                    execSQL("INSERT INTO ResourceType VALUES('1', 'resourceTypeName', 'resourceTypeSlug', 1644909225833)")
                    execSQL(
                        "INSERT INTO Resource VALUES('id2','folderid','READ', '1'," +
                            " 'favouriteId', 1644909225833, 1644909225833, null, 'SHARED')",
                    )
                    close()
                }

            helper
                .runMigrationsAndValidate(18, listOf(Migration17to18))
                .apply {
                    execSQL(
                        "INSERT INTO MetadataPrivateKey VALUES(1,'id', 'userId', 'data', 'passphrase')",
                    )
                    execSQL(
                        "INSERT INTO Resource VALUES('id3','folderid','READ', '1'," +
                            " 'favouriteId', 1644909225833, 1644909225833, null, 'SHARED')",
                    )
                    close()
                }
        }

    @Test
    fun migrate18To19() =
        runTest {
            helper
                .createDatabase(18)
                .apply {
                    execSQL(
                        "INSERT INTO MetadataKey VALUES('id', 'fingerprint', 'armoredKey', " +
                            "1644909225833, 1644909225833)",
                    )
                    execSQL("INSERT INTO MetadataPrivateKey VALUES(1,'id', 'userId', 'data', 'passphrase')")
                    close()
                }

            helper
                .runMigrationsAndValidate(19, listOf(Migration18to19))
                .apply {
                    execSQL(
                        "INSERT INTO MetadataKey VALUES('id1', 'fingerprint', 'armoredKey', 1644909225833, " +
                            "1644909225833, 1644909225833)",
                    )
                    execSQL(
                        "INSERT INTO MetadataPrivateKey VALUES('id','id1', 'userId', 'data', 'passphrase', 1644909225833, " +
                            "'modifiedBy', 1644909225833, 'createdBy', 'pgpMessage', 'fingerprint', 'domain')",
                    )
                    close()
                }
        }

    @Test
    fun migrate19To20() =
        runTest {
            helper
                .createDatabase(19)
                .apply {
                    execSQL("INSERT INTO ResourceMetadata VALUES(0, 'id', 'metadataJson', 'name', 'username', 'desc')")
                    close()
                }

            helper
                .runMigrationsAndValidate(20, listOf(Migration19to20))
                .apply {
                    execSQL(
                        "INSERT INTO ResourceMetadata VALUES(1, 'id', 'metadataJson', 'name', 'username', 'desc', 'customFieldsKeys')",
                    )
                    close()
                }
        }

    @Test
    fun migrate20To21() =
        runTest {
            helper
                .createDatabase(20)
                .apply {
                    execSQL(
                        "INSERT INTO Resource VALUES('id3','folderid','READ', '1'," +
                            " 'favouriteId', 1644909225833, 1644909225833, null, 'SHARED')",
                    )
                    execSQL(
                        "INSERT INTO ResourceMetadata VALUES(1, 'id3', 'metadataJson', 'name', 'username', 'desc', 'customFieldsKeys')",
                    )
                    close()
                }

            helper
                .runMigrationsAndValidate(21, listOf(Migration20to21))
                .apply {
                    execSQL(
                        "INSERT INTO Resource VALUES('id4','folderid','READ', '1'," +
                            " 'favouriteId', 1644909225833, 1644909225833, null, 'SHARED', 'PENDING')",
                    )
                    execSQL(
                        "INSERT INTO ResourceMetadata VALUES('id3', 'metadataJson', 'name', 'username', 'desc', 'customFieldsKeys')",
                    )
                    close()
                }
        }

    @Test
    fun migrate21To22() =
        runTest {
            helper
                .createDatabase(21)
                .apply {
                    execSQL(
                        "INSERT INTO ResourceMetadata VALUES('resId', 'metadataJson', 'name', 'username', 'desc', 'customFieldsKeys')",
                    )
                    execSQL("INSERT INTO ResourceUri VALUES(1, 'resId', 'https://example.com')")
                    execSQL("INSERT INTO Tag VALUES('tagId', 'tagSlug', 0)")
                    execSQL("INSERT INTO Folder VALUES('folderId', 'folderName', 'READ', null, 0)")
                    execSQL("INSERT INTO UsersGroup VALUES('groupId', 'groupName')")
                    close()
                }

            helper
                .runMigrationsAndValidate(22, listOf(Migration21to22))
                .apply {
                    prepare("SELECT count(*) FROM ResourceMetadataFts WHERE ResourceMetadataFts MATCH 'name*'").use { statement ->
                        statement.step()
                        assertThat(statement.getInt(0)).isEqualTo(1)
                    }

                    prepare("SELECT count(*) FROM ResourceUriFts WHERE ResourceUriFts MATCH 'example*'").use { statement ->
                        statement.step()
                        assertThat(statement.getInt(0)).isEqualTo(1)
                    }

                    prepare("SELECT count(*) FROM TagFts WHERE TagFts MATCH 'tagSlug*'").use { statement ->
                        statement.step()
                        assertThat(statement.getInt(0)).isEqualTo(1)
                    }

                    prepare("SELECT count(*) FROM FolderFts WHERE FolderFts MATCH 'folderName*'").use { statement ->
                        statement.step()
                        assertThat(statement.getInt(0)).isEqualTo(1)
                    }

                    prepare("SELECT count(*) FROM UsersGroupFts WHERE UsersGroupFts MATCH 'groupName*'").use { statement ->
                        statement.step()
                        assertThat(statement.getInt(0)).isEqualTo(1)
                    }

                    close()
                }
        }

    @Test
    fun migrate22To23() =
        runTest {
            helper
                .createDatabase(22)
                .apply {
                    execSQL("INSERT INTO Folder VALUES('folderId', 'folderName', 'READ', null, 0)")
                    close()
                }

            helper
                .runMigrationsAndValidate(23, listOf(Migration22to23))
                .apply {
                    prepare("SELECT updateState FROM Folder WHERE folderId = 'folderId'").use { statement ->
                        statement.step()
                        assertThat(statement.getText(0)).isEqualTo("UPDATED")
                    }

                    execSQL(
                        "INSERT INTO Folder VALUES('folderId2', 'folderName2', 'READ', null, 0, 'PENDING')",
                    )

                    close()
                }
        }

    @Test
    fun migrate23To24() =
        runTest {
            helper
                .createDatabase(23)
                .apply {
                    close()
                }

            helper
                .runMigrationsAndValidate(24, listOf(Migration23to24))
                .apply {
                    val indexNames = mutableListOf<String>()
                    prepare(
                        "SELECT name FROM sqlite_master WHERE type='index' AND name LIKE 'index_%'",
                    ).use { statement ->
                        while (statement.step()) {
                            indexNames.add(statement.getText(0))
                        }
                    }

                    assertThat(indexNames).containsAtLeast(
                        "index_Resource_folderId",
                        "index_Resource_resourceTypeId",
                        "index_ResourceAndTagsCrossRef_resourceId",
                        "index_ResourceAndGroupsCrossRef_groupId",
                        "index_UsersAndGroupCrossRef_groupId",
                        "index_ResourceAndUsersCrossRef_userId",
                        "index_FolderAndUsersCrossRef_folderId",
                        "index_FolderAndGroupsCrossRef_groupId",
                        "index_ResourceUri_resourceId",
                        "index_MetadataPrivateKey_metadataKeyId",
                    )

                    close()
                }
        }

    @Test
    fun migrate24To25() =
        runTest {
            helper
                .createDatabase(24)
                .apply {
                    execSQL("INSERT INTO Folder VALUES('folderId', 'folderName', 'READ', null, 0, 'UPDATED')")
                    close()
                }

            helper
                .runMigrationsAndValidate(25, listOf(Migration24to25))
                .apply {
                    prepare("SELECT modified FROM Folder WHERE folderId = 'folderId'").use { statement ->
                        statement.step()
                        assertThat(statement.getLong(0)).isEqualTo(0)
                    }

                    execSQL(
                        "INSERT INTO Folder VALUES('folderId2', 'folderName2', 'READ', null, 0, 'UPDATED', 1644909225833)",
                    )

                    close()
                }
        }

    @Test
    fun migrate25To26() =
        runTest {
            helper
                .createDatabase(25)
                .apply {
                    execSQL(
                        "INSERT INTO User VALUES('id','username',1,'fName','lName','avatar','userKeyId','armoredKey'," +
                            "4096,'uid','keyId','fingerprint','type',1644909225833, 1644909225830)",
                    )
                    close()
                }

            helper
                .runMigrationsAndValidate(26, listOf(Migration25to26))
                .apply {
                    prepare("SELECT updateState FROM User WHERE id = 'id'").use { statement ->
                        statement.step()
                        assertThat(statement.getText(0)).isEqualTo("UPDATED")
                    }

                    execSQL(
                        "INSERT INTO User VALUES('id2','username',1,'fName','lName','avatar','userKeyId','armoredKey'," +
                            "4096,'uid','keyId','fingerprint','type',1644909225833, 1644909225830, 'PENDING')",
                    )

                    close()
                }
        }

    @Test
    fun migrate26To27() =
        runTest {
            helper
                .createDatabase(26)
                .apply {
                    execSQL("INSERT INTO UsersGroup VALUES('groupId', 'groupName')")
                    close()
                }

            helper
                .runMigrationsAndValidate(27, listOf(Migration26to27))
                .apply {
                    prepare("SELECT updateState FROM UsersGroup WHERE groupId = 'groupId'").use { statement ->
                        statement.step()
                        assertThat(statement.getText(0)).isEqualTo("UPDATED")
                    }

                    execSQL("INSERT INTO UsersGroup VALUES('groupId2', 'groupName2', 'PENDING')")

                    close()
                }
        }

    @Test
    fun migrate27To28() =
        runTest {
            helper
                .createDatabase(27)
                .apply {
                    execSQL(
                        "INSERT INTO Folder(folderId, name, permission, parentId, isShared, modified, updateState) " +
                            "VALUES('folderId1', 'parentFolder', 'READ', null, 0, 1644909225833, 'UPDATED')",
                    )
                    execSQL(
                        "INSERT INTO Folder(folderId, name, permission, parentId, isShared, modified, updateState) " +
                            "VALUES('folderId2', 'childFolder', 'READ', 'folderId1', 0, 1644909225834, 'UPDATED')",
                    )
                    close()
                }

            helper
                .runMigrationsAndValidate(28, listOf(Migration27to28))
                .apply {
                    val indexNames = mutableListOf<String>()
                    prepare(
                        "SELECT name FROM sqlite_master WHERE type='index' AND name LIKE 'index_%'",
                    ).use { statement ->
                        while (statement.step()) {
                            indexNames.add(statement.getText(0))
                        }
                    }

                    assertThat(indexNames).containsAtLeast(
                        "index_Folder_parentId",
                        "index_Folder_modified_folderId",
                        "index_Resource_modified_resourceId",
                        "index_Resource_expiry_resourceId",
                        "index_Resource_favouriteId",
                    )

                    prepare("SELECT count(*) FROM Folder WHERE parentId = 'folderId1'").use { statement ->
                        statement.step()
                        assertThat(statement.getInt(0)).isEqualTo(1)
                    }

                    close()
                }
        }

    @Test
    fun migrate28To29() =
        runTest {
            helper
                .createDatabase(28)
                .apply {
                    execSQL(
                        "INSERT INTO User VALUES('userId', 'ada@passbolt.com', 0, 'UPDATED', 'Ada', 'Lovelace', null, " +
                            "'userKeyId', 'armoredKey', 4096, null, 'keyId', 'fingerprint', 'RSA', null, null)",
                    )
                    close()
                }

            helper
                .runMigrationsAndValidate(29, listOf(Migration28to29))
                .apply {
                    prepare("SELECT count(*) FROM UserFts WHERE UserFts MATCH 'lovelace*'").use { statement ->
                        statement.step()
                        assertThat(statement.getInt(0)).isEqualTo(1)
                    }

                    execSQL("UPDATE User SET lastName = 'Byron' WHERE id = 'userId'")

                    prepare("SELECT count(*) FROM UserFts WHERE UserFts MATCH 'byron*'").use { statement ->
                        statement.step()
                        assertThat(statement.getInt(0)).isEqualTo(1)
                    }

                    close()
                }
        }

    @Test
    fun migrateAll() =
        runTest {
            helper.createDatabase(1).apply {
                close()
            }

            Room
                .databaseBuilder(
                    InstrumentationRegistry.getInstrumentation().targetContext,
                    ResourceDatabase::class.java,
                    TEST_DB,
                ).addMigrations(
                    Migration1to2,
                    Migration2to3,
                    Migration3to4,
                    Migration4to5,
                    Migration5to6,
                    Migration6to7,
                    Migration7to8,
                    Migration8to9,
                    Migration9to10,
                    Migration10to11,
                    Migration11to12,
                    Migration12to13,
                    Migration13to14,
                    Migration14to15,
                    Migration15to16,
                    Migration16to17,
                    Migration17to18,
                    Migration18to19,
                    Migration19to20,
                    Migration20to21,
                    Migration21to22,
                    Migration22to23,
                    Migration23to24,
                    Migration24to25,
                    Migration25to26,
                    Migration26to27,
                    Migration27to28,
                    Migration28to29,
                ).setDriver(driver)
                .build()
                .apply {
                    useWriterConnection { }
                    close()
                }

            val header = ByteArray(PLAIN_SQLITE_HEADER.size)
            databaseFile.inputStream().use { it.read(header) }
            assertThat(header).isNotEqualTo(PLAIN_SQLITE_HEADER)
        }

    private companion object {
        private const val TEST_DB = "migration-test"
        private val PLAIN_SQLITE_HEADER = "SQLite format 3".toByteArray()
    }
}
