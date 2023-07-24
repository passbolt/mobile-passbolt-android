package com.passbolt.mobile.android.feature.home.screen.model

data class HeaderSectionConfiguration(
    val isInCurrentFolderSectionVisible: Boolean,
    val isInSubFoldersSectionVisible: Boolean,
    val currentFolderName: String? = null,
    val isSuggestedSectionVisible: Boolean,
    val isOtherItemsSectionVisible: Boolean
)
