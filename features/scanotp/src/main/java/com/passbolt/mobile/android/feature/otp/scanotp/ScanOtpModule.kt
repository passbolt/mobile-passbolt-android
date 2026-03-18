package com.passbolt.mobile.android.feature.otp.scanotp

import com.passbolt.mobile.android.feature.otp.scanotp.compose.ScanOtpViewModel
import com.passbolt.mobile.android.feature.otp.scanotp.parser.OtpQrParser
import com.passbolt.mobile.android.feature.otp.scanotp.parser.OtpQrScanResultsMapper
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf

fun Module.scanOtpModule() {
    factoryOf(::OtpQrParser)
    factoryOf(::OtpQrScanResultsMapper)
    viewModelOf(::ScanOtpViewModel)
}
