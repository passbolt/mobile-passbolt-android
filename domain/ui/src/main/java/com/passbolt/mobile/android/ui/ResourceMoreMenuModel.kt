package com.passbolt.mobile.android.ui

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class ResourceMoreMenuModel(
    val title: String,
    val canDelete: Boolean
) : Parcelable
