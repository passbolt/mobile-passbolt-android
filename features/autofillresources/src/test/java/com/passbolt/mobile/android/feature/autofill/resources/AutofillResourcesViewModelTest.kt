package com.passbolt.mobile.android.feature.autofill.resources

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.google.gson.GsonBuilder
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.spi.json.GsonJsonProvider
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider
import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.core.accounts.usecase.accounts.GetAccountsUseCase
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.resources.actions.SecretPropertiesActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.SecretPropertyActionResult
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourceUseCase
import com.passbolt.mobile.android.feature.autofill.resources.AutofillResourcesIntent.NewResourceCreated
import com.passbolt.mobile.android.feature.autofill.resources.AutofillResourcesIntent.SelectAutofillItem
import com.passbolt.mobile.android.feature.autofill.resources.AutofillResourcesIntent.UserAuthenticated
import com.passbolt.mobile.android.feature.autofill.resources.AutofillResourcesSideEffect.AutofillReturn
import com.passbolt.mobile.android.feature.autofill.resources.AutofillResourcesSideEffect.NavigateToAuth
import com.passbolt.mobile.android.feature.autofill.resources.AutofillResourcesSideEffect.NavigateToSetup
import com.passbolt.mobile.android.feature.autofill.resources.AutofillResourcesSideEffect.ShowToast
import com.passbolt.mobile.android.jsonmodel.JSON_MODEL_GSON
import com.passbolt.mobile.android.jsonmodel.jsonpathops.JsonPathJsonPathOps
import com.passbolt.mobile.android.jsonmodel.jsonpathops.JsonPathsOps
import com.passbolt.mobile.android.ui.MetadataJsonModel
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.ResourcePermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import java.time.ZonedDateTime
import java.util.EnumSet
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class AutofillResourcesViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single { mock<GetAccountsUseCase>() }
                        single { mock<GetLocalResourceUseCase>() }
                        single { mock<SecretPropertiesActionsInteractor>() }
                        singleOf(::TestCoroutineLaunchContext) bind CoroutineLaunchContext::class
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
                        factory { (uri: String?) ->
                            AutofillResourcesViewModel(
                                getAccountsUseCase = get(),
                                uri = uri,
                                getLocalResourceUseCase = get(),
                                coroutineLaunchContext = get(),
                            )
                        }
                    },
                ),
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should navigate to auth when accounts exist`() =
        runTest {
            val getAccountsUseCase: GetAccountsUseCase = get()
            whenever(getAccountsUseCase.execute(Unit)) doReturn
                GetAccountsUseCase.Output(users = setOf("user1"))

            val viewModel: AutofillResourcesViewModel = get { parametersOf(TEST_URI) }

            viewModel.sideEffect.test {
                val effect = awaitItem()
                assertThat(effect).isEqualTo(NavigateToAuth)
            }
        }

    @Test
    fun `should navigate to setup when no accounts exist`() =
        runTest {
            val getAccountsUseCase: GetAccountsUseCase = get()
            whenever(getAccountsUseCase.execute(Unit)) doReturn
                GetAccountsUseCase.Output(users = emptySet())

            val viewModel: AutofillResourcesViewModel = get { parametersOf(TEST_URI) }

            viewModel.sideEffect.test {
                val effect = awaitItem()
                assertThat(effect).isEqualTo(NavigateToSetup)
            }
        }

    @Test
    fun `should show home after user authenticated`() =
        runTest {
            val getAccountsUseCase: GetAccountsUseCase = get()
            whenever(getAccountsUseCase.execute(Unit)) doReturn
                GetAccountsUseCase.Output(users = setOf("user1"))

            val viewModel: AutofillResourcesViewModel = get { parametersOf(TEST_URI) }

            viewModel.sideEffect.test {
                assertIs<NavigateToAuth>(awaitItem())
            }

            viewModel.onIntent(UserAuthenticated)

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.showHome).isTrue()
            }
        }

    @Test
    fun `should return autofill data on successful item click`() =
        runTest {
            val getAccountsUseCase: GetAccountsUseCase = get()
            whenever(getAccountsUseCase.execute(Unit)) doReturn
                GetAccountsUseCase.Output(users = setOf("user1"))

            val secretPropertiesActionsInteractor: SecretPropertiesActionsInteractor = get()
            secretPropertiesActionsInteractor.stub {
                onBlocking { providePassword() } doReturn
                    flowOf(
                        SecretPropertyActionResult.Success(
                            label = "password",
                            isSecret = true,
                            result = TEST_PASSWORD,
                        ),
                    )
            }

            val viewModel: AutofillResourcesViewModel = get { parametersOf(TEST_URI) }

            viewModel.sideEffect.test {
                assertIs<NavigateToAuth>(awaitItem())

                viewModel.onIntent(SelectAutofillItem(testResource))

                val effect = assertIs<AutofillReturn>(awaitItem())
                assertThat(effect.username).isEqualTo(TEST_USERNAME)
                assertThat(effect.password).isEqualTo(TEST_PASSWORD)
                assertThat(effect.uri).isEqualTo(TEST_URI)
            }
        }

    @Test
    fun `should show fetch failure toast on fetch error`() =
        runTest {
            val getAccountsUseCase: GetAccountsUseCase = get()
            whenever(getAccountsUseCase.execute(Unit)) doReturn
                GetAccountsUseCase.Output(users = setOf("user1"))

            val secretPropertiesActionsInteractor: SecretPropertiesActionsInteractor = get()
            secretPropertiesActionsInteractor.stub {
                onBlocking { providePassword() } doReturn
                    flowOf(
                        SecretPropertyActionResult.FetchFailure(),
                    )
            }

            val viewModel: AutofillResourcesViewModel = get { parametersOf(TEST_URI) }

            viewModel.sideEffect.test {
                assertIs<NavigateToAuth>(awaitItem())

                viewModel.onIntent(SelectAutofillItem(testResource))

                val effect = assertIs<ShowToast>(awaitItem())
                assertThat(effect.type).isEqualTo(ToastType.FETCH_FAILURE)
            }
        }

    @Test
    fun `should show decryption failure toast on decryption error`() =
        runTest {
            val getAccountsUseCase: GetAccountsUseCase = get()
            whenever(getAccountsUseCase.execute(Unit)) doReturn
                GetAccountsUseCase.Output(users = setOf("user1"))

            val secretPropertiesActionsInteractor: SecretPropertiesActionsInteractor = get()
            secretPropertiesActionsInteractor.stub {
                onBlocking { providePassword() } doReturn
                    flowOf(
                        SecretPropertyActionResult.DecryptionFailure(),
                    )
            }

            val viewModel: AutofillResourcesViewModel = get { parametersOf(TEST_URI) }

            viewModel.sideEffect.test {
                assertIs<NavigateToAuth>(awaitItem())

                viewModel.onIntent(SelectAutofillItem(testResource))

                val effect = assertIs<ShowToast>(awaitItem())
                assertThat(effect.type).isEqualTo(ToastType.DECRYPTION_FAILURE)
            }
        }

    @Test
    fun `should show progress during item click and hide after`() =
        runTest {
            val getAccountsUseCase: GetAccountsUseCase = get()
            whenever(getAccountsUseCase.execute(Unit)) doReturn
                GetAccountsUseCase.Output(users = setOf("user1"))

            val secretPropertiesActionsInteractor: SecretPropertiesActionsInteractor = get()
            secretPropertiesActionsInteractor.stub {
                onBlocking { providePassword() } doReturn
                    flowOf(
                        SecretPropertyActionResult.Success(
                            label = "password",
                            isSecret = true,
                            result = TEST_PASSWORD,
                        ),
                    )
            }

            val viewModel: AutofillResourcesViewModel = get { parametersOf(TEST_URI) }

            viewModel.sideEffect.test {
                assertIs<NavigateToAuth>(awaitItem())
            }

            viewModel.onIntent(SelectAutofillItem(testResource))

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.showProgress).isFalse()
            }
        }

    @Test
    fun `should load resource and autofill on new resource created`() =
        runTest {
            val getAccountsUseCase: GetAccountsUseCase = get()
            whenever(getAccountsUseCase.execute(Unit)) doReturn
                GetAccountsUseCase.Output(users = setOf("user1"))

            val getLocalResourceUseCase: GetLocalResourceUseCase = get()
            getLocalResourceUseCase.stub {
                onBlocking { execute(GetLocalResourceUseCase.Input(TEST_RESOURCE_ID)) } doReturn
                    GetLocalResourceUseCase.Output(testResource)
            }

            val secretPropertiesActionsInteractor: SecretPropertiesActionsInteractor = get()
            secretPropertiesActionsInteractor.stub {
                onBlocking { providePassword() } doReturn
                    flowOf(
                        SecretPropertyActionResult.Success(
                            label = "password",
                            isSecret = true,
                            result = TEST_PASSWORD,
                        ),
                    )
            }

            val viewModel: AutofillResourcesViewModel = get { parametersOf(TEST_URI) }

            viewModel.sideEffect.test {
                assertIs<NavigateToAuth>(awaitItem())

                viewModel.onIntent(NewResourceCreated(TEST_RESOURCE_ID))

                val effect = assertIs<AutofillReturn>(awaitItem())
                assertThat(effect.username).isEqualTo(TEST_USERNAME)
                assertThat(effect.password).isEqualTo(TEST_PASSWORD)
            }
        }

    private companion object {
        private const val TEST_URI = "https://example.com"
        private const val TEST_PASSWORD = "secretPassword"
        private const val TEST_USERNAME = "testuser"
        private const val TEST_RESOURCE_ID = "resourceId"

        private val testResource by lazy {
            ResourceModel(
                resourceId = TEST_RESOURCE_ID,
                resourceTypeId = "resTypeId",
                folderId = null,
                permission = ResourcePermission.READ,
                favouriteId = null,
                modified = ZonedDateTime.now(),
                expiry = null,
                metadataJsonModel =
                    MetadataJsonModel(
                        """
                        {
                            "name": "Test Resource",
                            "uri": "$TEST_URI",
                            "username": "$TEST_USERNAME",
                            "description": "Test description"
                        }
                        """.trimIndent(),
                    ),
                metadataKeyId = null,
                metadataKeyType = null,
            )
        }
    }
}
