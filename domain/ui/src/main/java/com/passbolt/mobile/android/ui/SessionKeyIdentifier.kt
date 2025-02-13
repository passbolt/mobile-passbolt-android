package com.passbolt.mobile.android.ui

import java.util.UUID

data class SessionKeyIdentifier(
    val foreignModel: String,
    val foreignId: UUID
)
