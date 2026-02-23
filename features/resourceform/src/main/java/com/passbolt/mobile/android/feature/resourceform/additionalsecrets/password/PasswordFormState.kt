package com.passbolt.mobile.android.feature.resourceform.additionalsecrets.password

import com.passbolt.mobile.android.ui.PasswordStrength
import com.passbolt.mobile.android.ui.ResourceFormMode

internal data class PasswordFormState(
    val resourceFormMode: ResourceFormMode? = null,
    val password: String = "",
    val passwordStrength: PasswordStrength = PasswordStrength.Empty,
    val entropy: Double = 0.0,
    val mainUri: String = "",
    val username: String = "",
)
