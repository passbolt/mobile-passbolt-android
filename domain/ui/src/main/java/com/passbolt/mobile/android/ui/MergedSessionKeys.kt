package com.passbolt.mobile.android.ui

import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentHashMap

data class MergedSessionKeys(
    val keys: ConcurrentHashMap<SessionKeyIdentifier, SessionKeyModel> = ConcurrentHashMap(),
    val originMetadata: ConcurrentHashMap<String, ZonedDateTime> = ConcurrentHashMap()
)
