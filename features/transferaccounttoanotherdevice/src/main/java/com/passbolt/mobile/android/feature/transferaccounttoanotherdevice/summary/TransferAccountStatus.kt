package com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.summary

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import java.io.Serializable
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

sealed class TransferAccountStatus(
    @DrawableRes val icon: Int,
    @StringRes val title: Int,
    @StringRes val buttonText: Int,
) : Serializable {
    class Success :
        TransferAccountStatus(
            CoreUiR.drawable.ic_success,
            LocalizationR.string.transfer_account_summary_success,
            LocalizationR.string.transfer_account_summary_go_back,
        )

    class Failure :
        TransferAccountStatus(
            CoreUiR.drawable.ic_failed,
            LocalizationR.string.common_failure,
            LocalizationR.string.transfer_account_summary_go_back,
        )

    class Canceled :
        TransferAccountStatus(
            CoreUiR.drawable.ic_failed,
            LocalizationR.string.transfer_account_summary_cancelled,
            LocalizationR.string.transfer_account_summary_go_back,
        )
}
