package com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.summary

import com.passbolt.mobile.android.ui.TransferAccountStatusType

class TransferAccountStatusFactory {
    fun create(type: TransferAccountStatusType): TransferAccountStatus =
        when (type) {
            TransferAccountStatusType.SUCCESS -> TransferAccountStatus.Success()
            TransferAccountStatusType.FAILURE -> TransferAccountStatus.Failure()
            TransferAccountStatusType.CANCELED -> TransferAccountStatus.Canceled()
        }
}
