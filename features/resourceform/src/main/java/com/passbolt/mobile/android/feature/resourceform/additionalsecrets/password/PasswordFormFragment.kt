package com.passbolt.mobile.android.feature.resourceform.additionalsecrets.password

import PassboltTheme
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.passbolt.mobile.android.common.dialogs.unableToGeneratePasswordAlertDialog
import com.passbolt.mobile.android.ui.PasswordUiModel

class PasswordFormFragment :
    Fragment(),
    PasswordFormNavigation {
    private val navArgs: PasswordFormFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setContent {
                PassboltTheme {
                    PasswordFormScreen(
                        mode = navArgs.mode,
                        passwordModel = navArgs.passwordModel,
                        navigation = this@PasswordFormFragment,
                    )
                }
            }
        }

    override fun navigateBack() {
        findNavController().popBackStack()
    }

    override fun navigateBackWithResult(model: PasswordUiModel) {
        setFragmentResult(
            REQUEST_PASSWORD,
            bundleOf(EXTRA_PASSWORD to model),
        )
        findNavController().popBackStack()
    }

    override fun showUnableToGeneratePassword(minimumEntropyBits: Int) {
        unableToGeneratePasswordAlertDialog(requireContext(), minimumEntropyBits).show()
    }

    companion object {
        const val REQUEST_PASSWORD = "PASSWORD"

        const val EXTRA_PASSWORD = "password"
    }
}
