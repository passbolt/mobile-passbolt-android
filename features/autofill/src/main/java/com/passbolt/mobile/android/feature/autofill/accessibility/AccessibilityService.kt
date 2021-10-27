package com.passbolt.mobile.android.feature.autofill.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Point
import android.os.PowerManager
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.passbolt.mobile.android.common.extension.gone
import com.passbolt.mobile.android.common.extension.setDebouncingOnClick
import com.passbolt.mobile.android.common.extension.visible
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.feature.autofill.accessibility.notification.AccessibilityServiceNotificationFactory
import com.passbolt.mobile.android.feature.autofill.databinding.ViewAutofillLabelBinding
import com.passbolt.mobile.android.feature.autofill.resources.AutofillResourcesActivity
import com.passbolt.mobile.android.feature.autofill.resources.AutofillResourcesActivity.Companion.URI_KEY
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

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
class AccessibilityService : AccessibilityService(), KoinComponent {

    private val accessibilityOperationsProvider: AccessibilityOperationsProvider by inject()
    private val coroutineLaunchContext: CoroutineLaunchContext by inject()
    private val windowManager: WindowManager by inject()
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)
    private var overlayDisplayed = false
    private var overlayView: ViewAutofillLabelBinding? = null
    private val powerManager: PowerManager by inject()
    private var uri: String? = null
    private val accessibilityServiceNotificationFactory: AccessibilityServiceNotificationFactory by inject()
    private var overlayViewHeight: Int = 0
    private var anchorNode: AccessibilityNodeInfo? = null
    private var isOverlayAboveAnchor: Boolean = false
    private var overlayAnchorObserverRunning = false
    private var overlayAnchorObserverRunnable: Job? = null
    private var lastAnchorX = 0
    private var lastAnchorY = 0

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, accessibilityServiceNotificationFactory.getNotification(this))
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        createOverlayView()
        Timber.d("AccessibilityService connected")
    }

    private fun createOverlayView() {
        overlayView = ViewAutofillLabelBinding.inflate(LayoutInflater.from(applicationContext))
        overlayView?.root?.setDebouncingOnClick {
            hideOverlay()
            openResourcesActivity()
        }
        overlayView?.close?.setDebouncingOnClick {
            hideOverlay()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!powerManager.isInteractive) {
            return
        }
        if (accessibilityOperationsProvider.shouldSkipPackage(event?.packageName.toString())) {
            if (event?.packageName != SYSTEM_UI_PACKAGE) {
                hideOverlay()
            }
            return
        }
        when (event?.eventType) {
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_VIEW_CLICKED -> viewClicked(event)
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> stateChanged(event)
            else -> {
                // ignoring
            }
        }
    }

    private fun stateChanged(event: AccessibilityEvent) {
        val root = rootInActiveWindow
        if (AccessibilityCommunicator.lastCredentials == null) {
            Timber.d("Last credentials are null - ignoring event")
        } else if (event.source == null || event.packageName == PASSBOLT_PACKAGE) {
            Timber.d("Event source is null or package is Passbolt - hiding overlay")
            hideOverlay()
        } else if (root == null || root.packageName != event.packageName) {
            Timber.d("Root is null or root package name is different than event - ignoring")
        } else if (scanAndAutofill(root, event)) {
            Timber.d("Scanning return false - hiding overlay")
            hideOverlay()
        }
    }

    private fun viewClicked(event: AccessibilityEvent) {
        val root = rootInActiveWindow
        if (event.source == null || event.packageName == PASSBOLT_PACKAGE) {
            Timber.d("Event source is null or package is Passbolt - hiding overlay")
            hideOverlay()
            return
        } else if (root == null || root.packageName != event.packageName) {
            Timber.d("Root is null or root package name is different than event - ignoring")
        } else if (event.source?.isPassword != true &&
            !accessibilityOperationsProvider.isUsernameEditText(root, event)
        ) {
            Timber.d("Field is not a password or username - hiding overlay")
            hideOverlay()
        } else if (scanAndAutofill(root, event)) {
            Timber.d("Scanning return false - hiding overlay")
            hideOverlay()
        } else {
            Timber.d("View clicked else - displaying overlay")
            uri = accessibilityOperationsProvider.getUri(root)
            displayOverlay(event)
        }
    }

    private fun scanAndAutofill(root: AccessibilityNodeInfo, event: AccessibilityEvent): Boolean {
        var filled = false
        val allEditTexts = accessibilityOperationsProvider.getAllNodes(root, event)
        val passwordEditText = accessibilityOperationsProvider.getPasswordNode(allEditTexts)
        val usernameEditText = accessibilityOperationsProvider.getUsernameNode(
            allEditTexts,
            passwordEditText?.viewIdResourceName
        )
        val uri = accessibilityOperationsProvider.getUri(root)

        if (uri != null && usernameEditText != null && passwordEditText != null &&
            accessibilityOperationsProvider.needToAutofill(AccessibilityCommunicator.lastCredentials, uri)
        ) {
            fillUsernameField(usernameEditText)
            fillPasswordField(passwordEditText)
            filled = true
            AccessibilityCommunicator.lastCredentials = null
        }

        if (AccessibilityCommunicator.lastCredentials != null) {
            scope.launch {
                delay(CLEAR_CREDENTIALS_DELAY)
                AccessibilityCommunicator.lastCredentials = null
            }
        }
        return filled
    }

    private fun fillUsernameField(usernameEditText: AccessibilityNodeInfo) {
        accessibilityOperationsProvider.fillNode(
            usernameEditText,
            AccessibilityCommunicator.lastCredentials!!.username
        )
    }

    private fun fillPasswordField(passwordEditText: AccessibilityNodeInfo) {
        accessibilityOperationsProvider.fillNode(
            passwordEditText,
            AccessibilityCommunicator.lastCredentials!!.password
        )
    }

    private fun openResourcesActivity() {
        val intent = Intent(applicationContext, AutofillResourcesActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(URI_KEY, uri)
        }
        startActivity(intent)
    }

    private fun displayOverlay(event: AccessibilityEvent) {
        if (!overlayDisplayed) {
            overlayDisplayed = true
            createOverlayView()
            val params = accessibilityOperationsProvider.createOverlayParams()
            overlayView?.root?.measure(View.MeasureSpec.makeMeasureSpec(0, 0), View.MeasureSpec.makeMeasureSpec(0, 0))
            overlayViewHeight = overlayView?.root?.measuredHeight ?: 0

            val anchorPosition = accessibilityOperationsProvider.getOverlayAnchorPosition(
                event.source, overlayViewHeight, isOverlayAboveAnchor
            )
            anchorNode = event.source

            params.x = anchorPosition.x
            params.y = anchorPosition.y
            windowManager.addView(overlayView?.root, params)
            startOverlayAnchorObserver()
        }
    }

    private fun startOverlayAnchorObserver() {
        if (!overlayAnchorObserverRunning) {
            overlayAnchorObserverRunning = true
            overlayAnchorObserverRunnable = scope.launch {
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
        val anchorPosition = accessibilityOperationsProvider.getOverlayAnchorPosition(
            anchorNode, root, windows, overlayViewHeight, false
        )
        if (anchorPosition == null) {
            hideOverlay()
        } else if (anchorPosition.x == -1 && anchorPosition.y == -1) {
            if (overlayView?.root?.visibility != View.GONE) {
                overlayView?.root?.gone()
            }
        } else if (anchorPosition.x == -1) {
            isOverlayAboveAnchor = false
        } else if (anchorPosition.y == -1) {
            isOverlayAboveAnchor = true
        } else if (anchorPosition.x == lastAnchorX && anchorPosition.y == lastAnchorY) {
            if (overlayView?.root?.visibility != View.VISIBLE) {
                overlayView?.root?.visibility = View.VISIBLE
            }
        } else {
            updateOverlay(anchorPosition)
        }
    }

    private fun updateOverlay(anchorPosition: Point) {
        val layoutParams = accessibilityOperationsProvider.createOverlayParams()
        layoutParams.x = anchorPosition.x
        layoutParams.y = anchorPosition.y

        lastAnchorX = anchorPosition.x
        lastAnchorY = anchorPosition.y

        windowManager.updateViewLayout(overlayView?.root, layoutParams)

        if (overlayView?.root?.visibility != View.VISIBLE) {
            overlayView?.root?.visible()
        }
    }

    private fun hideOverlay() {
        if (overlayDisplayed) {
            windowManager.removeViewImmediate(overlayView?.root)
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
        private const val PASSBOLT_PACKAGE = "com.passbolt.mobile.android.debug"
        private const val SYSTEM_UI_PACKAGE = "com.android.systemui"
        private const val CLEAR_CREDENTIALS_DELAY = 1000L
        private const val OBSERVE_POSITION_DELAY = 250L
        private const val NOTIFICATION_ID = 1
    }
}
