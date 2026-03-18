package com.passbolt.mobile.android.feature.otp.scanotp

import com.passbolt.mobile.android.core.qrscan.CameraInformationProvider
import com.passbolt.mobile.android.core.qrscan.analyzer.BarcodeScanResult
import com.passbolt.mobile.android.feature.otp.scanotp.compose.ScanOtpViewModel
import com.passbolt.mobile.android.feature.otp.scanotp.parser.OtpQrParser
import com.passbolt.mobile.android.ui.OtpParseResult
import com.passbolt.mobile.android.ui.OtpParseResult.UserResolvableError.ErrorType.NO_BARCODES_IN_RANGE
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import org.mockito.kotlin.mock

internal val cameraInformationProvider = mock<CameraInformationProvider>()
internal val qrParser = mock<OtpQrParser>()
internal val scanningFlow = MutableStateFlow<BarcodeScanResult>(BarcodeScanResult.NoBarcodeInRange)
internal val parseFlow = MutableStateFlow<OtpParseResult>(OtpParseResult.UserResolvableError(NO_BARCODES_IN_RANGE))

val scanOtpTestModule =
    module {
        single { cameraInformationProvider }
        single { qrParser }
        factoryOf(::ScanOtpViewModel)
    }
