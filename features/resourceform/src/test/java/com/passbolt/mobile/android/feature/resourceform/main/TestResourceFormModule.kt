package com.passbolt.mobile.android.feature.resourceform.main

import com.google.gson.Gson
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.spi.json.GsonJsonProvider
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider
import com.passbolt.mobile.android.common.datarefresh.DataRefreshTrackingFlow
import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.core.idlingresource.CreateResourceIdlingResource
import com.passbolt.mobile.android.core.idlingresource.UpdateResourceIdlingResource
import com.passbolt.mobile.android.core.mvp.authentication.SessionRefreshTrackingFlow
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.passphrasememorycache.PassphraseMemoryCache
import com.passbolt.mobile.android.core.passwordgenerator.PinCodeGenerator
import com.passbolt.mobile.android.core.passwordgenerator.SecretGenerator
import com.passbolt.mobile.android.core.passwordgenerator.entropy.EntropyCalculator
import com.passbolt.mobile.android.core.passwordgenerator.usecase.CheckPasswordPropertiesUseCase
import com.passbolt.mobile.android.core.resourcetypes.graph.redesigned.ResourceTypesUpdatesAdjacencyGraph
import com.passbolt.mobile.android.domain.metadata.interactor.MetadataPrivateKeysHelperInteractor
import com.passbolt.mobile.android.domain.metadata.usecase.GetMetadataTypesSettingsUseCase
import com.passbolt.mobile.android.domain.passwordexpiry.usecase.PasswordExpiryPoliciesInteractor
import com.passbolt.mobile.android.domain.passwordpolicies.usecase.GetPasswordPoliciesUseCase
import com.passbolt.mobile.android.domain.passwordpolicies.usecase.PasswordPoliciesInteractor
import com.passbolt.mobile.android.domain.resources.actions.ResourceCreateActionsInteractor
import com.passbolt.mobile.android.domain.resources.actions.ResourceUpdateActionsInteractorFactory
import com.passbolt.mobile.android.domain.resources.actions.SecretPropertiesActionsInteractorFactory
import com.passbolt.mobile.android.domain.resources.usecase.GetDefaultCreateContentTypeUseCase
import com.passbolt.mobile.android.domain.resources.usecase.GetEditContentTypeUseCase
import com.passbolt.mobile.android.domain.resources.usecase.db.GetLocalResourceUseCase
import com.passbolt.mobile.android.domain.secrets.offline.OfflineSessionState
import com.passbolt.mobile.android.entity.featureflags.FeatureFlagsModel
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetSessionExpiryUseCase
import com.passbolt.mobile.android.featureflags.usecase.GetFeatureFlagsUseCase
import com.passbolt.mobile.android.jsonmodel.JSON_MODEL_GSON
import com.passbolt.mobile.android.jsonmodel.jsonpathops.JsonPathJsonPathOps
import com.passbolt.mobile.android.jsonmodel.jsonpathops.JsonPathsOps
import com.passbolt.mobile.android.mappers.ResourceFormMapper
import com.passbolt.mobile.android.ui.MetadataTypeModel.V4
import com.passbolt.mobile.android.ui.MetadataTypesSettingsModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import java.util.EnumSet

/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021 Passbolt SA
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * The name "Passbolt" is a registered trademark of Passbolt SA, and Passbolt SA hereby declines to grant a trademark
 * license to "Passbolt" pursuant to the GNU Affero General Public License version 3 Section 7(e), without a separate
 * agreement with Passbolt SA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Passbolt SA (https://www.passbolt.com)
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://www.passbolt.com Passbolt (tm)
 * @since v1.0
 */

internal val DEFAULT_TEST_FEATURE_FLAGS =
    FeatureFlagsModel(
        privacyPolicyUrl = null,
        termsAndConditionsUrl = null,
        isPreviewPasswordAvailable = false,
        areFoldersAvailable = false,
        areTagsAvailable = false,
        isTotpAvailable = false,
        isRbacAvailable = false,
        isPasswordExpiryAvailable = false,
        arePasswordPoliciesAvailable = false,
        canUpdatePasswordPolicies = false,
        isV5MetadataAvailable = false,
    )

internal val mockGetPasswordPoliciesUseCase = mock<GetPasswordPoliciesUseCase>()
internal val mockPasswordPoliciesInteractor = mock<PasswordPoliciesInteractor>()
internal val mockPasswordExpiryPoliciesInteractor = mock<PasswordExpiryPoliciesInteractor>()
internal val mockGetFeatureFlagsUseCase =
    mock<GetFeatureFlagsUseCase>().apply {
        stub { onBlocking { execute(Unit) } doReturn GetFeatureFlagsUseCase.Output(DEFAULT_TEST_FEATURE_FLAGS) }
    }
internal val mockSecretGenerator = mock<SecretGenerator>()
internal val mockPinCodeGenerator = mock<PinCodeGenerator>()
internal val mockEntropyCalculator = mock<EntropyCalculator>()
internal val mockGetDefaultCreateContentTypeUseCase = mock<GetDefaultCreateContentTypeUseCase>()
internal val mockGetEditContentTypeUseCase = mock<GetEditContentTypeUseCase>()
internal val mockGetLocalResourceUseCase = mock<GetLocalResourceUseCase>()
internal val mockMetadataPrivateKeysHelperInteractor = mock<MetadataPrivateKeysHelperInteractor>()
internal val mockSecretPropertiesActionsInteractorSecretPropertiesActionsInteractorFactory =
    mock<SecretPropertiesActionsInteractorFactory>()
internal val mockResourceUpdateActionsInteractorFactory = mock<ResourceUpdateActionsInteractorFactory>()
internal val mockResourceCreateActionsInteractor = mock<ResourceCreateActionsInteractor>()
internal val mockCheckPasswordPropertiesUseCase = mock<CheckPasswordPropertiesUseCase>()
internal val mockGetMetadataTypesSettingsUseCase = mock<GetMetadataTypesSettingsUseCase>()

internal val DEFAULT_FEATURE_FLAGS =
    FeatureFlagsModel(
        privacyPolicyUrl = null,
        termsAndConditionsUrl = null,
        isPreviewPasswordAvailable = false,
        areFoldersAvailable = false,
        areTagsAvailable = false,
        isTotpAvailable = false,
        isRbacAvailable = false,
        isPasswordExpiryAvailable = false,
        arePasswordPoliciesAvailable = false,
        canUpdatePasswordPolicies = false,
        isV5MetadataAvailable = false,
    )

internal val DEFAULT_METADATA_TYPES_SETTINGS =
    MetadataTypesSettingsModel(
        defaultMetadataType = V4,
        defaultFolderType = V4,
        defaultTagType = V4,
        allowCreationOfV5Resources = false,
        allowCreationOfV5Folders = false,
        allowCreationOfV5Tags = false,
        allowCreationOfV4Resources = true,
        allowCreationOfV4Folders = true,
        allowCreationOfV4Tags = true,
        allowV4V5Upgrade = false,
        allowV5V4Downgrade = false,
    )

@OptIn(ExperimentalCoroutinesApi::class)
internal val testResourceFormModule =
    module {
        factoryOf(::TestCoroutineLaunchContext) bind CoroutineLaunchContext::class
        factoryOf(::ResourceFormMapper)
        singleOf(::ResourceModelHandler)
        factoryOf(::ResourceTypesUpdatesAdjacencyGraph)
        factoryOf(::CreateResourceIdlingResource)
        factoryOf(::UpdateResourceIdlingResource)

        single { mockGetDefaultCreateContentTypeUseCase }
        single { mockGetEditContentTypeUseCase }
        single { mockGetLocalResourceUseCase }
        single<SecretPropertiesActionsInteractorFactory> { mockSecretPropertiesActionsInteractorSecretPropertiesActionsInteractorFactory }
        single<ResourceUpdateActionsInteractorFactory> { mockResourceUpdateActionsInteractorFactory }
        single<ResourceCreateActionsInteractor> { mockResourceCreateActionsInteractor }
        single {
            mapOf(
                DefaultValue.NAME to "no name",
            )
        }

        single { mock<GetSessionExpiryUseCase>() }

        singleOf(::OfflineSessionState)
        single { mock<PassphraseMemoryCache>() }

        viewModel { params ->
            ResourceFormViewModel(
                mode = params.get(),
                getPasswordPoliciesUseCase = mockGetPasswordPoliciesUseCase,
                getOrLoadGeneratorSettingsUseCase = GetOrLoadGeneratorSettingsUseCase(mockGetPasswordPoliciesUseCase),
                passwordPoliciesInteractor = mockPasswordPoliciesInteractor,
                passwordExpiryPoliciesInteractor = mockPasswordExpiryPoliciesInteractor,
                getFeatureFlagsUseCase = mockGetFeatureFlagsUseCase,
                coroutineLaunchContext = get(),
                secretGenerator = mockSecretGenerator,
                pinCodeGenerator = mockPinCodeGenerator,
                entropyCalculator = mockEntropyCalculator,
                metadataPrivateKeysHelperInteractor = mockMetadataPrivateKeysHelperInteractor,
                getLocalResourceUseCase = get(),
                resourceFormMapper = get(),
                resourceModelHandler = get(),
                dataRefreshTrackingFlow = get(),
                createResourceIdlingResource = get(),
                updateResourceIdlingResource = get(),
                resourceUpdateActionsInteractorFactory = get(),
                checkPasswordPropertiesUseCase = mockCheckPasswordPropertiesUseCase,
                getMetadataTypesSettingsUseCase = mockGetMetadataTypesSettingsUseCase,
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
        singleOf(::DataRefreshTrackingFlow)
        singleOf(::SessionRefreshTrackingFlow)
    }
