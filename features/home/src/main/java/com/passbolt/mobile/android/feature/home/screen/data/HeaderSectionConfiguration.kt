package com.passbolt.mobile.android.feature.home.screen.data

data class HeaderSectionConfiguration(
    val isInCurrentFolderSectionVisible: Boolean = false,
    val isInSubFoldersSectionVisible: Boolean = false,
    val currentFolderName: String? = null,
    val isSuggestedSectionVisible: Boolean = false,
    val isOtherItemsSectionVisible: Boolean = false,
)
