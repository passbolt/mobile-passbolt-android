package com.passbolt.mobile.android.feature.autofill.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.PixelFormat
import android.os.PowerManager
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.cardview.widget.CardView
import com.passbolt.mobile.android.common.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.feature.autofill.R
import com.passbolt.mobile.android.feature.autofill.accessibility.notification.AccessibilityServiceNotificationFactory
import com.passbolt.mobile.android.feature.autofill.resources.AutofillResourcesActivity
import com.passbolt.mobile.android.feature.autofill.resources.AutofillResourcesActivity.Companion.URI_KEY
import kotlinx.coroutines.CoroutineScope
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
    private var focusEnabled = true
    private var overlayDisplayed = false
    private var filled = false
    private var overlayView: View? = null
    private val powerManager: PowerManager by inject()
    private var uri: String? = null
    private val accessibilityServiceNotificationFactory: AccessibilityServiceNotificationFactory by inject()

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
        val inflater = applicationContext.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        overlayView = inflater?.inflate(R.layout.view_autofill_label, null) as CardView
        overlayView?.setDebouncingOnClick {
            focusEnabled = false
            hideOverlay()
            openResourcesActivity()
        }
        overlayView?.findViewById<View>(R.id.close)?.setDebouncingOnClick {
            hideOverlay()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!powerManager.isInteractive) {
            return
        }
        if (accessibilityOperationsProvider.skipPackage(event?.packageName.toString())) {
            if (event?.packageName != "com.android.systemui") {
                hideOverlay()
            }
            return
        }
        when (event?.eventType) {
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                if (event.source == null || event.packageName == PASSBOLT_PACKAGE) {
                    Timber.d("DDD CLICKED event.source == null || event.packageName == PASSBOLT_PACKAGE")
                    hideOverlay()
                    return
                }
                val root = rootInActiveWindow
                if (root == null || root.packageName != event.packageName) {
                    Timber.d("DDD CLICKED root == null || root.packageName != event.packageName")
                    return
                } else if (event.source?.isPassword != true &&
                    !accessibilityOperationsProvider.isUsernameEditText(root, event)
                ) {
                    Timber.d("DDD CLICKED event.source?.isPassword != true && !accessibilityOperationsProvider.isUsernameEditText(root, event)")
                    hideOverlay();
                    return;
                } else if (scanAndAutofill(root, event)) {
                    Timber.d("DDD CLICKED scanAndAutofill(root, event)")
                    hideOverlay()
                    return
                } else {
                    Timber.d("DDD CLICKED else")
                    uri = accessibilityOperationsProvider.getUri(root)
                    displayOverlay(event)
                }
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val root = rootInActiveWindow
                if (AccessibilityCommunicator.lastCredentials == null) {
                    Timber.d("DDD lastCredentials == null")
                    return
                }
                if (event.source == null || event.packageName == PASSBOLT_PACKAGE) {
                    Timber.d("DDD event.source == null || event.packageName == PASSBOLT_PACKAGE")
                    hideOverlay()
                    return
                } else if (root == null || root.packageName != event.packageName) {
                    Timber.d("DDD root == null || root.packageName != event.packageName")
                    return
                } else if (scanAndAutofill(root, event)) {
                    Timber.d("DDD scanAndAutofill(root, event)")
                    hideOverlay()
                    return
                }
            }
        }
    }

    private fun scanAndAutofill(root: AccessibilityNodeInfo, event: AccessibilityEvent): Boolean {
        Timber.d("DDD scanAndAutofill")
        var filled = false
        val allEditTexts = accessibilityOperationsProvider.getAllNodes(root, event)
        val passwordNodes = accessibilityOperationsProvider.getPasswordNode(allEditTexts)
        val usernameEditText =
            accessibilityOperationsProvider.getUsernameNode(allEditTexts, passwordNodes?.viewIdResourceName)
        Timber.d("DDD passwordNodes: $passwordNodes")
        Timber.d("DDD usernameEditText: $usernameEditText")
        val uri = accessibilityOperationsProvider.getUri(root)
        Timber.d("DDD uri: $uri")
        Timber.d(
            "DDD need to autofill: ${
                accessibilityOperationsProvider.needToAutofill(
                    AccessibilityCommunicator.lastCredentials, uri ?: ""
                )
            }"
        )


        if (uri != null && usernameEditText != null && passwordNodes != null && accessibilityOperationsProvider.needToAutofill(
                AccessibilityCommunicator.lastCredentials, uri
            )
        ) {
            accessibilityOperationsProvider.fillEditText(
                usernameEditText,
                AccessibilityCommunicator.lastCredentials!!.username
            )
            accessibilityOperationsProvider.fillEditText(
                passwordNodes,
                AccessibilityCommunicator.lastCredentials!!.password
            )
            filled = true
            AccessibilityCommunicator.lastCredentials = null
        }
        if (AccessibilityCommunicator.lastCredentials != null) {
            scope.launch {
                delay(1000)
                AccessibilityCommunicator.lastCredentials = null
            }
        }
        return filled
    }

    private fun openResourcesActivity() {
        val intent = Intent(applicationContext, AutofillResourcesActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(URI_KEY, uri)
        }
        startActivity(intent)
    }

    private fun displayOverlay(event: AccessibilityEvent) {
        Timber.d("DDD displayOverlay: $overlayDisplayed")
        if (!overlayDisplayed) {
            overlayDisplayed = true
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, PixelFormat.TRANSPARENT
            )
            overlayView?.measure(View.MeasureSpec.makeMeasureSpec(0, 0), View.MeasureSpec.makeMeasureSpec(0, 0))
            val height = overlayView?.measuredHeight ?: 0
            val width = overlayView?.measuredWidth ?: 0
            val anchorPosition = accessibilityOperationsProvider.getOverlayAnchorPosition(
                event.source, height, width
            )
            params.x = anchorPosition.x
            params.y = anchorPosition.y
            windowManager.addView(overlayView, params)
        }
    }

    private fun hideOverlay() {
        Timber.d("DDD hideOverlay")
        filled = false
        if (overlayDisplayed) {
            windowManager.removeViewImmediate(overlayView)
            overlayDisplayed = false
        }
    }

    override fun onInterrupt() {
        hideOverlay()
    }

    companion object {
        private const val PASSBOLT_PACKAGE = "com.passbolt.mobile.android.debug"
        private const val NOTIFICATION_ID = 1
    }
}
