package com.passbolt.mobile.android.feature.authentication.mfa.totp.compose

fun interface PinInputSanitizer {
    fun sanitize(input: String): String
}

class DigitsOnlySanitizer(
    private val maxLength: Int,
) : PinInputSanitizer {
    override fun sanitize(input: String): String = input.filter { it.isDigit() }.take(maxLength)
}
