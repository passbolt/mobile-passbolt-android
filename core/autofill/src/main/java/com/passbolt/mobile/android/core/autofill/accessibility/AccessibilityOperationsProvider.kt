package com.passbolt.mobile.android.core.autofill.accessibility

import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_TEXT
import android.view.accessibility.AccessibilityWindowInfo
import androidx.core.net.toUri
import com.passbolt.mobile.android.common.ResourceDimenProvider
import com.passbolt.mobile.android.core.autofill.system.AutofillField
import com.passbolt.mobile.android.core.autofill.system.AutofillHintsFactory
import com.passbolt.mobile.android.core.autofill.system.BrowserSignatureVerifier
import com.passbolt.mobile.android.core.navigation.AutofillType

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
    private val resourceDimenProvider: ResourceDimenProvider,
    private val autofillHintsFactory: AutofillHintsFactory,
    private val trustedBrowserPackages: TrustedBrowserPackages,
    private val browserSignatureVerifier: BrowserSignatureVerifier,
) {
    fun fillNode(
        node: AccessibilityNodeInfo,
        value: String,
    ) {
        val bundle =
            Bundle().apply {
                putString(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
            }
        node.performAction(ACTION_SET_TEXT, bundle)
    }

    private fun isEditText(node: AccessibilityNodeInfo): Boolean = (node.className != null) && node.className.contains("EditText")

    fun getAllNodes(
        node: AccessibilityNodeInfo,
        event: AccessibilityEvent,
    ): List<AccessibilityNodeInfo> {
        val nodesList = mutableListOf<AccessibilityNodeInfo>()
        getNodes(node, event, nodesList)
        return nodesList
    }

    fun isUsernameEditText(
        root: AccessibilityNodeInfo,
        event: AccessibilityEvent,
    ): Boolean {
        val allNodes = getAllNodes(root, event)
        val passwordNode = getPasswordNode(allNodes)?.viewIdResourceName
        val usernameEditText = getUsernameNode(allNodes, passwordNode)

        var isUsernameEditText = false
        if (usernameEditText != null) {
            isUsernameEditText = isSameNode(usernameEditText, event.source)
        }

        return isUsernameEditText
    }

    fun isTotpEditText(
        root: AccessibilityNodeInfo,
        event: AccessibilityEvent,
    ): Boolean {
        val totpNode = getTotpNode(getAllNodes(root, event)) ?: return false
        return isSameNode(totpNode, event.source)
    }

    private fun isSameNode(
        node1: AccessibilityNodeInfo?,
        node2: AccessibilityNodeInfo?,
    ): Boolean {
        if (node1 != null && node2 != null) {
            return node1 == node2 || node1.hashCode() == node2.hashCode()
        }
        return false
    }

    fun needToAutofill(
        lastFill: AccessibilityCommunicator.LastFill?,
        currentUriString: String?,
    ): Boolean {
        if (lastFill == null) {
            return false
        }
        val lastHost = lastFill.uri?.toUri()?.host
        val currentHost = currentUriString?.toUri()?.host
        return lastHost != null && lastHost == currentHost
    }

    fun getUri(root: AccessibilityNodeInfo): String? {
        val uri = ANDROID_APP_PROTOCOL + root.packageName
        val browser = trustedBrowserPackages.browsersWithUrlBar.find { it.packageName == root.packageName }
        if (browser != null && browserSignatureVerifier.isTrustedBrowser(root.packageName?.toString())) {
            val addressNode =
                root
                    .findAccessibilityNodeInfosByViewId(
                        "${root.packageName}:id/${browser.viewId}",
                    ).firstOrNull() ?: return null

            return extractUri(uri, addressNode)
        }
        return uri
    }

    private fun extractUri(
        uri: String,
        addressNode: AccessibilityNodeInfo,
    ): String {
        if (addressNode.text == null) {
            return uri
        }
        val newUri = addressNode.text
        var extractedUri = uri
        if (newUri != null && newUri.contains(".")) {
            val hasHttpProtocol = uri.startsWith("http://") || uri.startsWith("https://")
            if (!hasHttpProtocol) {
                try {
                    "https://$newUri".toUri()
                    extractedUri = "https://$newUri"
                } catch (_: Exception) {
                    // ignoring
                }
            } else {
                try {
                    newUri.toString().toUri()
                    extractedUri = newUri.toString()
                } catch (_: Exception) {
                    // ignoring
                }
            }
        }

        return extractedUri
    }

    fun getOverlayAnchorPosition(
        anchorView: AccessibilityNodeInfo?,
        height: Int,
        isOverlayAboveAnchor: Boolean,
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
        isOverlayAboveAnchor: Boolean,
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
        inputMethodHeight: Int,
    ): OverlayPosition? {
        val minY = 0
        val rootNodeHeight = getNodeHeight(root)
        if (rootNodeHeight == -1) {
            return null
        }
        val maxY =
            rootNodeHeight - resourceDimenProvider.getNavigationBarHeight() -
                resourceDimenProvider.getStatusBarHeight() - inputMethodHeight
        val point = getOverlayAnchorPosition(anchorNode, overlayViewHeight, isOverlayAboveAnchor)

        val position =
            if (point.y < minY) {
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
            } else {
                OverlayPosition.Position(point.x, point.y)
            }
        return position
    }

    fun getPasswordNode(nodes: List<AccessibilityNodeInfo>): AccessibilityNodeInfo? {
        val passwordKeywords = autofillHintsFactory.getHintValues(AutofillField.PASSWORD)
        return nodes.find { node -> node.isPassword || matchesKeywords(node, passwordKeywords) }
    }

    fun getTotpNode(nodes: List<AccessibilityNodeInfo>): AccessibilityNodeInfo? {
        val totpKeywords = autofillHintsFactory.getHintValues(AutofillField.TOTP)
        return nodes.find { node -> !node.isPassword && matchesKeywords(node, totpKeywords) }
    }

    fun classifyAutofillType(
        root: AccessibilityNodeInfo,
        event: AccessibilityEvent,
    ): AutofillType? {
        val allNodes = getAllNodes(root, event)
        val passwordNode = getPasswordNode(allNodes)
        val usernameNode = getUsernameNode(allNodes, passwordNode?.viewIdResourceName)
        val totpNode = getTotpNode(allNodes)

        val hasCredentials = usernameNode != null && passwordNode != null
        val hasTotp = totpNode != null

        return when {
            hasCredentials && hasTotp -> AutofillType.CREDENTIALS_AND_TOTP
            hasCredentials -> AutofillType.CREDENTIALS
            hasTotp -> AutofillType.TOTP
            else -> null
        }
    }

    fun getUsernameNode(
        nodes: List<AccessibilityNodeInfo>,
        passwordNodeId: String?,
    ): AccessibilityNodeInfo? {
        val usernameKeywords = autofillHintsFactory.getHintValues(AutofillField.USERNAME)
        var usernameNode: AccessibilityNodeInfo? = nodes.find { matchesKeywords(it, usernameKeywords) }

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

    private fun matchesKeywords(
        node: AccessibilityNodeInfo,
        keywords: Array<String>,
    ): Boolean {
        val hintText = node.hintText?.toString().orEmpty()
        val contentDescription = node.contentDescription?.toString().orEmpty()
        val viewId = node.viewIdResourceName.orEmpty()
        return keywords.any { keyword ->
            hintText.contains(keyword, ignoreCase = true) ||
                contentDescription.contains(keyword, ignoreCase = true) ||
                viewId.contains(keyword, ignoreCase = true)
        }
    }

    private fun getNodes(
        root: AccessibilityNodeInfo,
        event: AccessibilityEvent,
        result: MutableList<AccessibilityNodeInfo>,
    ) {
        val visited = hashSetOf(root)
        val pending = ArrayDeque<AccessibilityNodeInfo>()
        pushChildren(root, visited, pending)
        while (pending.isNotEmpty() && visited.size < MAX_VISITED_NODES) {
            val node = pending.removeLast()
            if (node.windowId == event.windowId && isEditText(node)) {
                result.add(node)
            }
            pushChildren(node, visited, pending)
        }
    }

    private fun pushChildren(
        node: AccessibilityNodeInfo,
        visited: MutableSet<AccessibilityNodeInfo>,
        pending: ArrayDeque<AccessibilityNodeInfo>,
    ) {
        for (i in node.childCount - 1 downTo 0) {
            val child = node.getChild(i) ?: continue
            if (visited.add(child)) {
                pending.addLast(child)
            }
        }
    }

    fun createOverlayParams() =
        WindowManager
            .LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSPARENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.START
            }

    fun shouldSkipPackage(eventPackageName: String?): Boolean =
        eventPackageName.isNullOrEmpty() ||
            filteredPackageNames.any { it == eventPackageName } ||
            eventPackageName.contains("launcher", true)

    private val filteredPackageNames =
        hashSetOf(
            "com.android.systemui",
            "com.google.android.googlequicksearchbox",
        )

    sealed class OverlayPosition {
        data object InBoundsBottomAnchor : OverlayPosition()

        data object InBoundsTopAnchor : OverlayPosition()

        data object OutBoundsHide : OverlayPosition()

        data object ForceBottom : OverlayPosition()

        class Position(
            val x: Int,
            val y: Int,
        ) : OverlayPosition()
    }

    companion object {
        private const val ANDROID_APP_PROTOCOL = "androidapp://"
        private const val MAX_VISITED_NODES = 10_000
    }
}
