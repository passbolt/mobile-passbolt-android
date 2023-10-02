package com.passbolt.mobile.android.feature.home.screen.model

enum class State(
    val emptyVisible: Boolean,
    val listVisible: Boolean
) {
    EMPTY(true, false),
    SEARCH_EMPTY(true, false),
    SUCCESS(false, true)
}
