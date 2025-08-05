package com.passbolt.mobile.android.feature.accountdetails.screen

sealed class AccountDetailsValidationError {
    data class MaxLengthExceeded(
        val maxLength: Int,
    ) : AccountDetailsValidationError()
}
