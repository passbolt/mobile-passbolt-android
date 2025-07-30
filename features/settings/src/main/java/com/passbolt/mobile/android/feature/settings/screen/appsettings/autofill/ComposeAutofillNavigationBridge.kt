package com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import com.passbolt.mobile.android.feature.autofill.enabled.AutofillEnabledDialog
import com.passbolt.mobile.android.feature.autofill.enabled.DialogMode
import com.passbolt.mobile.android.feature.autofill.encourage.accessibility.EncourageAccessibilityAutofillDialog
import com.passbolt.mobile.android.feature.autofill.encourage.autofill.EncourageAutofillServiceDialog

/**
 * Compose implementation of AuthenticationNavigation that works with FragmentManager
 * This allows displaying authentication dialogs from a Composable context
 */
class ComposeAutofillNavigationBridge(
    private val activity: FragmentActivity,
) : AutofillSettingsNavigation {
    override fun showEncourageNativeAutofillDialog() {
        val dialog = EncourageAutofillServiceDialog()

        dialog.show(
            activity.supportFragmentManager,
            EncourageAutofillServiceDialog::class.java.name,
        )

        dialog.listener =
            object : EncourageAutofillServiceDialog.Listener {
                override fun autofillSetupSuccessfully() {
                    showNativeAutofillEnabledDialog()
                }
            }
    }

    override fun showNativeAutofillEnabledDialog() {
        val dialog = AutofillEnabledDialog.newInstance(DialogMode.Settings)

        dialog.show(
            activity.supportFragmentManager,
            AutofillEnabledDialog::class.java.name,
        )

        dialog.listener = EmptyAutofillEnabledListener()
    }

    override fun showEncourageAccessibilityAutofillDialog() {
        val dialog = EncourageAccessibilityAutofillDialog()

        dialog.show(
            activity.supportFragmentManager,
            EncourageAccessibilityAutofillDialog::class.java.name,
        )

        dialog.listener = EmptyAccessibilityAutofillChangeListener()
    }

    companion object Companion {
        @Composable
        fun rememberAutofillNavigation(): ComposeAutofillNavigationBridge {
            val context = LocalContext.current
            val activity =
                context as? FragmentActivity
                    ?: error("Currently FragmentActivity is needed as parent to show MFA dialogs.")

            return ComposeAutofillNavigationBridge(activity = activity)
        }
    }
}
