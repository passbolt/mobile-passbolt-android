package com.passbolt.mobile.android.ui

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class OtpMoreMenuModel(
    val title: String,
    val canDelete: Boolean,
    val canEdit: Boolean,
    val canShow: Boolean
) : Parcelable
