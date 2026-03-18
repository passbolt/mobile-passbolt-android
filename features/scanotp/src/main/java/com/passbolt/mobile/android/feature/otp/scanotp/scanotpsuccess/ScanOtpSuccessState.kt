package com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess

import com.passbolt.mobile.android.ui.NewMetadataKeyToTrustModel
import com.passbolt.mobile.android.ui.TrustedKeyDeletedModel

data class ScanOtpSuccessState(
    val showProgress: Boolean = false,
    val metadataKeyToTrust: NewMetadataKeyToTrustModel? = null,
    val metadataKeyDeleted: TrustedKeyDeletedModel? = null,
    val showNewMetadataTrustDialog: Boolean = false,
    val showTrustedMetadataKeyDeletedDialog: Boolean = false,
)
