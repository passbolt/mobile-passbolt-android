package com.passbolt.mobile.android.core.commonfolders.usecase

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState
import com.passbolt.mobile.android.core.networking.MfaStatus
import com.passbolt.mobile.android.core.networking.NetworkResult
import com.passbolt.mobile.android.dto.PassphraseNotInCacheException
import com.passbolt.mobile.android.dto.response.BasePaginatedResponse
import com.passbolt.mobile.android.dto.response.FolderResponseDto
import com.passbolt.mobile.android.dto.response.HeaderWithPaginationResponse
import com.passbolt.mobile.android.dto.response.Pagination
import com.passbolt.mobile.android.dto.response.StatusResponse
import com.passbolt.mobile.android.mappers.FolderModelMapper
import com.passbolt.mobile.android.passboltapi.folders.FoldersRepository
import com.passbolt.mobile.android.ui.FolderModel
import com.passbolt.mobile.android.ui.FolderModelWithAttributes
import com.passbolt.mobile.android.ui.ResourcePermission
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.koin.test.inject
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import java.net.HttpURLConnection
import kotlin.test.assertIs

@ExperimentalCoroutinesApi
class GetFoldersPaginatedUseCaseTest : KoinTest {
    private val useCase: GetFoldersPaginatedUseCase by inject()

    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                module {
                    single { mock<FoldersRepository>() }
                    single { mock<FolderModelMapper>() }
                    singleOf(::GetFoldersPaginatedUseCase)
                },
            )
        }

    @Test
    fun `should return success with mapped folders and pagination`() =
        runTest {
            val pagination = Pagination(count = 10, page = 1, limit = 2000)
            val folderDto = mock<FolderResponseDto>()
            stubRepositorySuccess(listOf(folderDto), pagination)
            get<FolderModelMapper>().stub {
                on { map(any<FolderResponseDto>()) }.doReturn(FOLDER_WITH_ATTRIBUTES)
            }

            val result = useCase.execute(GetFoldersPaginatedUseCase.Input(page = 1, limit = 2000))

            val success = assertIs<GetFoldersPaginatedUseCase.Output.Success>(result)
            assertThat(success.pagination.count).isEqualTo(10)
            assertThat(success.folders).hasSize(1)
            assertThat(success.authenticationState).isEqualTo(AuthenticationState.Authenticated)
        }

    @Test
    fun `should return failure on network error`() =
        runTest {
            get<FoldersRepository>().stub {
                onBlocking { getFoldersPaginated(any(), any()) }.doReturn(
                    NetworkResult.Failure.NetworkError(
                        exception = Exception("timeout"),
                        headerMessage = "Error",
                    ),
                )
            }

            val result = useCase.execute(GetFoldersPaginatedUseCase.Input(page = 1, limit = 2000))

            assertIs<GetFoldersPaginatedUseCase.Output.Failure<*>>(result)
            assertThat(result.authenticationState).isEqualTo(AuthenticationState.Authenticated)
        }

    @Test
    fun `should return session unauthenticated on unauthorized response`() =
        runTest {
            get<FoldersRepository>().stub {
                onBlocking { getFoldersPaginated(any(), any()) }.doReturn(
                    NetworkResult.Failure.ServerError(
                        exception = Exception("Unauthorized"),
                        errorCode = HttpURLConnection.HTTP_UNAUTHORIZED,
                        headerMessage = "Unauthorized",
                    ),
                )
            }

            val result = useCase.execute(GetFoldersPaginatedUseCase.Input(page = 1, limit = 2000))

            assertIs<GetFoldersPaginatedUseCase.Output.Failure<*>>(result)
            val unauthenticated = assertIs<AuthenticationState.Unauthenticated>(result.authenticationState)
            assertIs<AuthenticationState.Unauthenticated.Reason.Session>(unauthenticated.reason)
        }

    @Test
    fun `should return passphrase unauthenticated when passphrase not in cache`() =
        runTest {
            get<FoldersRepository>().stub {
                onBlocking { getFoldersPaginated(any(), any()) }.doReturn(
                    NetworkResult.Failure.NetworkError(
                        exception = PassphraseNotInCacheException(),
                        headerMessage = "Error",
                    ),
                )
            }

            val result = useCase.execute(GetFoldersPaginatedUseCase.Input(page = 1, limit = 2000))

            assertIs<GetFoldersPaginatedUseCase.Output.Failure<*>>(result)
            val unauthenticated = assertIs<AuthenticationState.Unauthenticated>(result.authenticationState)
            assertIs<AuthenticationState.Unauthenticated.Reason.Passphrase>(unauthenticated.reason)
        }

    @Test
    fun `should return mfa unauthenticated when mfa is required`() =
        runTest {
            get<FoldersRepository>().stub {
                onBlocking { getFoldersPaginated(any(), any()) }.doReturn(
                    NetworkResult.Failure.ServerError(
                        exception = Exception("MFA required"),
                        errorCode = HttpURLConnection.HTTP_FORBIDDEN,
                        headerMessage = "MFA required",
                        mfaStatus = MfaStatus.Required(listOf(null)),
                    ),
                )
            }

            val result = useCase.execute(GetFoldersPaginatedUseCase.Input(page = 1, limit = 2000))

            assertIs<GetFoldersPaginatedUseCase.Output.Failure<*>>(result)
            val unauthenticated = assertIs<AuthenticationState.Unauthenticated>(result.authenticationState)
            assertIs<AuthenticationState.Unauthenticated.Reason.Mfa>(unauthenticated.reason)
        }

    private fun stubRepositorySuccess(
        folders: List<FolderResponseDto>,
        pagination: Pagination,
    ) {
        get<FoldersRepository>().stub {
            onBlocking { getFoldersPaginated(any(), any()) }.doReturn(
                NetworkResult.Success(
                    BasePaginatedResponse(
                        header =
                            HeaderWithPaginationResponse(
                                id = "id",
                                status = StatusResponse.COMPLETE,
                                message = "success",
                                url = "/folders.json",
                                code = 200,
                                serverTime = 0L,
                                pagination = pagination,
                            ),
                        body = folders,
                    ),
                ),
            )
        }
    }

    private companion object {
        private val FOLDER_WITH_ATTRIBUTES =
            FolderModelWithAttributes(
                folderModel =
                    FolderModel(
                        folderId = "folderId",
                        parentFolderId = null,
                        name = "Test Folder",
                        isShared = false,
                        permission = ResourcePermission.READ,
                    ),
                folderPermissions = emptyList(),
            )
    }
}
