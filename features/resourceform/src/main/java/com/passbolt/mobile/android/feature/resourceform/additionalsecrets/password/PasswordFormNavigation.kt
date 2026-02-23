package com.passbolt.mobile.android.feature.resourceform.additionalsecrets.password

import com.passbolt.mobile.android.ui.PasswordUiModel

internal interface PasswordFormNavigation {
    fun navigateBack()

    fun navigateBackWithResult(model: PasswordUiModel)

    fun showUnableToGeneratePassword(minimumEntropyBits: Int)
}
