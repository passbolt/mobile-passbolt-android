package com.passbolt.mobile.android.otpmoremenu.compose

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.core.fulldatarefresh.FullDataRefreshExecutor
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.otpmoremenu.compose.OtpMoreMenuIntent.Close
import com.passbolt.mobile.android.otpmoremenu.compose.OtpMoreMenuIntent.CopyOtp
import com.passbolt.mobile.android.otpmoremenu.compose.OtpMoreMenuIntent.DeleteOtp
import com.passbolt.mobile.android.otpmoremenu.compose.OtpMoreMenuIntent.EditOtp
import com.passbolt.mobile.android.otpmoremenu.compose.OtpMoreMenuIntent.ShowOtp
import com.passbolt.mobile.android.otpmoremenu.compose.OtpMoreMenuSideEffect.Dismiss
import com.passbolt.mobile.android.otpmoremenu.compose.OtpMoreMenuSideEffect.InvokeCopyOtp
import com.passbolt.mobile.android.otpmoremenu.compose.OtpMoreMenuSideEffect.InvokeDeleteOtp
import com.passbolt.mobile.android.otpmoremenu.compose.OtpMoreMenuSideEffect.InvokeEditOtp
import com.passbolt.mobile.android.otpmoremenu.compose.OtpMoreMenuSideEffect.InvokeShowOtp
import com.passbolt.mobile.android.otpmoremenu.usecase.CreateOtpMoreMenuModelUseCase
import com.passbolt.mobile.android.ui.OtpMoreMenuModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.stub
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class)
class OtpMoreMenuViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single { mock<CreateOtpMoreMenuModelUseCase>() }
                        single { mock<FullDataRefreshExecutor>() }
                        singleOf(::TestCoroutineLaunchContext) bind CoroutineLaunchContext::class
                        factoryOf(::OtpMoreMenuViewModel)
                    },
                ),
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: OtpMoreMenuViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        val createOtpMoreMenuModelUseCase = get<CreateOtpMoreMenuModelUseCase>()
        createOtpMoreMenuModelUseCase.stub {
            onBlocking { execute(any()) } doReturn
                CreateOtpMoreMenuModelUseCase.Output(
                    OtpMoreMenuModel(
                        title = RESOURCE_NAME,
                        canDelete = true,
                        canEdit = true,
                    ),
                )
        }

        val fullDataRefreshExecutor = get<FullDataRefreshExecutor>()
        fullDataRefreshExecutor.stub {
            onBlocking { awaitFinish() } doSuspendableAnswer { }
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should update state when initialized`() =
        runTest {
            viewModel = get()

            viewModel.onIntent(
                OtpMoreMenuIntent.Initialize(
                    resourceId = RESOURCE_ID,
                    resourceName = RESOURCE_NAME,
                    canShowTotp = CAN_SHOW_TOTP,
                ),
            )

            viewModel.viewState.drop(1).test {
                val state = expectItem()
                assertThat(state.title).isEqualTo(RESOURCE_NAME)
                assertThat(state.showShowOtpButton).isEqualTo(CAN_SHOW_TOTP)
                assertThat(state.showDeleteButton).isTrue()
                assertThat(state.showEditButton).isTrue()
                assertThat(state.showSeparator).isTrue()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should emit side effects when corresponding intents are received`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(Close)
                assertThat(expectItem()).isEqualTo(Dismiss)

                viewModel.onIntent(CopyOtp)
                assertThat(expectItem()).isEqualTo(InvokeCopyOtp)

                viewModel.onIntent(DeleteOtp)
                assertThat(expectItem()).isEqualTo(InvokeDeleteOtp)

                viewModel.onIntent(EditOtp)
                assertThat(expectItem()).isEqualTo(InvokeEditOtp)

                viewModel.onIntent(ShowOtp)
                assertThat(expectItem()).isEqualTo(InvokeShowOtp)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should properly set separator visibility based on edit and delete permissions`() =
        runTest {
            val createOtpMoreMenuModelUseCase = get<CreateOtpMoreMenuModelUseCase>()
            createOtpMoreMenuModelUseCase.stub {
                onBlocking { execute(any()) } doReturn
                    CreateOtpMoreMenuModelUseCase.Output(
                        OtpMoreMenuModel(
                            title = RESOURCE_NAME,
                            canDelete = false,
                            canEdit = false,
                        ),
                    )
            }

            viewModel = get()

            viewModel.onIntent(
                OtpMoreMenuIntent.Initialize(
                    resourceId = RESOURCE_ID,
                    resourceName = RESOURCE_NAME,
                    canShowTotp = CAN_SHOW_TOTP,
                ),
            )

            viewModel.viewState.test {
                val state = expectItem()
                assertThat(state.showDeleteButton).isFalse()
                assertThat(state.showEditButton).isFalse()
                assertThat(state.showSeparator).isFalse()
            }
        }

    private companion object {
        private const val RESOURCE_ID = "resource-id"
        private const val RESOURCE_NAME = "Resource Name"
        private const val CAN_SHOW_TOTP = true
    }
}
