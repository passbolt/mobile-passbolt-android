package com.passbolt.mobile.android.core.navigation.compose.results

data class ResourceDetailsCompleteResult(
    val resourceEdited: Boolean,
    val resourceDeleted: Boolean,
    val resourceName: String?,
)

data class CreateFolderCompleteResult(
    val folderName: String,
)

data class PermissionsShareCompleteResult(
    val shared: Boolean,
)
