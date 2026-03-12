package com.passbolt.mobile.android.core.ui.header

sealed class ValueStyle {
    data object Plain : ValueStyle()

    data class Secret(
        val differentiateCharacters: Boolean = false,
    ) : ValueStyle()

    data object Linkified : ValueStyle()

    data object Concealed : ValueStyle()
}
