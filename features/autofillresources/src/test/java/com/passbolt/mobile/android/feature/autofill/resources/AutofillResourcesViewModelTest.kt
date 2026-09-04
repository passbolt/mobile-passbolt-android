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

package com.passbolt.mobile.android.feature.autofill.resources

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.google.gson.GsonBuilder
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.spi.json.GsonJsonProvider
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider
import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.otpcore.TotpParametersProvider
import com.passbolt.mobile.android.domain.accounts.usecase.GetAccountsUseCase
import com.passbolt.mobile.android.domain.preferences.usecase.GetGlobalPreferencesUseCase
import com.passbolt.mobile.android.domain.resources.actions.SecretPropertiesActionsInteractor
import com.passbolt.mobile.android.domain.resources.actions.SecretPropertyActionResult
import com.passbolt.mobile.android.domain.resources.usecase.db.GetLocalResourceUseCase
import com.passbolt.mobile.android.domain.secrets.model.SecretJsonModel
import com.passbolt.mobile.android.feature.autofill.resources.AutofillResourcesIntent.NewResourceCreated
import com.passbolt.mobile.android.feature.autofill.resources.AutofillResourcesIntent.SelectAutofillItem
import com.passbolt.mobile.android.feature.autofill.resources.AutofillResourcesIntent.UserAuthenticated
import com.passbolt.mobile.android.feature.autofill.resources.AutofillResourcesSideEffect.AutofillReturn
import com.passbolt.mobile.android.feature.autofill.resources.AutofillResourcesSideEffect.NavigateToAuth
import com.passbolt.mobile.android.feature.autofill.resources.AutofillResourcesSideEffect.NavigateToSetup
import com.passbolt.mobile.android.feature.autofill.resources.AutofillResourcesSideEffect.ShowToast
import com.passbolt.mobile.android.jsonmodel.JSON_MODEL_GSON
import com.passbolt.mobile.android.jsonmodel.delegates.TotpSecret
import com.passbolt.mobile.android.jsonmodel.jsonpathops.JsonPathJsonPathOps
import com.passbolt.mobile.android.jsonmodel.jsonpathops.JsonPathsOps
import com.passbolt.mobile.android.ui.MetadataJsonModel
import com.passbolt.mobile.android.ui.ResourcePermission
import com.passbolt.mobile.android.ui.ResourceUiModel
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
                        single { mock<GetGlobalPreferencesUseCase>() }
                        single { mock<SecretPropertiesActionsInteractor>() }
                        single { mock<TotpParametersProvider>() }
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
                                autofillType = null,
                                getGlobalPreferencesUseCase = get(),
                                getLocalResourceUseCase = get(),
                                totpParametersProvider = get(),
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

    private fun stubDecryptedSecret(result: SecretPropertyActionResult<SecretJsonModel>) {
        val secretPropertiesActionsInteractor: SecretPropertiesActionsInteractor = get()
        secretPropertiesActionsInteractor.stub {
            onBlocking { provideDecryptedSecret() } doReturn flowOf(result)
        }
    }

    private fun stubDecryptedSecret(secret: SecretJsonModel) =
        stubDecryptedSecret(
            SecretPropertyActionResult.Success(
                label = "JSON Secret",
                isSecret = true,
                result = secret,
            ),
        )

    @Test
    fun `should navigate to auth when accounts exist`() =
        runTest {
            val getAccountsUseCase: GetAccountsUseCase = get()
            whenever(getAccountsUseCase.execute(Unit)) doReturn
                GetAccountsUseCase.Output(users = setOf("user1"))

            val viewModel: AutofillResourcesViewModel =
                get { parametersOf(TEST_URI) }

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

            val viewModel: AutofillResourcesViewModel =
                get { parametersOf(TEST_URI) }

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

            val viewModel: AutofillResourcesViewModel =
                get { parametersOf(TEST_URI) }

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

            stubDecryptedSecret(passwordSecretJson())

            val viewModel: AutofillResourcesViewModel =
                get { parametersOf(TEST_URI) }

            viewModel.sideEffect.test {
                assertIs<NavigateToAuth>(awaitItem())

                viewModel.onIntent(SelectAutofillItem(testResource))

                val effect = assertIs<AutofillReturn>(awaitItem())
                val payload = effect.payload
                assertThat(payload.username).isEqualTo(TEST_USERNAME)
                assertThat(payload.password).isEqualTo(TEST_PASSWORD)
                assertThat(payload.totpCode).isNull()
                assertThat(payload.uri).isEqualTo(TEST_URI)
            }
        }

    @Test
    fun `should show fetch failure toast on fetch error`() =
        runTest {
            val getAccountsUseCase: GetAccountsUseCase = get()
            whenever(getAccountsUseCase.execute(Unit)) doReturn
                GetAccountsUseCase.Output(users = setOf("user1"))

            stubDecryptedSecret(SecretPropertyActionResult.FetchFailure())

            val viewModel: AutofillResourcesViewModel =
                get { parametersOf(TEST_URI) }

            viewModel.sideEffect.test {
                assertIs<NavigateToAuth>(awaitItem())

                viewModel.onIntent(SelectAutofillItem(testResource))

                val toast = assertIs<ShowToast>(awaitItem())
                assertThat(toast.type).isEqualTo(ToastType.FETCH_FAILURE)

                val effect = assertIs<AutofillReturn>(awaitItem())
                val payload = effect.payload
                assertThat(payload.username).isEqualTo(TEST_USERNAME)
                assertThat(payload.password).isNull()
                assertThat(payload.totpCode).isNull()
                assertThat(payload.uri).isEqualTo(TEST_URI)
            }
        }

    @Test
    fun `should show decryption failure toast on decryption error`() =
        runTest {
            val getAccountsUseCase: GetAccountsUseCase = get()
            whenever(getAccountsUseCase.execute(Unit)) doReturn
                GetAccountsUseCase.Output(users = setOf("user1"))

            stubDecryptedSecret(SecretPropertyActionResult.DecryptionFailure())

            val viewModel: AutofillResourcesViewModel =
                get { parametersOf(TEST_URI) }

            viewModel.sideEffect.test {
                assertIs<NavigateToAuth>(awaitItem())

                viewModel.onIntent(SelectAutofillItem(testResource))

                val toast = assertIs<ShowToast>(awaitItem())
                assertThat(toast.type).isEqualTo(ToastType.DECRYPTION_FAILURE)

                val effect = assertIs<AutofillReturn>(awaitItem())
                val payload = effect.payload
                assertThat(payload.username).isEqualTo(TEST_USERNAME)
                assertThat(payload.password).isNull()
                assertThat(payload.totpCode).isNull()
                assertThat(payload.uri).isEqualTo(TEST_URI)
            }
        }

    @Test
    fun `should show progress during item click and hide after`() =
        runTest {
            val getAccountsUseCase: GetAccountsUseCase = get()
            whenever(getAccountsUseCase.execute(Unit)) doReturn
                GetAccountsUseCase.Output(users = setOf("user1"))

            stubDecryptedSecret(passwordSecretJson())

            val viewModel: AutofillResourcesViewModel =
                get { parametersOf(TEST_URI) }

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

            stubDecryptedSecret(passwordSecretJson())

            val viewModel: AutofillResourcesViewModel =
                get { parametersOf(TEST_URI) }

            viewModel.sideEffect.test {
                assertIs<NavigateToAuth>(awaitItem())

                viewModel.onIntent(NewResourceCreated(TEST_RESOURCE_ID))

                val effect = assertIs<AutofillReturn>(awaitItem())
                val payload = effect.payload
                assertThat(payload.username).isEqualTo(TEST_USERNAME)
                assertThat(payload.password).isEqualTo(TEST_PASSWORD)
            }
        }

    @Test
    fun `should return TOTP-only payload for TOTP-only resource`() =
        runTest {
            val getAccountsUseCase: GetAccountsUseCase = get()
            whenever(getAccountsUseCase.execute(Unit)) doReturn
                GetAccountsUseCase.Output(users = setOf("user1"))

            stubDecryptedSecret(totpSecretJson())

            val totpParametersProvider: TotpParametersProvider = get()
            whenever(
                totpParametersProvider.provideOtpParameters(
                    secretKey = TEST_TOTP_SECRET.key,
                    digits = TEST_TOTP_SECRET.digits,
                    period = TEST_TOTP_SECRET.period,
                    algorithm = TEST_TOTP_SECRET.algorithm,
                ),
            ) doReturn
                TotpParametersProvider.OtpParametersResult.OtpParameters(
                    otpValue = TEST_TOTP_CODE,
                    secondsValid = 30,
                )

            val viewModel: AutofillResourcesViewModel =
                get { parametersOf(TEST_URI) }

            viewModel.sideEffect.test {
                assertIs<NavigateToAuth>(awaitItem())

                viewModel.onIntent(SelectAutofillItem(testTotpResource))

                val effect = assertIs<AutofillReturn>(awaitItem())
                val payload = effect.payload
                assertThat(payload.username).isNull()
                assertThat(payload.password).isNull()
                assertThat(payload.totpCode).isEqualTo(TEST_TOTP_CODE)
                assertThat(payload.uri).isEqualTo(TEST_URI)
            }
        }

    @Test
    fun `should return full payload when resource has both password and TOTP`() =
        runTest {
            val getAccountsUseCase: GetAccountsUseCase = get()
            whenever(getAccountsUseCase.execute(Unit)) doReturn
                GetAccountsUseCase.Output(users = setOf("user1"))

            stubDecryptedSecret(passwordAndTotpSecretJson())

            val totpParametersProvider: TotpParametersProvider = get()
            whenever(
                totpParametersProvider.provideOtpParameters(
                    secretKey = TEST_TOTP_SECRET.key,
                    digits = TEST_TOTP_SECRET.digits,
                    period = TEST_TOTP_SECRET.period,
                    algorithm = TEST_TOTP_SECRET.algorithm,
                ),
            ) doReturn
                TotpParametersProvider.OtpParametersResult.OtpParameters(
                    otpValue = TEST_TOTP_CODE,
                    secondsValid = 30,
                )

            val viewModel: AutofillResourcesViewModel =
                get { parametersOf(TEST_URI) }

            viewModel.sideEffect.test {
                assertIs<NavigateToAuth>(awaitItem())

                viewModel.onIntent(SelectAutofillItem(testPasswordDescriptionTotpResource))

                val effect = assertIs<AutofillReturn>(awaitItem())
                val payload = effect.payload
                assertThat(payload.username).isEqualTo(TEST_USERNAME)
                assertThat(payload.password).isEqualTo(TEST_PASSWORD)
                assertThat(payload.totpCode).isEqualTo(TEST_TOTP_CODE)
                assertThat(payload.uri).isEqualTo(TEST_URI)
            }
        }

    @Test
    fun `should return credentials-only payload when resource has no TOTP`() =
        runTest {
            val getAccountsUseCase: GetAccountsUseCase = get()
            whenever(getAccountsUseCase.execute(Unit)) doReturn
                GetAccountsUseCase.Output(users = setOf("user1"))

            stubDecryptedSecret(passwordSecretJson())

            val viewModel: AutofillResourcesViewModel =
                get { parametersOf(TEST_URI) }

            viewModel.sideEffect.test {
                assertIs<NavigateToAuth>(awaitItem())

                viewModel.onIntent(SelectAutofillItem(testResource))

                val effect = assertIs<AutofillReturn>(awaitItem())
                val payload = effect.payload
                assertThat(payload.username).isEqualTo(TEST_USERNAME)
                assertThat(payload.password).isEqualTo(TEST_PASSWORD)
                assertThat(payload.totpCode).isNull()
                assertThat(payload.uri).isEqualTo(TEST_URI)
            }
        }

    @Test
    fun `should show toast on invalid TOTP input`() =
        runTest {
            val getAccountsUseCase: GetAccountsUseCase = get()
            whenever(getAccountsUseCase.execute(Unit)) doReturn
                GetAccountsUseCase.Output(users = setOf("user1"))

            stubDecryptedSecret(totpSecretJson())

            val totpParametersProvider: TotpParametersProvider = get()
            whenever(
                totpParametersProvider.provideOtpParameters(
                    secretKey = TEST_TOTP_SECRET.key,
                    digits = TEST_TOTP_SECRET.digits,
                    period = TEST_TOTP_SECRET.period,
                    algorithm = TEST_TOTP_SECRET.algorithm,
                ),
            ) doReturn TotpParametersProvider.OtpParametersResult.InvalidTotpInput

            val viewModel: AutofillResourcesViewModel =
                get { parametersOf(TEST_URI) }

            viewModel.sideEffect.test {
                assertIs<NavigateToAuth>(awaitItem())

                viewModel.onIntent(SelectAutofillItem(testTotpResource))

                val effect = assertIs<ShowToast>(awaitItem())
                assertThat(effect.type).isEqualTo(ToastType.INVALID_TOTP_PARAMETERS)
            }
        }

    private companion object {
        private const val TEST_URI = "https://example.com"
        private const val TEST_PASSWORD = "secretPassword"
        private const val TEST_USERNAME = "testuser"
        private const val TEST_RESOURCE_ID = "resourceId"
        private const val TEST_TOTP_CODE = "123456"

        private val TEST_TOTP_SECRET =
            TotpSecret(
                algorithm = "SHA1",
                key = "JBSWY3DPEHPK3PXP",
                digits = 6,
                period = 30L,
            )

        private fun passwordSecretJson() = SecretJsonModel("""{"password": "$TEST_PASSWORD"}""")

        private fun totpSecretJson() =
            SecretJsonModel(
                """
                {
                    "totp": {
                        "secret_key": "${TEST_TOTP_SECRET.key}",
                        "algorithm": "${TEST_TOTP_SECRET.algorithm}",
                        "digits": ${TEST_TOTP_SECRET.digits},
                        "period": ${TEST_TOTP_SECRET.period}
                    }
                }
                """.trimIndent(),
            )

        private fun passwordAndTotpSecretJson() =
            SecretJsonModel(
                """
                {
                    "password": "$TEST_PASSWORD",
                    "totp": {
                        "secret_key": "${TEST_TOTP_SECRET.key}",
                        "algorithm": "${TEST_TOTP_SECRET.algorithm}",
                        "digits": ${TEST_TOTP_SECRET.digits},
                        "period": ${TEST_TOTP_SECRET.period}
                    }
                }
                """.trimIndent(),
            )

        private val testResource by lazy {
            ResourceUiModel(
                resourceId = TEST_RESOURCE_ID,
                resourceTypeId = "resTypeId",
                slug = "password-and-description",
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

        private val testTotpResource by lazy {
            ResourceUiModel(
                resourceId = TEST_RESOURCE_ID,
                resourceTypeId = "resTypeId",
                slug = "totp",
                folderId = null,
                permission = ResourcePermission.READ,
                favouriteId = null,
                modified = ZonedDateTime.now(),
                expiry = null,
                metadataJsonModel =
                    MetadataJsonModel(
                        """
                        {
                            "name": "Test TOTP Resource",
                            "uri": "$TEST_URI",
                            "username": null,
                            "description": null
                        }
                        """.trimIndent(),
                    ),
                metadataKeyId = null,
                metadataKeyType = null,
            )
        }

        private val testPasswordDescriptionTotpResource by lazy {
            ResourceUiModel(
                resourceId = TEST_RESOURCE_ID,
                resourceTypeId = "resTypeId",
                slug = "password-description-totp",
                folderId = null,
                permission = ResourcePermission.READ,
                favouriteId = null,
                modified = ZonedDateTime.now(),
                expiry = null,
                metadataJsonModel =
                    MetadataJsonModel(
                        """
                        {
                            "name": "Test Combined Resource",
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
