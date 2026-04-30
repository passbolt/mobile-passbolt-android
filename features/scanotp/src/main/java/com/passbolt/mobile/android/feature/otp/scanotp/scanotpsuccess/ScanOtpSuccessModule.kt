package com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel

fun Module.scanOtpSuccessModule() {
    viewModel { params ->
        ScanOtpSuccessViewModel(
            scannedTotp = params.get(),
            parentFolderId = params.getOrNull(),
            idToSlugMappingProvider = get(),
            getDefaultCreateContentTypeUseCase = get(),
            metadataPrivateKeysHelperInteractor = get(),
            resourceUpdateActionsInteractorFactory = get(),
        )
    }
}
