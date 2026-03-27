package com.passbolt.mobile.android.permissions.permissions

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.google.gson.GsonBuilder
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.spi.json.GsonJsonProvider
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider
import com.passbolt.mobile.android.common.datarefresh.DataRefreshTrackingFlow
import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.commontest.session.validSessionTestModule
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderDetailsUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderPermissionsUseCase
import com.passbolt.mobile.android.core.fulldatarefresh.HomeDataInteractor
import com.passbolt.mobile.android.core.mvp.authentication.SessionRefreshTrackingFlow
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.resources.actions.ResourceUpdateActionsInteractorFactory
import com.passbolt.mobile.android.core.resources.usecase.ResourceShareInteractor
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourcePermissionsUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourceUseCase
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.ResourceTypeIdToSlugMappingProvider
import com.passbolt.mobile.android.jsonmodel.JSON_MODEL_GSON
import com.passbolt.mobile.android.jsonmodel.jsonpathops.JsonPathJsonPathOps
import com.passbolt.mobile.android.jsonmodel.jsonpathops.JsonPathsOps
import com.passbolt.mobile.android.metadata.interactor.MetadataPrivateKeysHelperInteractor
import com.passbolt.mobile.android.metadata.usecase.CanShareResourceUseCase
import com.passbolt.mobile.android.permissions.permissions.PermissionsIntent.GroupPermissionDeleted
import com.passbolt.mobile.android.permissions.permissions.PermissionsIntent.GroupPermissionModified
import com.passbolt.mobile.android.permissions.permissions.PermissionsIntent.MainButtonIntent
import com.passbolt.mobile.android.permissions.permissions.PermissionsIntent.UserPermissionDeleted
import com.passbolt.mobile.android.permissions.permissions.PermissionsIntent.UserPermissionModified
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
import com.passbolt.mobile.android.ui.GroupModel
import com.passbolt.mobile.android.ui.MetadataJsonModel
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.PermissionsItem
import com.passbolt.mobile.android.ui.PermissionsMode
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.ResourcePermission
import com.passbolt.mobile.android.ui.UserWithAvatar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.core.module.dsl.singleOf
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import java.time.ZonedDateTime
import java.util.EnumSet
import java.util.UUID
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class PermissionsViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                module {
                    single { mock<GetLocalResourcePermissionsUseCase>() }
                    single { mock<GetLocalFolderPermissionsUseCase>() }
                    single { mock<GetLocalResourceUseCase>() }
                    single { mock<GetLocalFolderDetailsUseCase>() }
                    single { mock<ResourceShareInteractor>() }
                    single { mock<HomeDataInteractor>() }
                    single { mock<ResourceTypeIdToSlugMappingProvider>() }
                    single { mock<MetadataPrivateKeysHelperInteractor>() }
                    single { mock<ResourceUpdateActionsInteractorFactory>() }
                    single { mock<CanShareResourceUseCase>() }
                    singleOf(::TestCoroutineLaunchContext) bind CoroutineLaunchContext::class
                    singleOf(::SessionRefreshTrackingFlow)
                    singleOf(::DataRefreshTrackingFlow)
                    factory { PermissionModelUiComparator() }
                    single(named(JSON_MODEL_GSON)) { GsonBuilder().serializeNulls().create() }
                    single {
                        Configuration
                            .builder()
                            .jsonProvider(GsonJsonProvider())
                            .mappingProvider(GsonMappingProvider())
                            .options(EnumSet.noneOf(Option::class.java))
                            .build()
                    }
                    singleOf(::JsonPathJsonPathOps) bind JsonPathsOps::class
                    factory { params ->
                        PermissionsViewModel(
                            permissionsItem = params.get(),
                            id = params.get(),
                            mode = params.get(),
                            getLocalResourcePermissionsUseCase = get(),
                            getLocalResourceUseCase = get(),
                            getLocalFolderPermissionsUseCase = get(),
                            getLocalFolderUseCase = get(),
                            permissionModelUiComparator = get(),
                            resourceShareInteractor = get(),
                            homeDataInteractor = get(),
                            resourceTypeIdToSlugMappingProvider = get(),
                            metadataPrivateKeysHelperInteractor = get(),
                            canShareResourceUseCase = get(),
                            dataRefreshTrackingFlow = get(),
                            coroutineLaunchContext = get(),
                            resourceUpdateActionsInteractorFactory = get(),
                        )
                    }
                },
                validSessionTestModule,
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        get<GetLocalResourcePermissionsUseCase>().stub {
            onBlocking { execute(GetLocalResourcePermissionsUseCase.Input(RESOURCE_ID)) }
                .doReturn(GetLocalResourcePermissionsUseCase.Output(GROUP_PERMISSIONS + USER_PERMISSIONS))
        }
        get<GetLocalResourceUseCase>().stub {
            onBlocking { execute(GetLocalResourceUseCase.Input(RESOURCE_ID)) }
                .doReturn(GetLocalResourceUseCase.Output(RESOURCE_MODEL))
        }
        get<CanShareResourceUseCase>().stub {
            onBlocking { execute(Unit) } doReturn CanShareResourceUseCase.Output(canShareResource = true)
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `save button and add user button should be shown in edit mode`() =
        runTest {
            val viewModel =
                get<PermissionsViewModel>(
                    parameters = { parametersOf(RESOURCE_ID, PermissionsMode.EDIT, PermissionsItem.RESOURCE) },
                )

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.showSaveButton).isTrue()
                assertThat(state.showAddUserButton).isTrue()
            }
        }

    @Test
    fun `edit button should be shown in view mode and if owner`() =
        runTest {
            get<GetLocalResourceUseCase>().stub {
                onBlocking { execute(GetLocalResourceUseCase.Input(RESOURCE_ID)) }
                    .doReturn(GetLocalResourceUseCase.Output(RESOURCE_MODEL.copy(permission = ResourcePermission.OWNER)))
            }

            val viewModel =
                get<PermissionsViewModel>(
                    parameters = { parametersOf(RESOURCE_ID, PermissionsMode.VIEW, PermissionsItem.RESOURCE) },
                )

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.showEditButton).isTrue()
            }
        }

    @Test
    fun `error should be shown when sharing not possible`() =
        runTest {
            get<CanShareResourceUseCase>().stub {
                onBlocking { execute(Unit) } doReturn CanShareResourceUseCase.Output(canShareResource = false)
            }
            get<GetLocalResourceUseCase>().stub {
                onBlocking { execute(GetLocalResourceUseCase.Input(RESOURCE_ID)) }
                    .doReturn(GetLocalResourceUseCase.Output(RESOURCE_MODEL.copy(permission = ResourcePermission.OWNER)))
            }

            val viewModel =
                get<PermissionsViewModel>(
                    parameters = { parametersOf(RESOURCE_ID, PermissionsMode.VIEW, PermissionsItem.RESOURCE) },
                )

            viewModel.sideEffect.test {
                viewModel.onIntent(MainButtonIntent)
                val effect = awaitItem()
                assertIs<PermissionsSideEffect.ShowErrorSnackbar>(effect)
                assertThat(effect.type).isEqualTo(SnackbarErrorType.CANNOT_SHARE_RESOURCE)
            }
        }

    @Test
    fun `empty state should be shown when there are no permissions`() =
        runTest {
            get<GetLocalResourcePermissionsUseCase>().stub {
                onBlocking { execute(GetLocalResourcePermissionsUseCase.Input(RESOURCE_ID)) }
                    .doReturn(GetLocalResourcePermissionsUseCase.Output(emptyList()))
            }

            val viewModel =
                get<PermissionsViewModel>(
                    parameters = { parametersOf(RESOURCE_ID, PermissionsMode.VIEW, PermissionsItem.RESOURCE) },
                )

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.showEmptyState).isTrue()
            }
        }

    @Test
    fun `existing permissions should be shown initially`() =
        runTest {
            val viewModel =
                get<PermissionsViewModel>(
                    parameters = { parametersOf(RESOURCE_ID, PermissionsMode.VIEW, PermissionsItem.RESOURCE) },
                )

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.permissions).containsExactlyElementsIn(GROUP_PERMISSIONS + USER_PERMISSIONS)
            }
        }

    @Test
    fun `should show warning if there is not at least one owner permission`() =
        runTest {
            val viewModel =
                get<PermissionsViewModel>(
                    parameters = { parametersOf(RESOURCE_ID, PermissionsMode.EDIT, PermissionsItem.RESOURCE) },
                )

            viewModel.sideEffect.test {
                viewModel.onIntent(MainButtonIntent)
                val effect = awaitItem()
                assertIs<PermissionsSideEffect.ShowErrorSnackbar>(effect)
                assertThat(effect.type).isEqualTo(SnackbarErrorType.ONE_OWNER_REQUIRED)
            }
        }

    @Test
    fun `should not show deleted user permission`() =
        runTest {
            val viewModel =
                get<PermissionsViewModel>(
                    parameters = { parametersOf(RESOURCE_ID, PermissionsMode.VIEW, PermissionsItem.RESOURCE) },
                )

            viewModel.onIntent(UserPermissionDeleted(USER_PERMISSIONS[0]))

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.permissions).containsExactlyElementsIn(GROUP_PERMISSIONS)
            }
        }

    @Test
    fun `should not show deleted group permission`() =
        runTest {
            val viewModel =
                get<PermissionsViewModel>(
                    parameters = { parametersOf(RESOURCE_ID, PermissionsMode.VIEW, PermissionsItem.RESOURCE) },
                )

            viewModel.onIntent(GroupPermissionDeleted(GROUP_PERMISSIONS[0]))

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.permissions).containsExactlyElementsIn(USER_PERMISSIONS)
            }
        }

    @Test
    fun `user permission modification should be reflected`() =
        runTest {
            val viewModel =
                get<PermissionsViewModel>(
                    parameters = { parametersOf(RESOURCE_ID, PermissionsMode.VIEW, PermissionsItem.RESOURCE) },
                )

            val modifiedPermission =
                PermissionModelUi.UserPermissionModel(
                    ResourcePermission.OWNER,
                    "permId",
                    USER_PERMISSIONS[0].user.copy(),
                )
            viewModel.onIntent(UserPermissionModified(modifiedPermission))

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.permissions).contains(GROUP_PERMISSIONS[0])
                val userPermissions = state.permissions.filterIsInstance<PermissionModelUi.UserPermissionModel>()
                assertThat(userPermissions).hasSize(1)
                assertThat(userPermissions[0].permission).isEqualTo(ResourcePermission.OWNER)
            }
        }

    @Test
    fun `group permission modification should be reflected`() =
        runTest {
            val viewModel =
                get<PermissionsViewModel>(
                    parameters = { parametersOf(RESOURCE_ID, PermissionsMode.VIEW, PermissionsItem.RESOURCE) },
                )

            val modifiedPermission =
                PermissionModelUi.GroupPermissionModel(
                    ResourcePermission.OWNER,
                    "permId",
                    GROUP_PERMISSIONS[0].group.copy(),
                )
            viewModel.onIntent(GroupPermissionModified(modifiedPermission))

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.permissions).contains(USER_PERMISSIONS[0])
                val groupPermissions = state.permissions.filterIsInstance<PermissionModelUi.GroupPermissionModel>()
                assertThat(groupPermissions).hasSize(1)
                assertThat(groupPermissions[0].permission).isEqualTo(ResourcePermission.OWNER)
            }
        }

    @Test
    fun `should close with share success after successful share`() =
        runTest {
            val ownerPermissions = GROUP_PERMISSIONS + USER_PERMISSIONS[0].copy(permission = ResourcePermission.OWNER)
            get<GetLocalResourcePermissionsUseCase>().stub {
                onBlocking { execute(GetLocalResourcePermissionsUseCase.Input(RESOURCE_ID)) }
                    .doReturn(GetLocalResourcePermissionsUseCase.Output(ownerPermissions))
            }
            get<ResourceShareInteractor>().stub {
                onBlocking { simulateAndShareResource(any(), any()) }
                    .doReturn(ResourceShareInteractor.Output.Success)
            }
            get<HomeDataInteractor>().stub {
                onBlocking { refreshAllHomeScreenData() }
                    .doReturn(HomeDataInteractor.Output.Success)
            }
            get<ResourceTypeIdToSlugMappingProvider>().stub {
                onBlocking { provideMappingForSelectedAccount() }.doReturn(
                    mapOf(RESOURCE_TYPE_ID to ContentType.PasswordAndDescription.slug),
                )
            }

            val viewModel =
                get<PermissionsViewModel>(
                    parameters = { parametersOf(RESOURCE_ID, PermissionsMode.EDIT, PermissionsItem.RESOURCE) },
                )

            viewModel.sideEffect.test {
                viewModel.onIntent(MainButtonIntent)
                val effect = awaitItem()
                assertIs<PermissionsSideEffect.CloseWithShareSuccess>(effect)
            }
        }

    private companion object {
        private const val RESOURCE_ID = "resid"
        private val RESOURCE_TYPE_ID = UUID.randomUUID()
        private val FOLDER_ID = UUID.randomUUID()
        private val RESOURCE_MODEL by lazy {
            ResourceModel(
                resourceId = RESOURCE_ID,
                resourceTypeId = RESOURCE_TYPE_ID.toString(),
                folderId = FOLDER_ID.toString(),
                permission = ResourcePermission.READ,
                favouriteId = null,
                modified = ZonedDateTime.now(),
                expiry = null,
                metadataJsonModel =
                    MetadataJsonModel(
                        """{"name":"name","uri":"https://passbolt.com","username":"user","description":"desc"}""",
                    ),
                metadataKeyId = null,
                metadataKeyType = null,
            )
        }
        private val GROUP_PERMISSIONS =
            listOf(
                PermissionModelUi.GroupPermissionModel(
                    permission = ResourcePermission.READ,
                    permissionId = "groupPermId",
                    group = GroupModel(groupId = "groupId", groupName = "groupname"),
                ),
            )
        private val USER_PERMISSIONS =
            listOf(
                PermissionModelUi.UserPermissionModel(
                    permission = ResourcePermission.READ,
                    permissionId = "userPermId",
                    user =
                        UserWithAvatar(
                            userId = "userId",
                            firstName = "first",
                            lastName = "last",
                            userName = "userName",
                            isDisabled = false,
                            avatarUrl = "avatarUrl",
                        ),
                ),
            )
    }
}
