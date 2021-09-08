package com.passbolt.mobile.android.feature.autofill.accessibility

import android.graphics.Point
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_TEXT
import timber.log.Timber
import java.lang.Exception

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
class AccessibilityOperationsProvider {

    fun fillEditText(node: AccessibilityNodeInfo, value: String) {
        val bundle = Bundle().apply {
            putString(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
        }
        node.performAction(ACTION_SET_TEXT, bundle)
    }

    private fun isEditText(node: AccessibilityNodeInfo): Boolean {
        return node.className.contains("EditText")
    }

    fun getAllNodes(
        node: AccessibilityNodeInfo,
        event: AccessibilityEvent
    ): List<AccessibilityNodeInfo> {
        val nodesList = mutableListOf<AccessibilityNodeInfo>()
        getNodes(node, event, nodesList)
        return nodesList
    }

    fun isUsernameEditText(root: AccessibilityNodeInfo, event: AccessibilityEvent): Boolean {
        val allNodes = getAllNodes(root, event)
        val passwordNode = getPasswordNode(allNodes)?.viewIdResourceName
        var usernameEditText = getUsernameNode(allNodes, passwordNode);

        var isUsernameEditText = false;
        if (usernameEditText != null) {
            isUsernameEditText = isSameNode(usernameEditText, event.source)
        }

        return isUsernameEditText
    }

    private fun isSameNode(node1: AccessibilityNodeInfo?, node2: AccessibilityNodeInfo?): Boolean {
        if (node1 != null && node2 != null) {
            return node1 == node2 || node1.hashCode() == node2.hashCode();
        }
        return false;
    }

    fun needToAutofill(credentials: AccessibilityCommunicator.Credentials?, currentUriString: String): Boolean {
        Timber.d("DDD needToAutofill: $credentials")
        if (credentials == null) {
            return false;
        }
        val lastUri = Uri.parse(credentials.uri)
        val currentUri = Uri.parse(currentUriString)
        Timber.d("DDD lastUri: ${lastUri.host}")
        Timber.d("DDD currentUri: ${currentUri.host}")
        return lastUri.host == currentUri.host
    }

    fun getUri(root: AccessibilityNodeInfo): String? {
        val uri = androidAppProtocol + root.packageName
        val browser = supportedBrowsers.find { it.packageName == root.packageName }
        if (browser != null) {
            val addressNode = root.findAccessibilityNodeInfosByViewId(
                "${root.packageName}:id/${browser.viewId}"
            ).firstOrNull() ?: return null

            return extractUri(uri, addressNode, browser)
        }
        return uri
    }

    public val androidAppProtocol = "androidapp://"

    private fun extractUri(uri: String, addressNode: AccessibilityNodeInfo, browser: Browser): String {
        if (addressNode.text == null) {
            return uri
        }
        val newUri = addressNode.text
        if (newUri != null && newUri.contains(".")) {
            val hasHttpProtocol = uri.startsWith("http://") || uri.startsWith("https://")
            if (!hasHttpProtocol) {
                try {
                    Uri.parse("https://$newUri")
                    return "https://$newUri"
                } catch (e: Exception) {
                    // ignoring
                }
            } else {
                try {
                    Uri.parse(newUri.toString())
                    return newUri.toString()
                } catch (e: Exception) {
                    // ignoring
                }
            }

        }

        return uri
    }

    fun getOverlayAnchorPosition(
        anchorView: AccessibilityNodeInfo,
        height: Int,
        width: Int
    ): Point {
        val anchorViewRect = Rect()
        anchorView.getBoundsInScreen(anchorViewRect)
        val anchorViewX = (anchorViewRect.right - width) / 2
        val anchorViewY = (anchorViewRect.bottom - height) / 2

        return Point(anchorViewX, anchorViewY)
    }

    fun getPasswordNode(nodes: List<AccessibilityNodeInfo>): AccessibilityNodeInfo? {
        return nodes.find {
            it.isPassword || it.hintText?.contains("pass", true) ?: false
        }
    }

    fun getUsernameNode(nodes: List<AccessibilityNodeInfo>, passwordNodeId: String?): AccessibilityNodeInfo? {
        var usernameNode: AccessibilityNodeInfo?

        usernameNode = nodes.find {
            it.hintText?.contains("username", true) ?: false ||
                    it.hintText?.contains("mail", true) ?: false
        }

        // username node is usually one before the password node
        if (usernameNode == null) {
            var lastNode: AccessibilityNodeInfo? = null
            nodes.forEach {
                if (it.viewIdResourceName == passwordNodeId) {
                    usernameNode = lastNode
                    return@forEach
                }
                lastNode = it
            }
        }

        return usernameNode
    }

    private fun getNodes(
        node: AccessibilityNodeInfo,
        event: AccessibilityEvent,
        result: MutableList<AccessibilityNodeInfo>
    ) {
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                if (child.windowId == event.windowId && isEditText(child)) {
                    result.add(child)
                }
                getNodes(child, event, result)
            }
        }
    }

    fun skipPackage(eventPackageName: String?): Boolean {
        if (eventPackageName.isNullOrEmpty() ||
            filteredPackageNames.any { it == eventPackageName } ||
            eventPackageName.contains("launcher", true)
        ) {
            return true;
        }
        return false
    }

    private val filteredPackageNames = hashSetOf(
        "com.android.systemui",
        "com.google.android.googlequicksearchbox",
        "com.google.android.apps.nexuslauncher",
        "com.google.android.launcher",
        "com.computer.desktop.ui.launcher",
        "com.launcher.notelauncher",
        "com.anddoes.launcher",
        "com.actionlauncher.playstore",
        "ch.deletescape.lawnchair.plah",
        "com.microsoft.launcher",
        "com.teslacoilsw.launcher",
        "com.teslacoilsw.launcher.prime",
        "is.shortcut",
        "me.craftsapp.nlauncher",
        "com.ss.squarehome2",
        "com.treydev.pns"
    )

    private val supportedBrowsers = listOf(
        Browser("com.android.browser", "url"),
        Browser("com.android.chrome", "url_bar")
    )
}
