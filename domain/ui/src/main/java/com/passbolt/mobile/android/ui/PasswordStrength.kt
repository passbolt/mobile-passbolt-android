package com.passbolt.mobile.android.ui

@Suppress("MagicNumber")
sealed class PasswordStrength(val progress: Int) {

    data object Empty : PasswordStrength(progress = 0)

    data object VeryWeak : PasswordStrength(progress = 20)

    data object Weak : PasswordStrength(progress = 40)

    data object Fair : PasswordStrength(progress = 60)

    data object Strong : PasswordStrength(progress = 80)

    data object VeryStrong : PasswordStrength(progress = 100)
}
