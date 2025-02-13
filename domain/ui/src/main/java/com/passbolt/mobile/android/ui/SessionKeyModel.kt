package com.passbolt.mobile.android.ui

import java.time.ZonedDateTime

data class SessionKeyModel(
    val sessionKey: String,
    val modified: ZonedDateTime
)
