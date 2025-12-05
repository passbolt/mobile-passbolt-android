package com.passbolt.mobile.android.locationdetails.data

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

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.ui.FolderModel
import com.passbolt.mobile.android.ui.ResourcePermission
import org.junit.Test

class ExpandableFolderTreeCreatorTest {
    @Test
    fun `create should use custom fake root name`() {
        val customRootName = "Root Name"
        val creator = ExpandableFolderTreeCreator(customRootName)

        val result = creator.create(listOf(testFolder))

        assertThat(result.rootNodes).hasSize(1)
        assertThat(
            result.rootNodes
                .first()
                .folderModel.name,
        ).isEqualTo(customRootName)
    }

    @Test
    fun `create should set fake root properties correctly`() {
        val creator = ExpandableFolderTreeCreator("Root")

        val result = creator.create(listOf(testFolder))

        val fakeRootNode = result.rootNodes.first()
        assertThat(fakeRootNode.id).isEqualTo("root_folder_id")
        assertThat(fakeRootNode.folderModel.folderId).isEqualTo("root_folder_id")
        assertThat(fakeRootNode.folderModel.parentFolderId).isNull()
        assertThat(fakeRootNode.folderModel.isShared).isFalse()
        assertThat(fakeRootNode.folderModel.permission).isEqualTo(ResourcePermission.OWNER)
        assertThat(fakeRootNode.depth).isEqualTo(0)
    }

    @Test
    fun `create should build linear chain from folder list`() {
        val creator = ExpandableFolderTreeCreator("Root")
        val folders =
            listOf(
                FolderModel("folder1", null, "Folder 1", false, ResourcePermission.OWNER),
                FolderModel("folder2", "folder1", "Folder 2", true, ResourcePermission.UPDATE),
                FolderModel("folder3", "folder2", "Folder 3", false, ResourcePermission.READ),
            )

        val result = creator.create(folders)

        // FakeRoot -> Folder1 -> Folder2 -> Folder3
        val root = result.rootNodes.first()
        assertThat(root.folderModel.name).isEqualTo("Root")
        assertThat(root.depth).isEqualTo(0)

        val folder1Node = root.children.first()
        assertThat(folder1Node.folderModel).isEqualTo(folders[0])
        assertThat(folder1Node.depth).isEqualTo(1)

        val folder2Node = folder1Node.children.first()
        assertThat(folder2Node.folderModel).isEqualTo(folders[1])
        assertThat(folder2Node.depth).isEqualTo(2)

        val folder3Node = folder2Node.children.first()
        assertThat(folder3Node.folderModel).isEqualTo(folders[2])
        assertThat(folder3Node.depth).isEqualTo(3)
        assertThat(folder3Node.children).isEmpty()
    }

    @Test
    fun `create should find correct deepest node for expand to`() {
        val creator = ExpandableFolderTreeCreator("Root")
        val folders =
            listOf(
                FolderModel("level1", null, "Level 1", false, ResourcePermission.OWNER),
                FolderModel("level2", "level1", "Level 2", false, ResourcePermission.OWNER),
                FolderModel("level3", "level2", "Level 3", false, ResourcePermission.OWNER),
            )

        val result = creator.create(folders)

        assertThat(result.expandToNode).isNotNull()
        val expandToNode = result.expandToNode!!
        assertThat(expandToNode.folderModel.folderId).isEqualTo("level3")
        assertThat(expandToNode.folderModel.name).isEqualTo("Level 3")
        assertThat(expandToNode.depth).isEqualTo(3)
        assertThat(expandToNode.children).isEmpty()
    }

    @Test
    fun `create should handle single folder correctly`() {
        val creator = ExpandableFolderTreeCreator("Root")

        val result = creator.create(listOf(testFolder))

        val rootNode = result.rootNodes.first()
        assertThat(rootNode.children).hasSize(1)

        val singleFolderNode = rootNode.children.first()
        assertThat(singleFolderNode.folderModel).isEqualTo(testFolder)
        assertThat(singleFolderNode.children).isEmpty()
        assertThat(singleFolderNode.depth).isEqualTo(1)

        assertThat(result.expandToNode).isEqualTo(singleFolderNode)
    }

    private companion object {
        private val testFolder =
            FolderModel(
                folderId = "test-folder-id",
                parentFolderId = "parent-id",
                name = "Test Folder",
                isShared = false,
                permission = ResourcePermission.OWNER,
            )
    }
}
