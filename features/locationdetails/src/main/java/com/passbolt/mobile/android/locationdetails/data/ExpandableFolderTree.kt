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

package com.passbolt.mobile.android.locationdetails.data

import com.passbolt.mobile.android.ui.FolderModel
import com.passbolt.mobile.android.ui.ResourcePermission

data class ExpandableFolderNode(
    val id: String,
    val folderModel: FolderModel,
    val children: List<ExpandableFolderNode> = emptyList(),
    val depth: Int = 0,
)

data class ExpandableFolderTree(
    val rootNodes: List<ExpandableFolderNode>,
    val expandToNode: ExpandableFolderNode?,
)

class ExpandableFolderTreeCreator(
    private val fakeRootFolderName: String,
) {
    fun create(parentFolders: List<FolderModel>): ExpandableFolderTree {
        if (parentFolders.isEmpty()) {
            return ExpandableFolderTree(emptyList(), null)
        }

        val foldersWithFakeRoot = createFoldersWithFakeRoot(parentFolders)
        val rootFolder = foldersWithFakeRoot.first()

        val rootNode = createNodeFromFolder(rootFolder, foldersWithFakeRoot, 0)
        val expandToNode = findDeepestNode(rootNode)

        return ExpandableFolderTree(listOf(rootNode), expandToNode)
    }

    /**
     * Tree needs to start from root and root is not stored in the database as folder.
     * Create a fake folder and link it with the oldest parent
     */
    private fun createFoldersWithFakeRoot(folders: List<FolderModel>): List<FolderModel> {
        val fakeRootFolder =
            FolderModel(
                folderId = FAKE_ROOT_FOLDER_ID,
                parentFolderId = null,
                name = fakeRootFolderName,
                isShared = false,
                permission = ResourcePermission.OWNER,
            )
        return listOf(fakeRootFolder) + folders
    }

    private fun createNodeFromFolder(
        folder: FolderModel,
        allFolders: List<FolderModel>,
        depth: Int,
    ): ExpandableFolderNode {
        val children =
            if (depth < allFolders.size - 1) {
                listOf(createNodeFromFolder(allFolders[depth + 1], allFolders, depth + 1))
            } else {
                emptyList()
            }

        return ExpandableFolderNode(
            id = folder.folderId,
            folderModel = folder,
            children = children,
            depth = depth,
        )
    }

    private fun findDeepestNode(node: ExpandableFolderNode): ExpandableFolderNode =
        if (node.children.isEmpty()) node else findDeepestNode(node.children.first())

    private companion object {
        private const val FAKE_ROOT_FOLDER_ID = "root_folder_id"
    }
}

/**
 * Flattens the tree into a list based on expanded IDs for UI layer
 */
internal fun flattenTree(
    nodes: List<ExpandableFolderNode>,
    expandedIds: Set<String>,
): List<ExpandableFolderNode> =
    buildList {
        nodes.forEach { node ->
            add(node)
            if (expandedIds.contains(node.id)) {
                addAll(flattenTree(node.children, expandedIds))
            }
        }
    }

/**
 * Creates initial expanded IDs to show the full path to the target folder
 */
internal fun createExpandedIds(folderTree: ExpandableFolderTree): Set<String> =
    buildSet {
        fun addExpandedNodesRecursively(nodes: List<ExpandableFolderNode>) {
            nodes.forEach { node ->
                if (node.children.isNotEmpty()) {
                    add(node.id)
                    addExpandedNodesRecursively(node.children)
                }
            }
        }
        addExpandedNodesRecursively(folderTree.rootNodes)
    }
