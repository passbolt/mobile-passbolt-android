package com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess

import com.google.gson.Gson
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.spi.json.GsonJsonProvider
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider
import com.passbolt.mobile.android.core.mvp.authentication.SessionRefreshTrackingFlow
import com.passbolt.mobile.android.core.resources.actions.ResourceCreateActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.ResourceUpdateActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.ResourceUpdateActionsInteractorFactory
import com.passbolt.mobile.android.core.resources.usecase.GetDefaultCreateContentTypeUseCase
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.ResourceTypeIdToSlugMappingProvider
import com.passbolt.mobile.android.jsonmodel.JSON_MODEL_GSON
import com.passbolt.mobile.android.jsonmodel.jsonpathops.JsonPathJsonPathOps
import com.passbolt.mobile.android.jsonmodel.jsonpathops.JsonPathsOps
import com.passbolt.mobile.android.metadata.interactor.MetadataPrivateKeysHelperInteractor
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import org.mockito.kotlin.mock
import java.util.EnumSet

internal val mockIdToSlugMappingProvider = mock<ResourceTypeIdToSlugMappingProvider>()
internal val mockResourceCreateActionsInteractor = mock<ResourceCreateActionsInteractor>()
internal val mockResourceUpdateActionsInteractor = mock<ResourceUpdateActionsInteractor>()
internal val mockResourceUpdateActionsInteractorFactory = ResourceUpdateActionsInteractorFactory { mockResourceUpdateActionsInteractor }
internal val mockGetDefaultCreateContentTypeUseCase = mock<GetDefaultCreateContentTypeUseCase>()
internal val mockMetadataPrivateKeysHelperInteractor = mock<MetadataPrivateKeysHelperInteractor>()

internal val testScanOtpSuccessModule =
    module {
        single { mockResourceCreateActionsInteractor }
        single<ResourceUpdateActionsInteractorFactory> { mockResourceUpdateActionsInteractorFactory }
        single { mockIdToSlugMappingProvider }
        single { mockGetDefaultCreateContentTypeUseCase }
        single { mockMetadataPrivateKeysHelperInteractor }
        singleOf(::SessionRefreshTrackingFlow)
        factory { params ->
            ScanOtpSuccessViewModel(
                scannedTotp = params.get(),
                parentFolderId = params.getOrNull(),
                idToSlugMappingProvider = get(),
                getDefaultCreateContentTypeUseCase = get(),
                metadataPrivateKeysHelperInteractor = get(),
                resourceUpdateActionsInteractorFactory = get(),
            )
        }
        single(named(JSON_MODEL_GSON)) { Gson() }
        single {
            Configuration
                .builder()
                .jsonProvider(GsonJsonProvider())
                .mappingProvider(GsonMappingProvider())
                .options(EnumSet.noneOf(Option::class.java))
                .build()
        }
        singleOf(::JsonPathJsonPathOps) bind JsonPathsOps::class
    }
