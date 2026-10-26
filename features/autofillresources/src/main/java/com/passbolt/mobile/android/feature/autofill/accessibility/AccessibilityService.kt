package com.passbolt.mobile.android.feature.autofill.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.passbolt.mobile.android.core.autofill.accessibility.AccessibilityCommunicator
import com.passbolt.mobile.android.core.autofill.accessibility.AccessibilityOperationsProvider
import com.passbolt.mobile.android.core.autofill.accessibility.AccessibilityOperationsProvider.OverlayPosition
import com.passbolt.mobile.android.core.autofill.accessibility.AccessibilityOperationsProvider.OverlayPosition.ForceBottom
import com.passbolt.mobile.android.core.autofill.accessibility.AccessibilityOperationsProvider.OverlayPosition.InBoundsBottomAnchor
import com.passbolt.mobile.android.core.autofill.accessibility.AccessibilityOperationsProvider.OverlayPosition.InBoundsTopAnchor
import com.passbolt.mobile.android.core.autofill.accessibility.AccessibilityOperationsProvider.OverlayPosition.OutBoundsHide
import com.passbolt.mobile.android.core.extension.gone
import com.passbolt.mobile.android.core.extension.visible
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AutofillMode
import com.passbolt.mobile.android.core.navigation.AutofillType
import com.passbolt.mobile.android.core.notifications.accessibilityautofill.AccessibilityServiceNotificationFactory
import com.passbolt.mobile.android.core.notifications.accessibilityautofill.AccessibilityServiceNotificationFactory.Companion.ACCESSIBILITY_SERVICE_NOTIFICATION_ID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

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
class AccessibilityService :
    AccessibilityService(),
    KoinComponent {
    private val accessibilityOperationsProvider: AccessibilityOperationsProvider by inject()
    private val accessibilityCommunicator: AccessibilityCommunicator by inject()
    private val coroutineLaunchContext: CoroutineLaunchContext by inject()
    private val windowManager: WindowManager by inject()
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)
    private var overlayDisplayed = false
    private var overlayView: View? = null
    private val powerManager: PowerManager by inject()
    private var uri: String? = null
    private var pendingAutofillType: AutofillType = AutofillType.CREDENTIALS
    private val accessibilityServiceNotificationFactory: AccessibilityServiceNotificationFactory by inject()
    private var overlayViewHeight: Int = 0
    private var anchorNode: AccessibilityNodeInfo? = null
    private var isOverlayAboveAnchor: Boolean = false
    private var overlayAnchorObserverRunning = false
    private var overlayAnchorObserverRunnable: Job? = null
    private var clearLastFillJob: Job? = null
    private var lastAnchorX = 0
    private var lastAnchorY = 0
    private var lastPosition: OverlayPosition? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(
            ACCESSIBILITY_SERVICE_NOTIFICATION_ID,
            accessibilityServiceNotificationFactory.getAccessibilityServiceNotification(this),
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Timber.d("AccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!powerManager.isInteractive) {
            return
        }
        if (accessibilityOperationsProvider.shouldSkipPackage(event?.packageName?.toString())) {
            if (event?.packageName != SYSTEM_UI_PACKAGE) {
                hideOverlay()
            }
            return
        }
        when (event?.eventType) {
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            -> viewClicked(event)
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            -> stateChanged(event)
            else -> {
                // ignoring
            }
        }
    }

    private fun stateChanged(event: AccessibilityEvent) {
        val root = rootInActiveWindow
        if (accessibilityCommunicator.lastFill == null) {
            Timber.d("Last fill payload is null - ignoring event")
        } else if (event.source == null ||
            (event.packageName != null && event.packageName.contains(PASSBOLT_PACKAGE))
        ) {
            Timber.d("Event source is null or package is Passbolt - hiding overlay")
            hideOverlay()
        } else if (root == null || root.packageName != event.packageName) {
            Timber.d("Root is null or root package name is different than event - ignoring")
        } else if (scanAndAutofill(root, event)) {
            Timber.d("Scanning returned true - hiding overlay")
            hideOverlay()
        }
    }

    private fun viewClicked(event: AccessibilityEvent) {
        val root = rootInActiveWindow
        val source = event.source
        if (source == null ||
            (event.packageName != null && event.packageName.contains(PASSBOLT_PACKAGE))
        ) {
            Timber.d("Event source is null or package is Passbolt - hiding overlay")
            hideOverlay()
            return
        } else if (root == null || root.packageName != event.packageName) {
            Timber.d("Root is null or root package name is different than event - ignoring")
        } else if (!source.isPassword &&
            !accessibilityOperationsProvider.isUsernameEditText(root, event) &&
            !accessibilityOperationsProvider.isTotpEditText(root, event)
        ) {
            Timber.d("Field is not a password, username or totp - hiding overlay")
            hideOverlay()
        } else if (scanAndAutofill(root, event)) {
            Timber.d("Scanning returned true - hiding overlay")
            hideOverlay()
        } else {
            val classifiedType = classifyAutofillType(root, event)
            if (classifiedType == null) {
                Timber.d("No autofillable fields classified - hiding overlay")
                hideOverlay()
                return
            }
            Timber.d("View clicked else - displaying overlay for $classifiedType")
            uri = accessibilityOperationsProvider.getUri(root)
            pendingAutofillType = classifiedType
            if (Settings.canDrawOverlays(this)) {
                displayOverlay(source)
            } else {
                Timber.e(
                    "Permission to draw over other apps not granted " +
                        "- please grant in the system application settings",
                )
            }
        }
    }

    private fun classifyAutofillType(
        root: AccessibilityNodeInfo,
        event: AccessibilityEvent,
    ): AutofillType? = accessibilityOperationsProvider.classifyAutofillType(root, event)

    private fun scanAndAutofill(
        root: AccessibilityNodeInfo,
        event: AccessibilityEvent,
    ): Boolean {
        var filled = false
        val allEditTexts = accessibilityOperationsProvider.getAllNodes(root, event)
        val passwordEditText = accessibilityOperationsProvider.getPasswordNode(allEditTexts)
        val usernameEditText =
            accessibilityOperationsProvider.getUsernameNode(
                allEditTexts,
                passwordEditText?.viewIdResourceName,
            )
        val totpEditText = accessibilityOperationsProvider.getTotpNode(allEditTexts)
        val currentUri = accessibilityOperationsProvider.getUri(root)

        val pendingFill = accessibilityCommunicator.lastFill
        if (currentUri != null &&
            pendingFill != null &&
            accessibilityOperationsProvider.needToAutofill(pendingFill, currentUri)
        ) {
            filled = fillAvailable(pendingFill, usernameEditText, passwordEditText, totpEditText)
            if (filled) {
                accessibilityCommunicator.lastFill = null
                clearLastFillJob?.cancel()
                clearLastFillJob = null
            }
        }

        if (accessibilityCommunicator.lastFill != null) {
            clearLastFillJob?.cancel()
            clearLastFillJob =
                scope.launch {
                    delay(CLEAR_CREDENTIALS_DELAY)
                    accessibilityCommunicator.lastFill = null
                    clearLastFillJob = null
                }
        }
        return filled
    }

    private fun fillAvailable(
        payload: AccessibilityCommunicator.LastFill,
        usernameNode: AccessibilityNodeInfo?,
        passwordNode: AccessibilityNodeInfo?,
        totpNode: AccessibilityNodeInfo?,
    ): Boolean {
        var anyFilled = false
        payload.username?.let { value ->
            usernameNode?.let { node ->
                accessibilityOperationsProvider.fillNode(node, value)
                anyFilled = true
            }
        }
        payload.password?.let { value ->
            passwordNode?.let { node ->
                accessibilityOperationsProvider.fillNode(node, value)
                anyFilled = true
            }
        }
        payload.totpCode?.let { value ->
            totpNode?.let { node ->
                accessibilityOperationsProvider.fillNode(node, value)
                anyFilled = true
            }
        }
        return anyFilled
    }

    private fun openResourcesActivity() {
        startActivity(
            ActivityIntents
                .autofill(
                    context = applicationContext,
                    autofillModeName = AutofillMode.ACCESSIBILITY.name,
                    uri = uri,
                    autofillTypeName = pendingAutofillType.name,
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
        )
    }

    private fun displayOverlay(anchor: AccessibilityNodeInfo) {
        if (!overlayDisplayed) {
            overlayDisplayed = true
            scope.launch {
                val view =
                    composeAutofillLabelView(
                        context = applicationContext,
                        onClick = {
                            hideOverlay()
                            openResourcesActivity()
                        },
                        onClose = ::hideOverlay,
                    )
                if (!overlayDisplayed) return@launch
                overlayView = view

                val params = accessibilityOperationsProvider.createOverlayParams()
                view.measure(
                    View.MeasureSpec.makeMeasureSpec(0, 0),
                    View.MeasureSpec.makeMeasureSpec(0, 0),
                )
                overlayViewHeight = view.measuredHeight

                val anchorPosition =
                    accessibilityOperationsProvider.getOverlayAnchorPosition(
                        anchor,
                        overlayViewHeight,
                        isOverlayAboveAnchor,
                    )
                anchorNode = anchor

                params.x = anchorPosition.x
                params.y = anchorPosition.y
                windowManager.addView(view, params)
                startOverlayAnchorObserver()
            }
        }
    }

    private fun startOverlayAnchorObserver() {
        if (!overlayAnchorObserverRunning) {
            overlayAnchorObserverRunning = true
            overlayAnchorObserverRunnable =
                scope.launch {
                    while (overlayAnchorObserverRunning) {
                        delay(OBSERVE_POSITION_DELAY)
                        adjustOverlayForScroll()
                    }
                }
        }
    }

    private fun adjustOverlayForScroll() {
        if (overlayView == null || anchorNode == null) {
            hideOverlay()
            return
        }
        val root = rootInActiveWindow
        val anchorPosition =
            accessibilityOperationsProvider.getOverlayAnchorPosition(
                anchorNode,
                root,
                windows,
                overlayViewHeight,
                false,
            )
        val currentLastPosition = lastPosition
        if (anchorPosition == null) {
            hideOverlay()
        } else if (anchorPosition is OutBoundsHide) {
            if (overlayView?.visibility != View.GONE) {
                overlayView?.gone()
            }
        } else if (anchorPosition is InBoundsBottomAnchor || anchorPosition is ForceBottom) {
            isOverlayAboveAnchor = false
        } else if (anchorPosition is InBoundsTopAnchor) {
            isOverlayAboveAnchor = true
        } else if (currentLastPosition != null && anchorPosition::class == currentLastPosition::class) {
            if (overlayView?.visibility != View.VISIBLE) {
                overlayView?.visibility = View.VISIBLE
            }
        } else if (anchorPosition is OverlayPosition.Position) {
            updateOverlay(anchorPosition)
        }
    }

    private fun updateOverlay(anchorPosition: OverlayPosition.Position) {
        val layoutParams = accessibilityOperationsProvider.createOverlayParams()
        layoutParams.x = anchorPosition.x
        layoutParams.y = anchorPosition.y

        lastPosition = anchorPosition

        windowManager.updateViewLayout(overlayView, layoutParams)

        if (overlayView?.visibility != View.VISIBLE) {
            overlayView?.visible()
        }
    }

    private fun hideOverlay() {
        if (overlayDisplayed) {
            overlayView?.let { windowManager.removeViewImmediate(it) }
            overlayView = null
            overlayDisplayed = false
            lastAnchorX = 0
            lastAnchorY = 0
            isOverlayAboveAnchor = false
        }
    }

    override fun onInterrupt() {
        hideOverlay()
    }

    companion object {
        private val OBSERVE_POSITION_DELAY = 250L.milliseconds
        private val CLEAR_CREDENTIALS_DELAY = 1000L.milliseconds

        private const val PASSBOLT_PACKAGE = "com.passbolt.mobile.android"
        private const val SYSTEM_UI_PACKAGE = "com.android.systemui"
    }
}
