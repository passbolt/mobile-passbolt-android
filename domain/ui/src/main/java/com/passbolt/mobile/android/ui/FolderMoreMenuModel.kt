package com.passbolt.mobile.android.ui

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class FolderMoreMenuModel(
    val folderName: String?,
) : Parcelable
