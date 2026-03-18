package com.passbolt.mobile.android.feature.resourceform.additionalsecrets.password

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.passwordgenerator.SecretGenerator
import com.passbolt.mobile.android.core.passwordgenerator.codepoints.toCodepoints
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.password.PasswordFormSideEffect.ApplyAndGoBack
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.password.PasswordFormSideEffect.NavigateBack
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.password.PasswordFormSideEffect.ShowUnableToGeneratePassword
import com.passbolt.mobile.android.ui.CaseTypeModel
import com.passbolt.mobile.android.ui.LeadingContentType
import com.passbolt.mobile.android.ui.PassphraseGeneratorSettingsModel
import com.passbolt.mobile.android.ui.PasswordGeneratorSettingsModel
import com.passbolt.mobile.android.ui.PasswordGeneratorTypeModel
import com.passbolt.mobile.android.ui.PasswordPolicies
import com.passbolt.mobile.android.ui.PasswordStrength.Empty
import com.passbolt.mobile.android.ui.PasswordStrength.VeryStrong
import com.passbolt.mobile.android.ui.PasswordStrength.Weak
import com.passbolt.mobile.android.ui.PasswordUiModel
import com.passbolt.mobile.android.ui.ResourceFormMode
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
import org.koin.core.parameter.parametersOf
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.kotlin.any
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class PasswordFormViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(testPasswordFormModule)
        }

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: PasswordFormViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initialize should show correct state`() =
        runTest {
            mockEntropyCalculator.stub {
                onBlocking { getSecretEntropy(any()) }.thenReturn(0.0)
            }

            viewModel =
                get(
                    parameters = {
                        parametersOf(resourceFormMode, password)
                    },
                )

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.resourceFormMode).isEqualTo(resourceFormMode)
                assertThat(state.password).isEqualTo(MOCK_PASSWORD)
                assertThat(state.mainUri).isEqualTo(MOCK_MAIN_URI)
                assertThat(state.username).isEqualTo(MOCK_USERNAME)
                assertThat(state.entropy).isEqualTo(0.0)
                assertThat(state.passwordStrength).isEqualTo(Empty)
            }
        }

    @Test
    fun `password text change should trigger entropy recalculation`() =
        runTest {
            mockEntropyCalculator.stub {
                onBlocking { getSecretEntropy(any()) }.thenReturn(0.0)
            }

            viewModel =
                get(
                    parameters = {
                        parametersOf(resourceFormMode, password)
                    },
                )

            mockEntropyCalculator.stub {
                onBlocking { getSecretEntropy("test") }.thenReturn(65.0)
            }

            viewModel.onIntent(PasswordFormIntent.PasswordTextChanged("test"))

            viewModel.viewState.drop(1).test {
                val state = awaitItem()
                assertThat(state.password).isEqualTo("test")
                assertThat(state.entropy).isEqualTo(65.0)
                assertThat(state.passwordStrength).isEqualTo(Weak)
            }
        }

    @Test
    fun `main uri text change should update state`() =
        runTest {
            mockEntropyCalculator.stub {
                onBlocking { getSecretEntropy(any()) }.thenReturn(0.0)
            }

            viewModel =
                get(
                    parameters = {
                        parametersOf(resourceFormMode, password)
                    },
                )
            viewModel.onIntent(PasswordFormIntent.MainUriTextChanged("new uri"))

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.mainUri).isEqualTo("new uri")
            }
        }

    @Test
    fun `username text change should update state`() =
        runTest {
            mockEntropyCalculator.stub {
                onBlocking { getSecretEntropy(any()) }.thenReturn(0.0)
            }

            viewModel =
                get(
                    parameters = {
                        parametersOf(resourceFormMode, password)
                    },
                )
            viewModel.onIntent(PasswordFormIntent.UsernameTextChanged("new username"))

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.username).isEqualTo("new username")
            }
        }

    @Test
    fun `apply should emit ApplyAndGoBack side effect with correct model`() =
        runTest {
            mockEntropyCalculator.stub {
                onBlocking { getSecretEntropy(any()) }.thenReturn(0.0)
            }

            viewModel =
                get(
                    parameters = {
                        parametersOf(resourceFormMode, password)
                    },
                )
            viewModel.onIntent(PasswordFormIntent.PasswordTextChanged("changed password"))
            viewModel.onIntent(PasswordFormIntent.MainUriTextChanged("changed uri"))
            viewModel.onIntent(PasswordFormIntent.UsernameTextChanged("changed username"))

            viewModel.sideEffect.test {
                viewModel.onIntent(PasswordFormIntent.ApplyChanges)

                val sideEffect = awaitItem()
                assertIs<ApplyAndGoBack>(sideEffect)
                with(sideEffect.model) {
                    assertThat(password).isEqualTo("changed password")
                    assertThat(mainUri).isEqualTo("changed uri")
                    assertThat(username).isEqualTo("changed username")
                }
            }
        }

    @Test
    fun `go back should emit NavigateUp side effect`() =
        runTest {
            viewModel =
                get(
                    parameters = {
                        parametersOf(resourceFormMode, password)
                    },
                )

            viewModel.sideEffect.test {
                viewModel.onIntent(PasswordFormIntent.GoBack)
                assertIs<NavigateBack>(awaitItem())
            }
        }

    @Test
    fun `generate password success should update state`() =
        runTest {
            mockEntropyCalculator.stub {
                onBlocking { getSecretEntropy(any()) }.thenReturn(0.0)
            }
            val generatedCodepoints = "generated123!".toCodepoints()
            whenever(mockGetPasswordPoliciesUseCase.execute(any())).thenReturn(
                defaultPasswordPolicies,
            )
            whenever(mockSecretGenerator.generatePassword(any())).thenReturn(
                SecretGenerator.SecretGenerationResult.Success(generatedCodepoints, 130.0),
            )

            viewModel =
                get(
                    parameters = {
                        parametersOf(resourceFormMode, password)
                    },
                )

            viewModel.onIntent(PasswordFormIntent.GeneratePassword)

            viewModel.viewState.drop(1).test {
                val state = awaitItem()
                assertThat(state.password).isEqualTo("generated123!")
                assertThat(state.entropy).isEqualTo(130.0)
                assertThat(state.passwordStrength).isEqualTo(VeryStrong)
            }
        }

    @Test
    fun `generate password failure should emit ShowUnableToGeneratePassword side effect`() =
        runTest {
            mockEntropyCalculator.stub {
                onBlocking { getSecretEntropy(any()) }.thenReturn(0.0)
            }
            whenever(mockGetPasswordPoliciesUseCase.execute(any())).thenReturn(
                defaultPasswordPolicies,
            )
            whenever(mockSecretGenerator.generatePassword(any())).thenReturn(
                SecretGenerator.SecretGenerationResult.FailedToGenerateLowEntropy(80),
            )

            viewModel =
                get(
                    parameters = {
                        parametersOf(resourceFormMode, password)
                    },
                )

            viewModel.viewState.drop(1).test { }
            viewModel.sideEffect.test {
                viewModel.onIntent(PasswordFormIntent.GeneratePassword)
                testScheduler.advanceUntilIdle()

                val sideEffect = awaitItem()
                assertIs<ShowUnableToGeneratePassword>(sideEffect)
                assertThat(sideEffect.minimumEntropyBits).isEqualTo(80)
            }
        }

    private companion object {
        private const val MOCK_PASSWORD = "mock password"
        private const val MOCK_MAIN_URI = "mock main uri"
        private const val MOCK_USERNAME = "mock username"

        private val password =
            PasswordUiModel(
                password = MOCK_PASSWORD,
                mainUri = MOCK_MAIN_URI,
                username = MOCK_USERNAME,
            )

        private val resourceFormMode =
            ResourceFormMode.Create(
                leadingContentType = LeadingContentType.PASSWORD,
                parentFolderId = null,
            )

        private val defaultPasswordPolicies =
            PasswordPolicies(
                defaultGenerator = PasswordGeneratorTypeModel.PASSWORD,
                passwordGeneratorSettings =
                    PasswordGeneratorSettingsModel(
                        length = 18,
                        maskUpper = true,
                        maskLower = true,
                        maskDigit = true,
                        maskParenthesis = true,
                        maskEmoji = false,
                        maskChar1 = true,
                        maskChar2 = true,
                        maskChar3 = true,
                        maskChar4 = true,
                        maskChar5 = true,
                        excludeLookAlikeChars = true,
                    ),
                passphraseGeneratorSettings =
                    PassphraseGeneratorSettingsModel(
                        words = 9,
                        wordSeparator = " ",
                        wordCase = CaseTypeModel.LOWERCASE,
                    ),
                isExternalDictionaryCheckEnabled = true,
            )
    }
}
