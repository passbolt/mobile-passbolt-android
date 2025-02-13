package com.passbolt.mobile.android.ui

import java.time.ZonedDateTime

data class MergedSessionKeys(
    val keys: HashMap<SessionKeyIdentifier, SessionKeyModel> = hashMapOf(),
    val originMetadata: HashMap<String, ZonedDateTime> = hashMapOf()
)
