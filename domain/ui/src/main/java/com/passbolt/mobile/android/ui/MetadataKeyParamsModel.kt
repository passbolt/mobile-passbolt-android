package com.passbolt.mobile.android.ui

sealed class MetadataKeyParamsModel {
    data object ErrorDuringVerification : MetadataKeyParamsModel()

    data class NewMetadataKeyToTrust(
        val newMetadataKeyToTrust: NewMetadataKeyToTrustModel,
    ) : MetadataKeyParamsModel()

    data class TrustedKeyDeleted(
        val trustedKeyDeleted: TrustedKeyDeletedModel,
    ) : MetadataKeyParamsModel()

    data class ParamsModel(
        val metadataKeyId: String?,
        val metadataKeyType: MetadataKeyTypeModel,
    ) : MetadataKeyParamsModel()
}
