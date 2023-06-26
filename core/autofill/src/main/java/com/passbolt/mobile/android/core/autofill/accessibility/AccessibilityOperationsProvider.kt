package com.passbolt.mobile.android.core.autofill.accessibility

import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_TEXT
import android.view.accessibility.AccessibilityWindowInfo
import com.passbolt.mobile.android.common.ResourceDimenProvider

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
class AccessibilityOperationsProvider(
    private val resourceDimenProvider: ResourceDimenProvider
) {

    fun fillNode(node: AccessibilityNodeInfo, value: String) {
        val bundle = Bundle().apply {
            putString(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
        }
        node.performAction(ACTION_SET_TEXT, bundle)
    }

    private fun isEditText(node: AccessibilityNodeInfo): Boolean =
        (node.className != null) && node.className.contains("EditText")

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
        val usernameEditText = getUsernameNode(allNodes, passwordNode)

        var isUsernameEditText = false
        if (usernameEditText != null) {
            isUsernameEditText = isSameNode(usernameEditText, event.source)
        }

        return isUsernameEditText
    }

    private fun isSameNode(node1: AccessibilityNodeInfo?, node2: AccessibilityNodeInfo?): Boolean {
        if (node1 != null && node2 != null) {
            return node1 == node2 || node1.hashCode() == node2.hashCode()
        }
        return false
    }

    fun needToAutofill(credentials: AccessibilityCommunicator.Credentials?, currentUriString: String?): Boolean {
        if (credentials == null) {
            return false
        }
        val lastUri = Uri.parse(credentials.uri)
        val currentUri = Uri.parse(currentUriString ?: "")
        return lastUri.host == currentUri.host
    }

    fun getUri(root: AccessibilityNodeInfo): String? {
        val uri = ANDROID_APP_PROTOCOL + root.packageName
        val browser = supportedBrowsers.find { it.packageName == root.packageName }
        if (browser != null) {
            val addressNode = root.findAccessibilityNodeInfosByViewId(
                "${root.packageName}:id/${browser.viewId}"
            ).firstOrNull() ?: return null

            return extractUri(uri, addressNode)
        }
        return uri
    }

    private fun extractUri(uri: String, addressNode: AccessibilityNodeInfo): String {
        if (addressNode.text == null) {
            return uri
        }
        val newUri = addressNode.text
        var extractedUri = uri
        if (newUri != null && newUri.contains(".")) {
            val hasHttpProtocol = uri.startsWith("http://") || uri.startsWith("https://")
            if (!hasHttpProtocol) {
                try {
                    Uri.parse("https://$newUri")
                    extractedUri = "https://$newUri"
                } catch (e: Exception) {
                    // ignoring
                }
            } else {
                try {
                    Uri.parse(newUri.toString())
                    extractedUri = newUri.toString()
                } catch (e: Exception) {
                    // ignoring
                }
            }
        }

        return extractedUri
    }

    fun getOverlayAnchorPosition(
        anchorView: AccessibilityNodeInfo?,
        height: Int,
        isOverlayAboveAnchor: Boolean
    ): Point {
        val anchorViewRect = Rect()
        anchorView?.getBoundsInScreen(anchorViewRect)
        val anchorViewX = anchorViewRect.left
        var anchorViewY = if (isOverlayAboveAnchor) anchorViewRect.top else anchorViewRect.bottom
        if (isOverlayAboveAnchor) {
            anchorViewY -= height
        }

        return Point(anchorViewX, anchorViewY)
    }

    private fun getInputMethodHeight(windows: List<AccessibilityWindowInfo>?): Int {
        var inputMethodWindowHeight = 0
        windows?.forEach {
            if (it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD) {
                val windowRect = Rect()
                it.getBoundsInScreen(windowRect)
                inputMethodWindowHeight = windowRect.height()
                return@forEach
            }
        }
        return inputMethodWindowHeight
    }

    private fun getNodeHeight(node: AccessibilityNodeInfo?): Int {
        if (node == null) {
            return -1
        }
        val nodeRect = Rect()
        node.getBoundsInScreen(nodeRect)
        return nodeRect.height()
    }

    fun getOverlayAnchorPosition(
        anchorNode: AccessibilityNodeInfo?,
        root: AccessibilityNodeInfo?,
        windows: List<AccessibilityWindowInfo>?,
        overlayViewHeight: Int,
        isOverlayAboveAnchor: Boolean
    ): OverlayPosition? {
        var point: OverlayPosition? = null
        if (anchorNode != null) {
            anchorNode.refresh()
            if (!anchorNode.isVisibleToUser) {
                return OverlayPosition.OutBoundsHide
            }
            if (!anchorNode.isFocused) {
                return null
            }
            val inputMethodHeight = getInputMethodHeight(windows)
            point = countOverlayPosition(anchorNode, root, overlayViewHeight, isOverlayAboveAnchor, inputMethodHeight)
        }
        return point
    }

    private fun countOverlayPosition(
        anchorNode: AccessibilityNodeInfo,
        root: AccessibilityNodeInfo?,
        overlayViewHeight: Int,
        isOverlayAboveAnchor: Boolean,
        inputMethodHeight: Int
    ): OverlayPosition? {
        val minY = 0
        val rootNodeHeight = getNodeHeight(root)
        if (rootNodeHeight == -1) {
            return null
        }
        val maxY = rootNodeHeight - resourceDimenProvider.getNavigationBarHeight() -
                resourceDimenProvider.getStatusBarHeight() - inputMethodHeight
        val point = getOverlayAnchorPosition(anchorNode, overlayViewHeight, isOverlayAboveAnchor)

        val position = if (point.y < minY) {
            if (isOverlayAboveAnchor) {
                OverlayPosition.InBoundsBottomAnchor
            } else {
                OverlayPosition.OutBoundsHide
            }
        } else if (point.y > (maxY - overlayViewHeight)) {
            if (isOverlayAboveAnchor) {
                OverlayPosition.OutBoundsHide
            } else {
                OverlayPosition.InBoundsTopAnchor
            }
        } else if (isOverlayAboveAnchor && point.y < (maxY - (overlayViewHeight * 2) - getNodeHeight(anchorNode))) {
            OverlayPosition.ForceBottom
        } else OverlayPosition.Position(point.x, point.y)
        return position
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

    fun createOverlayParams() =
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, PixelFormat.TRANSPARENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

    fun shouldSkipPackage(eventPackageName: String?): Boolean {
        if (eventPackageName.isNullOrEmpty() ||
            filteredPackageNames.any { it == eventPackageName } ||
            eventPackageName.contains("launcher", true)
        ) {
            return true
        }
        return false
    }

    private val filteredPackageNames = hashSetOf(
        "com.android.systemui",
        "com.google.android.googlequicksearchbox"
    )

    private val supportedBrowsers = listOf(
        Browser("com.android.browser", "url"),
        Browser("com.android.chrome", "url_bar"),
        Browser(
            "org.mozilla.firefox", "mozac_browser_toolbar_url_view"
        ),
        Browser("org.mozilla.firefox_beta", "mozac_browser_toolbar_url_view"),
        Browser("com.opera.browser", "url_field"),
        Browser("com.opera.browser.beta", "url_field"),
        Browser("com.opera.mini.native", "url_field"),
        Browser("com.opera.mini.native.beta", "url_field"),
        Browser("com.opera.touch", "addressbarEdit"),
        Browser("com.brave.browser", "url_bar"),
        Browser("com.brave.browser_beta", "url_bar"),
        Browser("com.brave.browser_default", "url_bar"),
        Browser("com.brave.browser_dev", "url_bar"),
        Browser("com.brave.browser_nightly", "url_bar"),
        Browser("com.chrome.beta", "url_bar"),
        Browser("com.chrome.canary", "url_bar"),
        Browser("com.chrome.dev", "url_bar")
    )

    sealed class OverlayPosition {
        object InBoundsBottomAnchor : OverlayPosition()
        object InBoundsTopAnchor : OverlayPosition()
        object OutBoundsHide : OverlayPosition()
        object ForceBottom : OverlayPosition()
        class Position(val x: Int, val y: Int) : OverlayPosition()
    }

    companion object {
        private const val ANDROID_APP_PROTOCOL = "androidapp://"
    }
}
