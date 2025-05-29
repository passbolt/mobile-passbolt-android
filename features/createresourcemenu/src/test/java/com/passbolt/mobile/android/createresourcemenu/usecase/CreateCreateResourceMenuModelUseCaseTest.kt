package com.passbolt.mobile.android.createresourcemenu.usecase

import com.passbolt.mobile.android.entity.featureflags.FeatureFlagsModel
import com.passbolt.mobile.android.featureflags.usecase.GetFeatureFlagsUseCase
import com.passbolt.mobile.android.metadata.usecase.GetMetadataTypesSettingsUseCase
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
import com.passbolt.mobile.android.supportedresourceTypes.SupportedContentTypes
import com.passbolt.mobile.android.ui.HomeDisplayViewModel
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.AllItems
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.Expiry
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.Favourites
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.OwnedByMe
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.RecentlyModified
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.SharedWithMe
import com.passbolt.mobile.android.ui.MetadataTypeModel
import com.passbolt.mobile.android.ui.MetadataTypesSettingsModel
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

class CreateCreateResourceMenuModelUseCaseTest : KoinTest {
    private val useCase: CreateCreateResourceMenuModelUseCase by inject()

    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(testCreateCreateResourceMenuModelUseCaseModule)
        }

    @Before
    fun setup() {
        mockResourceTypeIdToSlugMappingProvider.stub {
            onBlocking { provideMappingForSelectedAccount() }.doReturn(
                buildMap {
                    SupportedContentTypes.allSlugs.forEach {
                        // only map values matter for the tests
                        put(UUID.randomUUID(), it)
                    }
                },
            )
        }
        mockGetMetadataTypesSettingsUseCase.stub {
            onBlocking { execute(Unit) } doReturn
                GetMetadataTypesSettingsUseCase.Output(
                    metadataTypesSettingsModel =
                        MetadataTypesSettingsModel(
                            defaultMetadataType = MetadataTypeModel.V5,
                            defaultFolderType = MetadataTypeModel.V5,
                            defaultTagType = MetadataTypeModel.V5,
                            allowCreationOfV5Resources = true,
                            allowCreationOfV5Folders = false,
                            allowCreationOfV5Tags = false,
                            allowCreationOfV4Resources = true,
                            allowCreationOfV4Folders = true,
                            allowCreationOfV4Tags = true,
                            allowV4V5Upgrade = false,
                            allowV5V4Downgrade = false,
                        ),
                )
        }
        mockGetFeatureFlagsUseCase.stub {
            onBlocking { execute(Unit) } doReturn
                GetFeatureFlagsUseCase.Output(
                    FeatureFlagsModel(
                        privacyPolicyUrl = null,
                        termsAndConditionsUrl = null,
                        isPreviewPasswordAvailable = true,
                        areFoldersAvailable = true,
                        areTagsAvailable = true,
                        isTotpAvailable = false,
                        isRbacAvailable = true,
                        isPasswordExpiryAvailable = true,
                        arePasswordPoliciesAvailable = true,
                        canUpdatePasswordPolicies = true,
                        isV5MetadataAvailable = true,
                    ),
                )
        }
    }

    @Test
    fun `totp should be disabled when feature flag is turned off`() =
        runTest {
            mockGetFeatureFlagsUseCase.stub {
                onBlocking { execute(Unit) } doReturn
                    GetFeatureFlagsUseCase.Output(
                        FeatureFlagsModel(
                            privacyPolicyUrl = null,
                            termsAndConditionsUrl = null,
                            isPreviewPasswordAvailable = true,
                            areFoldersAvailable = true,
                            areTagsAvailable = true,
                            isTotpAvailable = false,
                            isRbacAvailable = true,
                            isPasswordExpiryAvailable = true,
                            arePasswordPoliciesAvailable = true,
                            canUpdatePasswordPolicies = true,
                            isV5MetadataAvailable = true,
                        ),
                    )
            }

            val model = useCase.execute(CreateCreateResourceMenuModelUseCase.Input(null)).model

            assertFalse { model.isTotpEnabled }
        }

    @Test
    fun `folders should be enabled on folders view only `() =
        runTest {
            mockGetFeatureFlagsUseCase.stub {
                onBlocking { execute(Unit) } doReturn
                    GetFeatureFlagsUseCase.Output(
                        FeatureFlagsModel(
                            privacyPolicyUrl = null,
                            termsAndConditionsUrl = null,
                            isPreviewPasswordAvailable = true,
                            areFoldersAvailable = true,
                            areTagsAvailable = true,
                            isTotpAvailable = false,
                            isRbacAvailable = true,
                            isPasswordExpiryAvailable = true,
                            arePasswordPoliciesAvailable = true,
                            canUpdatePasswordPolicies = true,
                            isV5MetadataAvailable = true,
                        ),
                    )
            }

            val foldersEnabledModel =
                useCase
                    .execute(
                        CreateCreateResourceMenuModelUseCase.Input(HomeDisplayViewModel.folderRoot()),
                    ).model
            val foldersDisabledModel =
                listOf(
                    AllItems,
                    Favourites,
                    RecentlyModified,
                    SharedWithMe,
                    OwnedByMe,
                    Expiry,
                    HomeDisplayViewModel.groupsRoot(),
                    HomeDisplayViewModel.tagsRoot(),
                    null,
                ).map {
                    useCase.execute(CreateCreateResourceMenuModelUseCase.Input(it)).model
                }

            assertTrue { foldersEnabledModel.isFolderEnabled }
            assert(foldersDisabledModel.all { !it.isFolderEnabled })
        }

    @Test
    fun `password creation for v5 should be blocked if no v5 resource type supported`() =
        runTest {
            mockResourceTypeIdToSlugMappingProvider.stub {
                onBlocking { provideMappingForSelectedAccount() }.doReturn(
                    buildMap {
                        SupportedContentTypes.allSlugs.forEach {
                            // only map values matter for the tests
                            if (ContentType.fromSlug(it) != ContentType.V5Default) {
                                put(UUID.randomUUID(), it)
                            }
                        }
                    },
                )
            }
            mockGetMetadataTypesSettingsUseCase.stub {
                onBlocking { execute(Unit) } doReturn
                    GetMetadataTypesSettingsUseCase.Output(
                        metadataTypesSettingsModel =
                            MetadataTypesSettingsModel(
                                defaultMetadataType = MetadataTypeModel.V5,
                                defaultFolderType = MetadataTypeModel.V5,
                                defaultTagType = MetadataTypeModel.V5,
                                allowCreationOfV5Resources = true,
                                allowCreationOfV5Folders = false,
                                allowCreationOfV5Tags = false,
                                allowCreationOfV4Resources = true,
                                allowCreationOfV4Folders = true,
                                allowCreationOfV4Tags = true,
                                allowV4V5Upgrade = false,
                                allowV5V4Downgrade = false,
                            ),
                    )
            }

            val model = useCase.execute(CreateCreateResourceMenuModelUseCase.Input(null)).model

            assertFalse { model.isPasswordEnabled }
        }

    @Test
    fun `password creation for v4 should be blocked if no v4 resource type supported`() =
        runTest {
            mockResourceTypeIdToSlugMappingProvider.stub {
                onBlocking { provideMappingForSelectedAccount() }.doReturn(
                    buildMap {
                        SupportedContentTypes.allSlugs.forEach {
                            // only map values matter for the tests
                            if (ContentType.fromSlug(it) != ContentType.PasswordAndDescription) {
                                put(UUID.randomUUID(), it)
                            }
                        }
                    },
                )
            }
            mockGetMetadataTypesSettingsUseCase.stub {
                onBlocking { execute(Unit) } doReturn
                    GetMetadataTypesSettingsUseCase.Output(
                        metadataTypesSettingsModel =
                            MetadataTypesSettingsModel(
                                defaultMetadataType = MetadataTypeModel.V4,
                                defaultFolderType = MetadataTypeModel.V5,
                                defaultTagType = MetadataTypeModel.V5,
                                allowCreationOfV5Resources = true,
                                allowCreationOfV5Folders = false,
                                allowCreationOfV5Tags = false,
                                allowCreationOfV4Resources = true,
                                allowCreationOfV4Folders = true,
                                allowCreationOfV4Tags = true,
                                allowV4V5Upgrade = false,
                                allowV5V4Downgrade = false,
                            ),
                    )
            }

            val model = useCase.execute(CreateCreateResourceMenuModelUseCase.Input(null)).model

            assertFalse { model.isPasswordEnabled }
        }

    @Test
    fun `totp creation for v5 should be blocked if no v5 resource type supported`() =
        runTest {
            mockResourceTypeIdToSlugMappingProvider.stub {
                onBlocking { provideMappingForSelectedAccount() }.doReturn(
                    buildMap {
                        SupportedContentTypes.allSlugs.forEach {
                            // only map values matter for the tests
                            if (ContentType.fromSlug(it) != ContentType.V5TotpStandalone) {
                                put(UUID.randomUUID(), it)
                            }
                        }
                    },
                )
            }
            mockGetMetadataTypesSettingsUseCase.stub {
                onBlocking { execute(Unit) } doReturn
                    GetMetadataTypesSettingsUseCase.Output(
                        metadataTypesSettingsModel =
                            MetadataTypesSettingsModel(
                                defaultMetadataType = MetadataTypeModel.V5,
                                defaultFolderType = MetadataTypeModel.V5,
                                defaultTagType = MetadataTypeModel.V5,
                                allowCreationOfV5Resources = true,
                                allowCreationOfV5Folders = false,
                                allowCreationOfV5Tags = false,
                                allowCreationOfV4Resources = true,
                                allowCreationOfV4Folders = true,
                                allowCreationOfV4Tags = true,
                                allowV4V5Upgrade = false,
                                allowV5V4Downgrade = false,
                            ),
                    )
            }

            val model = useCase.execute(CreateCreateResourceMenuModelUseCase.Input(null)).model

            assertFalse { model.isTotpEnabled }
        }

    @Test
    fun `totp creation for v4 should be blocked if no v4 resource type supported`() =
        runTest {
            mockResourceTypeIdToSlugMappingProvider.stub {
                onBlocking { provideMappingForSelectedAccount() }.doReturn(
                    buildMap {
                        SupportedContentTypes.allSlugs.forEach {
                            // only map values matter for the tests
                            if (ContentType.fromSlug(it) != ContentType.Totp) {
                                put(UUID.randomUUID(), it)
                            }
                        }
                    },
                )
            }
            mockGetMetadataTypesSettingsUseCase.stub {
                onBlocking { execute(Unit) } doReturn
                    GetMetadataTypesSettingsUseCase.Output(
                        metadataTypesSettingsModel =
                            MetadataTypesSettingsModel(
                                defaultMetadataType = MetadataTypeModel.V4,
                                defaultFolderType = MetadataTypeModel.V5,
                                defaultTagType = MetadataTypeModel.V5,
                                allowCreationOfV5Resources = true,
                                allowCreationOfV5Folders = false,
                                allowCreationOfV5Tags = false,
                                allowCreationOfV4Resources = true,
                                allowCreationOfV4Folders = true,
                                allowCreationOfV4Tags = true,
                                allowV4V5Upgrade = false,
                                allowV5V4Downgrade = false,
                            ),
                    )
            }

            val model = useCase.execute(CreateCreateResourceMenuModelUseCase.Input(null)).model

            assertFalse { model.isTotpEnabled }
        }
}
