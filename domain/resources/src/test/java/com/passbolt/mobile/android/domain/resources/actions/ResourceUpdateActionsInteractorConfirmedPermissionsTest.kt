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

package com.passbolt.mobile.android.domain.resources.actions

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.commontest.session.validSessionTestModule
import com.passbolt.mobile.android.core.architecture.result.DomainResult
import com.passbolt.mobile.android.core.architecture.result.DomainResult.Incomplete.Error.Reason.UNKNOWN
import com.passbolt.mobile.android.core.resourcetypes.graph.redesigned.ResourceTypesUpdatesAdjacencyGraph
import com.passbolt.mobile.android.domain.folders.usecase.GetLocalFolderPermissionsUseCase
import com.passbolt.mobile.android.domain.metadata.interactor.MetadataPrivateKeysInteractor
import com.passbolt.mobile.android.domain.metadata.usecase.GetMetadataKeysSettingsUseCase
import com.passbolt.mobile.android.domain.metadata.usecase.db.GetLocalMetadataKeysUseCase
import com.passbolt.mobile.android.domain.permissionsconfirmation.model.PermissionsSnapshot
import com.passbolt.mobile.android.domain.permissionsconfirmation.usecase.GetPermissionsSnapshotUseCase
import com.passbolt.mobile.android.domain.resources.interactor.update.UpdateResourceInteractor
import com.passbolt.mobile.android.domain.resources.usecase.CreatePermissionsSnapshotInteractor
import com.passbolt.mobile.android.domain.resources.usecase.CreatePermissionsSnapshotInteractor.DriftOutput
import com.passbolt.mobile.android.domain.resources.usecase.ResourceShareInteractor
import com.passbolt.mobile.android.domain.resources.usecase.db.GetLocalResourcePermissionsUseCase
import com.passbolt.mobile.android.domain.resources.usecase.db.UpdateLocalResourceUseCase
import com.passbolt.mobile.android.domain.resourcetypes.usecase.ResourceTypeIdToSlugMappingProvider
import com.passbolt.mobile.android.domain.secrets.model.SecretJsonModel
import com.passbolt.mobile.android.domain.secrets.usecase.decrypt.SecretInput
import com.passbolt.mobile.android.domain.users.model.GpgKey
import com.passbolt.mobile.android.domain.users.model.UserProfile
import com.passbolt.mobile.android.domain.users.usecase.GetLocalCurrentUserUseCase
import com.passbolt.mobile.android.jsonmodel.jsonModelModule
import com.passbolt.mobile.android.mappers.SharePermissionsModelMapper.Companion.TEMPORARY_NEW_PERMISSION_ID
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.PasswordAndDescription
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.V5Default
import com.passbolt.mobile.android.ui.GpgKeyUiModel
import com.passbolt.mobile.android.ui.MetadataJsonModel
import com.passbolt.mobile.android.ui.MetadataKeyTypeModel.PERSONAL
import com.passbolt.mobile.android.ui.MetadataKeysSettingsModel
import com.passbolt.mobile.android.ui.PermissionModel
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.ResourcePermission
import com.passbolt.mobile.android.ui.ResourceUiModel
import com.passbolt.mobile.android.ui.UpdateResourceModel
import com.passbolt.mobile.android.ui.UserProfileUiModel
import com.passbolt.mobile.android.ui.UserUiModel
import com.passbolt.mobile.android.ui.UserWithAvatar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.assertIs

@ExperimentalCoroutinesApi
class ResourceUpdateActionsInteractorConfirmedPermissionsTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(jsonModelModule, validSessionTestModule)
        }

    private val createPermissionsSnapshotInteractor = mock<CreatePermissionsSnapshotInteractor>()
    private val getPermissionsSnapshotUseCase = mock<GetPermissionsSnapshotUseCase>()
    private val resourceShareInteractor = mock<ResourceShareInteractor>()
    private val confirmedRecipientsPublicKeysResolver = mock<ConfirmedRecipientsPublicKeysResolver>()
    private val updateResourceInteractor = mock<UpdateResourceInteractor>()
    private val secretPropertiesActionsInteractor = mock<SecretPropertiesActionsInteractor>()
    private val getMetadataKeysSettingsUseCase = mock<GetMetadataKeysSettingsUseCase>()
    private val metadataPrivateKeysInteractor = mock<MetadataPrivateKeysInteractor>()
    private val getMetadataKeysUseCase = mock<GetLocalMetadataKeysUseCase>()
    private val getLocalResourcePermissionsUseCase = mock<GetLocalResourcePermissionsUseCase>()
    private val getLocalCurrentUserUseCase = mock<GetLocalCurrentUserUseCase>()
    private val mappingProvider = mock<ResourceTypeIdToSlugMappingProvider>()

    private val interactor =
        ResourceUpdateActionsInteractor(
            existingResource = resourceModel(),
            secretPropertiesActionsInteractor = secretPropertiesActionsInteractor,
            updateResourceInteractor = updateResourceInteractor,
            resourceTypesUpdateGraph = mock<ResourceTypesUpdatesAdjacencyGraph>(),
            updateLocalResourceUseCase = mock<UpdateLocalResourceUseCase>(),
            getLocalCurrentUserUseCase = getLocalCurrentUserUseCase,
            metadataPrivateKeysInteractor = metadataPrivateKeysInteractor,
            getLocalFolderPermissionsUseCase = mock<GetLocalFolderPermissionsUseCase>(),
            getLocalResourcePermissionsUseCase = getLocalResourcePermissionsUseCase,
            getMetadataKeysSettingsUseCase = getMetadataKeysSettingsUseCase,
            getMetadataKeysUseCase = getMetadataKeysUseCase,
            resourceTypeIdToSlugMappingProvider = mappingProvider,
            createPermissionsSnapshotInteractor = createPermissionsSnapshotInteractor,
            getPermissionsSnapshotUseCase = getPermissionsSnapshotUseCase,
            resourceShareInteractor = resourceShareInteractor,
            confirmedRecipientsPublicKeysResolver = confirmedRecipientsPublicKeysResolver,
        )

    @Before
    fun setUp() {
        createPermissionsSnapshotInteractor.stub {
            on { detectDriftForResource(RESOURCE_ID) } doReturn DriftOutput.NoDrift
        }
        getPermissionsSnapshotUseCase.stub {
            on { execute(Unit) } doReturn GetPermissionsSnapshotUseCase.Output(SNAPSHOT)
        }
        confirmedRecipientsPublicKeysResolver.stub {
            on { resolve(any()) } doReturn CONFIRMED_KEYS
        }
        getLocalCurrentUserUseCase.stub {
            on { execute(Unit) } doReturn GetLocalCurrentUserUseCase.Output(OPERATOR_USER)
        }
        resourceShareInteractor.stub {
            on { simulateAndShareResource(any(), any(), any(), anyOrNull()) }
                .doReturn(ResourceShareInteractor.Output.Success)
        }
        stubSuccessfulUpdate()
    }

    @Test
    fun `drift detected stops before any permission changes or update`() =
        runTest {
            createPermissionsSnapshotInteractor.stub {
                on { detectDriftForResource(RESOURCE_ID) } doReturn DriftOutput.DriftDetected(listOf("drifted-user"))
            }

            val result =
                interactor
                    .updateGenericResourceWithConfirmedPermissions(PasswordAndDescription, listOf(OPERATOR_UI, USER_UI))
                    .single()

            assertIs<ResourceUpdateActionResult.PermissionsDrifted>(result)
            verifyNoInteractions(resourceShareInteractor)
            verifyNoInteractions(updateResourceInteractor)
        }

    @Test
    fun `missing snapshot during the drift check stops with a drift result without names`() =
        runTest {
            createPermissionsSnapshotInteractor.stub {
                on { detectDriftForResource(RESOURCE_ID) } doReturn DriftOutput.SnapshotMissing
            }

            val result =
                interactor
                    .updateGenericResourceWithConfirmedPermissions(PasswordAndDescription, listOf(OPERATOR_UI, USER_UI))
                    .single()

            assertThat(result).isEqualTo(ResourceUpdateActionResult.PermissionsDrifted(driftedEntityNames = emptyList()))
            verifyNoInteractions(resourceShareInteractor)
            verifyNoInteractions(updateResourceInteractor)
        }

    @Test
    fun `dry-run drift during the revocation share stops before the update`() =
        runTest {
            resourceShareInteractor.stub {
                on { simulateAndShareResource(any(), any(), any(), anyOrNull()) }
                    .doReturn(ResourceShareInteractor.Output.DriftDetected)
            }

            val result =
                interactor
                    .updateGenericResourceWithConfirmedPermissions(PasswordAndDescription, listOf(OPERATOR_UI))
                    .single()

            assertThat(result).isEqualTo(ResourceUpdateActionResult.PermissionsDrifted(driftedEntityNames = emptyList()))
            verifyNoInteractions(updateResourceInteractor)
        }

    @Test
    fun `drift check failure stops before any permission changes or update`() =
        runTest {
            createPermissionsSnapshotInteractor.stub {
                on { detectDriftForResource(RESOURCE_ID) } doReturn DriftOutput.Failure(FAILURE)
            }

            val result =
                interactor
                    .updateGenericResourceWithConfirmedPermissions(PasswordAndDescription, listOf(OPERATOR_UI, USER_UI))
                    .single()

            assertIs<ResourceUpdateActionResult.ShareFailure>(result)
            verifyNoInteractions(resourceShareInteractor)
            verifyNoInteractions(updateResourceInteractor)
        }

    @Test
    fun `missing snapshot is treated as drift`() =
        runTest {
            getPermissionsSnapshotUseCase.stub {
                on { execute(Unit) } doReturn GetPermissionsSnapshotUseCase.Output(null)
            }

            val result =
                interactor
                    .updateGenericResourceWithConfirmedPermissions(PasswordAndDescription, listOf(OPERATOR_UI, USER_UI))
                    .single()

            assertIs<ResourceUpdateActionResult.PermissionsDrifted>(result)
            verifyNoInteractions(resourceShareInteractor)
            verifyNoInteractions(updateResourceInteractor)
        }

    @Test
    fun `revocations are applied before the update and new recipients after it`() =
        runTest {
            val confirmedPermissions = listOf(OPERATOR_UI, ADDED_UI)

            val result =
                interactor
                    .updateGenericResourceWithConfirmedPermissions(PasswordAndDescription, confirmedPermissions)
                    .single()

            assertIs<ResourceUpdateActionResult.Success>(result)
            val recipientsCaptor = argumentCaptor<List<PermissionModelUi>>()
            val existingCaptor = argumentCaptor<List<PermissionModelUi>>()
            verify(resourceShareInteractor, times(2))
                .simulateAndShareResource(any(), recipientsCaptor.capture(), any(), existingCaptor.capture())
            verify(updateResourceInteractor).execute(any(), any(), any())

            val revocationCallRecipients = recipientsCaptor.firstValue.map { it.permissionId }
            val revocationCallExisting = existingCaptor.firstValue.map { it.permissionId }
            assertThat(revocationCallRecipients).containsExactly(OPERATOR_PERMISSION_ID)
            assertThat(revocationCallExisting).containsExactly(OPERATOR_PERMISSION_ID, USER_PERMISSION_ID)

            val additionCallRecipients = recipientsCaptor.secondValue.map { it.permissionId }
            val additionCallExisting = existingCaptor.secondValue.map { it.permissionId }
            assertThat(additionCallRecipients).containsExactly(OPERATOR_PERMISSION_ID, TEMPORARY_NEW_PERMISSION_ID)
            assertThat(additionCallExisting).containsExactly(OPERATOR_PERMISSION_ID)
        }

    @Test
    fun `operator self-removal is applied in a single share after the update`() =
        runTest {
            val result =
                interactor
                    .updateGenericResourceWithConfirmedPermissions(PasswordAndDescription, listOf(USER_UI))
                    .single()

            assertIs<ResourceUpdateActionResult.Success>(result)
            val recipientsCaptor = argumentCaptor<List<PermissionModelUi>>()
            verify(resourceShareInteractor, times(1))
                .simulateAndShareResource(any(), recipientsCaptor.capture(), any(), anyOrNull())
            val orderVerifier = inOrder(updateResourceInteractor, resourceShareInteractor)
            orderVerifier.verify(updateResourceInteractor).execute(any(), any(), any())
            orderVerifier
                .verify(resourceShareInteractor)
                .simulateAndShareResource(any(), any(), any(), anyOrNull())
            assertThat(recipientsCaptor.firstValue.map { it.permissionId }).containsExactly(USER_PERMISSION_ID)
        }

    @Test
    fun `operator self-downgrade is held at the snapshot level until the final share`() =
        runTest {
            val confirmedPermissions = listOf(OPERATOR_UI.copy(permission = ResourcePermission.UPDATE))

            val result =
                interactor
                    .updateGenericResourceWithConfirmedPermissions(PasswordAndDescription, confirmedPermissions)
                    .single()

            assertIs<ResourceUpdateActionResult.Success>(result)
            val recipientsCaptor = argumentCaptor<List<PermissionModelUi>>()
            verify(resourceShareInteractor, times(2))
                .simulateAndShareResource(any(), recipientsCaptor.capture(), any(), anyOrNull())
            verify(updateResourceInteractor).execute(any(), any(), any())

            assertThat(operatorRecipient(recipientsCaptor.firstValue).permission).isEqualTo(ResourcePermission.OWNER)
            assertThat(operatorRecipient(recipientsCaptor.secondValue).permission).isEqualTo(ResourcePermission.UPDATE)
        }

    private fun operatorRecipient(recipients: List<PermissionModelUi>) =
        recipients
            .filterIsInstance<PermissionModelUi.UserPermissionModel>()
            .single { it.user.userId == OPERATOR_ID }

    @Test
    fun `identity secret modification marks the secret as unchanged for the update`() =
        runTest {
            interactor
                .updateGenericResourceWithConfirmedPermissions(PasswordAndDescription, listOf(OPERATOR_UI, USER_UI))
                .single()

            val secretInputCaptor = argumentCaptor<SecretInput>()
            verify(updateResourceInteractor).execute(any(), secretInputCaptor.capture(), any())
            assertThat(secretInputCaptor.firstValue.secretChanged).isFalse()
        }

    @Test
    fun `secret modification marks the secret as changed for the update`() =
        runTest {
            interactor
                .updateGenericResourceWithConfirmedPermissions(
                    PasswordAndDescription,
                    listOf(OPERATOR_UI, USER_UI),
                    secretModification = { it.apply { secret = "changed-password" } },
                ).single()

            val secretInputCaptor = argumentCaptor<SecretInput>()
            verify(updateResourceInteractor).execute(any(), secretInputCaptor.capture(), any())
            assertThat(secretInputCaptor.firstValue.secretChanged).isTrue()
        }

    @Test
    fun `unchanged permissions perform only the update`() =
        runTest {
            val result =
                interactor
                    .updateGenericResourceWithConfirmedPermissions(PasswordAndDescription, listOf(OPERATOR_UI, USER_UI))
                    .single()

            assertIs<ResourceUpdateActionResult.Success>(result)
            verify(resourceShareInteractor, never()).simulateAndShareResource(any(), any(), any(), anyOrNull())
            verify(updateResourceInteractor).execute(any(), any(), any())
        }

    @Test
    fun `update failure stops before granting access to new recipients`() =
        runTest {
            updateResourceInteractor.stub {
                on { execute(any(), any(), any()) }
                    .doReturn(UpdateResourceInteractor.Output.Failure(FAILURE))
            }

            val result =
                interactor
                    .updateGenericResourceWithConfirmedPermissions(
                        PasswordAndDescription,
                        listOf(OPERATOR_UI, USER_UI, ADDED_UI),
                    ).single()

            assertIs<ResourceUpdateActionResult.Failure>(result)
            verify(resourceShareInteractor, never()).simulateAndShareResource(any(), any(), any(), anyOrNull())
        }

    @Test
    fun `revocation share failure stops before the update`() =
        runTest {
            resourceShareInteractor.stub {
                on { simulateAndShareResource(any(), any(), any(), anyOrNull()) }
                    .doReturn(ResourceShareInteractor.Output.ShareFailure(FAILURE))
            }

            val result =
                interactor
                    .updateGenericResourceWithConfirmedPermissions(PasswordAndDescription, listOf(OPERATOR_UI))
                    .single()

            assertIs<ResourceUpdateActionResult.ShareFailure>(result)
            verifyNoInteractions(updateResourceInteractor)
        }

    @Test
    fun `confirmed upgrade applies the v5 target type through the confirmed permissions pipeline`() =
        runTest {
            stubMappingWithV5Default()

            val result = interactor.upgradeToV5WithConfirmedPermissions(listOf(OPERATOR_UI, USER_UI)).single()

            assertIs<ResourceUpdateActionResult.Success>(result)
            val resourceInputCaptor = argumentCaptor<UpdateResourceModel>()
            verify(updateResourceInteractor).execute(resourceInputCaptor.capture(), any(), any())
            assertThat(resourceInputCaptor.firstValue.contentType).isEqualTo(V5Default)
        }

    @Test
    fun `drift detected stops the confirmed upgrade before the update`() =
        runTest {
            stubMappingWithV5Default()
            createPermissionsSnapshotInteractor.stub {
                on { detectDriftForResource(RESOURCE_ID) } doReturn DriftOutput.DriftDetected(listOf("drifted-user"))
            }

            val result = interactor.upgradeToV5WithConfirmedPermissions(listOf(OPERATOR_UI, USER_UI)).single()

            assertIs<ResourceUpdateActionResult.PermissionsDrifted>(result)
            verifyNoInteractions(updateResourceInteractor)
            verifyNoInteractions(resourceShareInteractor)
        }

    @Test
    fun `confirmed keys are passed to the update and to the share calls`() =
        runTest {
            interactor
                .updateGenericResourceWithConfirmedPermissions(PasswordAndDescription, listOf(OPERATOR_UI, ADDED_UI))
                .single()

            val shareKeysCaptor = argumentCaptor<Map<String, String>>()
            verify(resourceShareInteractor, times(2))
                .simulateAndShareResource(any(), any(), shareKeysCaptor.capture(), anyOrNull())
            assertThat(shareKeysCaptor.allValues).containsExactly(CONFIRMED_KEYS, CONFIRMED_KEYS)
            val updateKeysCaptor = argumentCaptor<Map<String, String>>()
            verify(updateResourceInteractor).execute(any(), any(), updateKeysCaptor.capture())
            assertThat(updateKeysCaptor.firstValue).isEqualTo(CONFIRMED_KEYS)
        }

    private fun stubMappingWithV5Default() {
        mappingProvider.stub {
            on { provideMappingForSelectedAccount() } doReturn
                mapOf(
                    UUID.randomUUID() to PasswordAndDescription.slug,
                    UUID.randomUUID() to V5Default.slug,
                )
        }
    }

    private fun stubSuccessfulUpdate() {
        mappingProvider.stub {
            on { provideMappingForSelectedAccount() } doReturn mapOf(UUID.randomUUID() to PasswordAndDescription.slug)
        }
        getMetadataKeysSettingsUseCase.stub {
            on { execute(Unit) } doReturn
                GetMetadataKeysSettingsUseCase.Output(
                    MetadataKeysSettingsModel(allowUsageOfPersonalKeys = false, zeroKnowledgeKeyShare = false),
                )
        }
        metadataPrivateKeysInteractor.stub {
            on { refreshAndVerifyMetadataPrivateKey() } doReturn MetadataPrivateKeysInteractor.Output.KeyIsTrusted
        }
        getMetadataKeysUseCase.stub {
            on { execute(any()) } doReturn emptyList()
        }
        getLocalResourcePermissionsUseCase.stub {
            on { execute(any()) } doReturn GetLocalResourcePermissionsUseCase.Output(emptyList())
        }
        secretPropertiesActionsInteractor.stub {
            on { provideDecryptedSecret() } doReturn
                flowOf(
                    SecretPropertyActionResult.Success(
                        "secret",
                        isSecret = true,
                        SecretJsonModel("""{"password":"password"}"""),
                    ),
                )
        }
        updateResourceInteractor.stub {
            on { execute(any(), any(), any()) }
                .doReturn(UpdateResourceInteractor.Output.Success(resourceModel()))
        }
    }

    private companion object {
        private const val RESOURCE_ID = "resourceId"
        private const val OPERATOR_ID = "operator-id"
        private const val USER_ID = "user-id"
        private const val ADDED_USER_ID = "added-user-id"
        private const val OPERATOR_PERMISSION_ID = "perm-operator"
        private const val USER_PERMISSION_ID = "perm-user"

        private val FAILURE = DomainResult.Incomplete.Error(UNKNOWN, "error")
        private val CONFIRMED_KEYS = mapOf(OPERATOR_ID to "key-operator", USER_ID to "key-user", ADDED_USER_ID to "key-added")

        private val OPERATOR_USER =
            UserUiModel(
                id = OPERATOR_ID,
                userName = "$OPERATOR_ID@passbolt.com",
                disabled = false,
                gpgKey =
                    GpgKeyUiModel(
                        id = "gpg-$OPERATOR_ID",
                        armoredKey = "armored-key-$OPERATOR_ID",
                        fingerprint = "fingerprint-$OPERATOR_ID",
                        bits = 2048,
                        uid = null,
                        keyId = "key-$OPERATOR_ID",
                        type = null,
                        keyExpirationDate = null,
                        keyCreationDate = null,
                    ),
                profile =
                    UserProfileUiModel(
                        username = "$OPERATOR_ID@passbolt.com",
                        firstName = "first-$OPERATOR_ID",
                        lastName = "last-$OPERATOR_ID",
                        avatarUrl = null,
                    ),
            )

        private val OPERATOR_UI = userPermissionUi(OPERATOR_ID, OPERATOR_PERMISSION_ID, ResourcePermission.OWNER)
        private val USER_UI = userPermissionUi(USER_ID, USER_PERMISSION_ID, ResourcePermission.READ)
        private val ADDED_UI = userPermissionUi(ADDED_USER_ID, TEMPORARY_NEW_PERMISSION_ID, ResourcePermission.READ)

        private val SNAPSHOT =
            PermissionsSnapshot(
                permissions =
                    listOf(
                        PermissionModel.UserPermissionModel(ResourcePermission.OWNER, OPERATOR_PERMISSION_ID, OPERATOR_ID),
                        PermissionModel.UserPermissionModel(ResourcePermission.READ, USER_PERMISSION_ID, USER_ID),
                    ),
                groupsMembers = emptyMap(),
                users = listOf(userProfile(OPERATOR_ID), userProfile(USER_ID)).associateBy { it.id },
                created = ZonedDateTime.now(),
            )

        private fun userPermissionUi(
            userId: String,
            permissionId: String,
            permission: ResourcePermission,
        ) = PermissionModelUi.UserPermissionModel(
            permission = permission,
            permissionId = permissionId,
            user =
                UserWithAvatar(
                    userId = userId,
                    firstName = "first-$userId",
                    lastName = "last-$userId",
                    userName = "$userId@passbolt.com",
                    isDisabled = false,
                    avatarUrl = null,
                ),
        )

        private fun userProfile(userId: String) =
            UserProfile(
                id = userId,
                username = "$userId@passbolt.com",
                disabled = false,
                role = null,
                firstName = "first-$userId",
                lastName = "last-$userId",
                avatarUrl = null,
                gpgKey =
                    GpgKey(
                        id = "gpg-$userId",
                        armoredKey = "armored-key-$userId",
                        fingerprint = "fingerprint-$userId",
                        bits = 2048,
                        uid = null,
                        keyId = "key-$userId",
                        type = null,
                        keyExpirationDate = null,
                        keyCreationDate = null,
                    ),
            )

        private fun resourceModel(): ResourceUiModel =
            ResourceUiModel(
                resourceId = RESOURCE_ID,
                resourceTypeId = UUID.randomUUID().toString(),
                slug = PasswordAndDescription.slug,
                folderId = null,
                permission = ResourcePermission.OWNER,
                favouriteId = null,
                modified = ZonedDateTime.now(),
                expiry = null,
                metadataKeyId = null,
                metadataKeyType = PERSONAL,
                metadataJsonModel = MetadataJsonModel("""{"name": "Test"}"""),
            )
    }
}
