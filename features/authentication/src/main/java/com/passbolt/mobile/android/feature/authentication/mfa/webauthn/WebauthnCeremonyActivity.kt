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

package com.passbolt.mobile.android.feature.authentication.mfa.webauthn

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.browser.auth.AuthTabIntent
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.WebauthnCeremonyResult.BrowserUnavailable
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.WebauthnCeremonyResult.Cancelled
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.WebauthnCeremonyResultContract.Companion.EXTRA_CEREMONY_URL
import timber.log.Timber

/**
 * Invisible intermediate activity that launches the passkey (webauthn) MFA ceremony in a browser and returns the
 * outcome as an activity result.
 *
 * Why an intermediate activity: Auth Tab (specialized Custom Tab) delivers the redirect URI back to the app on its own and
 * closes the tab, so that path works correct even without this activity. When Auth Tab not available, Custom Tabs and regular
 * browsers used as fallback do not do that: the redirect only reaches the app as a VIEW intent to an exported activity,
 * and the browser tab stays open on top of our MFA screen. Both browser paths therefore need one place
 * that owns the browser launch and receives the redirect, and this activity is that place. As an additioanl
 * benefit, being already in the back stack it can remove the browser tab via CLEAR_TOP regardless of
 * which activity hosts the MFA screen (sign-in, in-app MFA on session expiry).
 *
 * Flow:
 * 1. [AuthWithWebauthnScreen] launches this activity through [WebauthnCeremonyResultContract] with the
 *    ceremony page URL. The stack is now: host activity -> this activity.
 * 2. On first [onResume] this activity opens the browser, picking the first available option:
 *    a. Auth Tab, when the default browser supports it. Launched through [authTabLauncher];
 *       the browser closes the tab itself and delivers the redirect URI as the launcher result.
 *    b. Custom Tab, launched into this task. The stack is: host -> this activity -> browser tab.
 *    c. Plain ACTION_VIEW browser intent when no Custom Tabs provider exists.
 * 3. The MFA page completes the passkey ceremony with the server and navigates to
 *    `passbolt://mfa/webauthn/callback?mfa_delegated_token=...` (or `?error=...`).
 * 4. For (a) the Auth Tab result arrives in [authTabLauncher] and this activity finishes with it.
 *    For (b) and (c) the browser resolves the scheme to [WebauthnCallbackActivity], which restarts
 *    this activity with the redirect URI as data and CLEAR_TOP | SINGLE_TOP. That destroys the browser
 *    tab and the callback activity above and delivers the URI through [onNewIntent].
 * 5. On the following [onResume]: data present -> result parsed by [parseWebauthnCallbackUri];
 *    data absent -> the user backed out of the browser -> [WebauthnCeremonyResult.Cancelled].
 * 6. The result is encoded by [WebauthnCeremonyResultContract] and returned to the launching screen, which
 *    forwards it to [AuthWithWebauthnViewModel] as [AuthWithWebauthnIntent.WebauthnCeremonyFinished].
 *    The view model checks the delegated token locally, then trades it together with the delegated secret
 *    for the MFA token via the JWT-authenticated API.
 *
 * Hosts of the MFA screen, and therefore of this flow:
 * - Sign-in: the screen is a navigation entry inside AuthenticationMainActivity. The result returns to
 *   that activity, the view model completes MFA and sign-in continues in AuthViewModel.
 * - In-app session refresh: the screen is rendered by AuthenticationHandler inside MainActivity (or
 *   AutofillResourcesActivity). The result returns to that activity, the MFA token is saved and the
 *   pending session refresh is notified. No activity switch is involved.
 * In both cases this activity is transparent and never visible; it exists only to own the browser
 * launch and to receive the redirect.
 *
 * [isCeremonyStarted] is kept in instance state so that a process death while the browser is open
 * resolves to a result on the next resume instead of launching the browser again.
 */
class WebauthnCeremonyActivity : ComponentActivity() {
    private var isCeremonyStarted = false

    private val ceremonyUrl: Uri?
        get() = intent.getStringExtra(EXTRA_CEREMONY_URL)?.toUri()

    private val customTabsPackage: String? by lazy { CustomTabsClient.getPackageName(this, null) }

    private val isAuthTabSupported: Boolean by lazy {
        customTabsPackage?.let { CustomTabsClient.isAuthTabSupported(this, it) } ?: false
    }

    private val authTabLauncher =
        registerForActivityResult(AuthTabIntent.AuthenticateUserResultContract()) { result ->
            finishWithResult(result.toWebauthnCeremonyResult())
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCeremonyStarted = savedInstanceState?.getBoolean(STATE_CEREMONY_STARTED) ?: false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_CEREMONY_STARTED, isCeremonyStarted)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if (isFinishing) {
            return
        }
        if (!isCeremonyStarted) {
            isCeremonyStarted = true
            startCeremony()
        } else {
            val callbackUri = intent.data
            if (callbackUri != null) {
                finishWithResult(parseWebauthnCallbackUri(callbackUri.toString()))
            } else {
                Timber.d("Returned from browser without webauthn callback")
                finishWithResult(Cancelled)
            }
        }
    }

    private fun startCeremony() {
        val ceremonyUrl = ceremonyUrl
        if (ceremonyUrl == null) {
            Timber.e("Webauthn ceremony url is missing")
            finishWithResult(Cancelled)
            return
        }
        Timber.d(
            "Launching webauthn ceremony, custom tabs available: ${customTabsPackage != null}, " +
                "auth tab supported: $isAuthTabSupported",
        )
        try {
            when {
                isAuthTabSupported -> launchAuthTab(ceremonyUrl)
                customTabsPackage != null -> CustomTabsIntent.Builder().build().launchUrl(this, ceremonyUrl)
                else -> startActivity(Intent(Intent.ACTION_VIEW, ceremonyUrl))
            }
        } catch (_: ActivityNotFoundException) {
            finishWithResult(BrowserUnavailable)
        }
    }

    private fun launchAuthTab(ceremonyUrl: Uri) {
        AuthTabIntent
            .Builder()
            .build()
            .launch(authTabLauncher, ceremonyUrl, REDIRECT_SCHEME)
    }

    private fun finishWithResult(result: WebauthnCeremonyResult) {
        setResult(
            WebauthnCeremonyResultContract.resultCodeOf(result),
            WebauthnCeremonyResultContract.resultIntentOf(result),
        )
        finish()
    }

    private companion object {
        private const val REDIRECT_SCHEME = "passbolt"
        private const val STATE_CEREMONY_STARTED = "CEREMONY_STARTED"
    }
}
