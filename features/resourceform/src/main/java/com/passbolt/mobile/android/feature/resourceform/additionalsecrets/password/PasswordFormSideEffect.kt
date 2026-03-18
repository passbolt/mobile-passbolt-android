package com.passbolt.mobile.android.feature.resourceform.additionalsecrets.password

import com.passbolt.mobile.android.ui.PasswordUiModel

internal sealed interface PasswordFormSideEffect {
    data object NavigateBack : PasswordFormSideEffect

    data class ApplyAndGoBack(
        val model: PasswordUiModel,
    ) : PasswordFormSideEffect

    data class ShowUnableToGeneratePassword(
        val minimumEntropyBits: Int,
    ) : PasswordFormSideEffect
}
