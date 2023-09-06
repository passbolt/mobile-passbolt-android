package com.passbolt.mobile.android.feature.home.screen.model

enum class State(
    val errorVisible: Boolean,
    val emptyVisible: Boolean,
    val listVisible: Boolean
) {
    EMPTY(false, true, false),
    SEARCH_EMPTY(false, true, false),
    ERROR(true, false, false),
    SUCCESS(false, false, true)
}
