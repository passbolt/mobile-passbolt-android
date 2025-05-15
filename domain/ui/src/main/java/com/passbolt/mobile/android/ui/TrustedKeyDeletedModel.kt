package com.passbolt.mobile.android.ui

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TrustedKeyDeletedModel(
    val keyFingerprint: String,
    val signedUsername: String,
    val signedName: String,
    val modificationKind: MetadataKeyModification = MetadataKeyModification.DELETION
) : Parcelable
