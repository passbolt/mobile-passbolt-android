package com.passbolt.mobile.android.feature.resourceform.main

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.passwordgenerator.SecretGenerator
import com.passbolt.mobile.android.core.passwordgenerator.codepoints.Codepoint
import com.passbolt.mobile.android.core.resources.usecase.GetDefaultCreateContentTypeUseCase
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.note.NoteValidationError
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpSecretValidationError
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.DismissMetadataKeyDialog
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.ExpandAdvancedSettings
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.GeneratePassword
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.GoBack
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.GoToAdditionalNote
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.GoToAdditionalPassword
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.GoToAdditionalTotp
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.GoToAdditionalUris
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.GoToAppearance
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.GoToCustomFields
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.GoToMetadataDescription
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.GoToTotpMoreSettings
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.NameTextChanged
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.NoteChanged
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.PasswordMainUriTextChanged
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.PasswordTextChanged
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.PasswordUsernameTextChanged
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.ScanOtpResult
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.ScanTotp
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.TotpSecretChanged
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.TotpUrlChanged
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateBack
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateToAdditionalUris
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateToAppearance
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateToDescription
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateToNote
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateToPassword
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateToScanOtp
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateToTotp
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateToTotpAdvancedSettings
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.ShowToast
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
import com.passbolt.mobile.android.ui.LeadingContentType
import com.passbolt.mobile.android.ui.MetadataTypeModel
import com.passbolt.mobile.android.ui.OtpParseResult
import com.passbolt.mobile.android.ui.PasswordGeneratorTypeModel
import com.passbolt.mobile.android.ui.PasswordStrength
import com.passbolt.mobile.android.ui.ResourceFormMode
import com.passbolt.mobile.android.ui.ResourceFormUiModel.Metadata.ADDITIONAL_URIS
import com.passbolt.mobile.android.ui.ResourceFormUiModel.Metadata.APPEARANCE
import com.passbolt.mobile.android.ui.ResourceFormUiModel.Metadata.DESCRIPTION
import com.passbolt.mobile.android.ui.ResourceFormUiModel.Secret.NOTE
import com.passbolt.mobile.android.ui.ResourceFormUiModel.Secret.PASSWORD
import com.passbolt.mobile.android.ui.ResourceFormUiModel.Secret.TOTP
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
import kotlin.test.assertIs

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

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("LargeClass")
class ResourceFormViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(testResourceFormModule)
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
    fun `view should show correct ui for create totp`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                onBlocking { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = ContentType.V5TotpStandalone,
                    ),
                )
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.TOTP,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }

            advanceUntilIdle()

            val state = viewModel.viewState.value
            assertThat(state.shouldShowScreenProgress).isFalse()
            assertThat(state.name).isEqualTo("")
            assertThat(state.leadingContentType).isEqualTo(LeadingContentType.TOTP)
            assertThat(state.isPrimaryButtonVisible).isTrue()
            assertThat(state.totpData.totpIssuer).isEqualTo("")
            assertThat(state.totpData.totpSecret).isEqualTo("")
            assertThat(state.totpData.totpUiModel).isNotNull()
            assertThat(state.totpData.totpUiModel!!.secret).isEqualTo("")
            assertThat(state.totpData.totpUiModel.issuer).isEqualTo("")
            assertThat(state.totpData.totpUiModel.algorithm).isEqualTo(OtpParseResult.OtpQr.Algorithm.DEFAULT.name)
            assertThat(state.totpData.totpUiModel.expiry).isEqualTo(
                OtpParseResult.OtpQr.TotpQr.DEFAULT_PERIOD_SECONDS
                    .toString(),
            )
            assertThat(state.totpData.totpUiModel.length).isEqualTo(
                OtpParseResult.OtpQr.TotpQr.DEFAULT_DIGITS
                    .toString(),
            )
        }

    @Test
    fun `view should show correct ui for create password`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                onBlocking { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = ContentType.V5Default,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                onBlocking { getSecretEntropy(any()) }.thenReturn(0.0)
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.PASSWORD,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }

            advanceUntilIdle()

            val state = viewModel.viewState.value
            assertThat(state.shouldShowScreenProgress).isFalse()
            assertThat(state.name).isEqualTo("")
            assertThat(state.leadingContentType).isEqualTo(LeadingContentType.PASSWORD)
            assertThat(state.isPrimaryButtonVisible).isTrue()
            assertThat(state.passwordData.mainUri).isEqualTo("")
            assertThat(state.passwordData.username).isEqualTo("")
            assertThat(state.passwordData.password).isEqualTo("")
            assertThat(state.passwordData.passwordStrength).isEqualTo(PasswordStrength.Empty)
            assertThat(state.passwordData.passwordEntropyBits).isEqualTo(0.0)
        }

    @Test
    fun `view should show correct ui for create standalone note`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                onBlocking { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = ContentType.V5Note,
                    ),
                )
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.STANDALONE_NOTE,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }

            advanceUntilIdle()

            val state = viewModel.viewState.value
            assertThat(state.shouldShowScreenProgress).isFalse()
            assertThat(state.leadingContentType).isEqualTo(LeadingContentType.STANDALONE_NOTE)
            assertThat(state.isPrimaryButtonVisible).isTrue()
            assertThat(state.noteData.note).isEqualTo("")
        }

    @Test
    fun `view should show correct initial mode in state`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                onBlocking { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = ContentType.V5Default,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                onBlocking { getSecretEntropy(any()) }.thenReturn(0.0)
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.PASSWORD,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }

            advanceUntilIdle()

            val state = viewModel.viewState.value
            assertIs<ResourceFormMode.Create>(state.mode)
        }

    @Test
    fun `initialization failure should emit toast and navigate back`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                onBlocking { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.NotPossibleNotCreateResource,
                )
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.PASSWORD,
                    parentFolderId = null,
                )

            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }

            viewModel.sideEffect.test {
                advanceUntilIdle()
                val toast = awaitItem()
                assertIs<ShowToast>(toast)
                assertThat(toast.type).isEqualTo(ToastMessage.CREATE_INITIALIZATION_ERROR)
                assertIs<NavigateBack>(awaitItem())
            }
        }

    @Test
    fun `advanced settings should show additional password sections`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                onBlocking { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = ContentType.V5Default,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                onBlocking { getSecretEntropy(any()) }.thenReturn(0.0)
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.PASSWORD,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }

            advanceUntilIdle()
            viewModel.onIntent(ExpandAdvancedSettings)
            advanceUntilIdle()

            val state = viewModel.viewState.value
            assertThat(state.supportedAdditionalSecrets).containsExactly(NOTE, TOTP)
            assertThat(state.supportedMetadata).containsExactly(DESCRIPTION, ADDITIONAL_URIS, APPEARANCE)
            assertThat(state.areAdvancedSettingsVisible).isFalse()
        }

    @Test
    fun `advanced settings should show additional totp sections`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                onBlocking { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = ContentType.V5TotpStandalone,
                    ),
                )
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.TOTP,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }

            advanceUntilIdle()
            viewModel.onIntent(ExpandAdvancedSettings)
            advanceUntilIdle()

            val state = viewModel.viewState.value
            assertThat(state.supportedAdditionalSecrets).containsExactly(PASSWORD, NOTE)
            assertThat(state.supportedMetadata).containsExactly(DESCRIPTION, ADDITIONAL_URIS, APPEARANCE)
            assertThat(state.areAdvancedSettingsVisible).isFalse()
        }

    @Test
    fun `advanced settings expanded flag should be set after expand`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                onBlocking { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = ContentType.V5Default,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                onBlocking { getSecretEntropy(any()) }.thenReturn(0.0)
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.PASSWORD,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }

            advanceUntilIdle()

            assertThat(viewModel.viewState.value.areAdvancedSettingsExpanded).isFalse()

            viewModel.onIntent(ExpandAdvancedSettings)
            advanceUntilIdle()

            assertThat(viewModel.viewState.value.areAdvancedSettingsExpanded).isTrue()
        }

    @Test
    fun `password change should trigger entropy recalculation`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                onBlocking { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = ContentType.V5Default,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                onBlocking { getSecretEntropy(any()) }.thenReturn(0.0)
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.PASSWORD,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }

            advanceUntilIdle()

            mockEntropyCalculator.stub {
                onBlocking { getSecretEntropy("t") }.thenReturn(5.0)
                onBlocking { getSecretEntropy("te") }.thenReturn(10.0)
                onBlocking { getSecretEntropy("tes") }.thenReturn(15.0)
                onBlocking { getSecretEntropy("test") }.thenReturn(20.0)
            }

            viewModel.onIntent(PasswordTextChanged("t"))
            advanceUntilIdle()
            viewModel.onIntent(PasswordTextChanged("te"))
            advanceUntilIdle()
            viewModel.onIntent(PasswordTextChanged("tes"))
            advanceUntilIdle()
            viewModel.onIntent(PasswordTextChanged("test"))
            advanceUntilIdle()

            val state = viewModel.viewState.value
            assertThat(state.passwordData.password).isEqualTo("test")
            assertThat(state.passwordData.passwordEntropyBits).isEqualTo(20.0)
        }

    @Test
    fun `password change should update password strength`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                onBlocking { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = ContentType.V5Default,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                onBlocking { getSecretEntropy(any()) }.thenReturn(0.0)
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.PASSWORD,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }

            advanceUntilIdle()

            mockEntropyCalculator.stub {
                onBlocking { getSecretEntropy("strongpassword") }.thenReturn(130.0)
            }

            viewModel.onIntent(PasswordTextChanged("strongpassword"))
            advanceUntilIdle()

            val state = viewModel.viewState.value
            assertThat(state.passwordData.passwordStrength).isEqualTo(PasswordStrength.VeryStrong)
        }

    @Test
    fun `password main uri change should update state`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                onBlocking { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = ContentType.V5Default,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                onBlocking { getSecretEntropy(any()) }.thenReturn(0.0)
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.PASSWORD,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }

            advanceUntilIdle()
            viewModel.onIntent(PasswordMainUriTextChanged("https://example.com"))
            advanceUntilIdle()

            val state = viewModel.viewState.value
            assertThat(state.passwordData.mainUri).isEqualTo("https://example.com")
        }

    @Test
    fun `password username change should update state`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                onBlocking { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = ContentType.V5Default,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                onBlocking { getSecretEntropy(any()) }.thenReturn(0.0)
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.PASSWORD,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }

            advanceUntilIdle()
            viewModel.onIntent(PasswordUsernameTextChanged("user@example.com"))
            advanceUntilIdle()

            val state = viewModel.viewState.value
            assertThat(state.passwordData.username).isEqualTo("user@example.com")
        }

    @Test
    fun `generate password should update state with generated password on success`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                onBlocking { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = ContentType.V5Default,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                onBlocking { getSecretEntropy(any()) }.thenReturn(0.0)
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.PASSWORD,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }

            advanceUntilIdle()

            val generatedCodepoints = "GeneratedPass1!".map { Codepoint(it.code) }
            mockGetPasswordPoliciesUseCase.stub {
                onBlocking { execute(any()) }.thenReturn(MOCK_PASSWORD_POLICIES)
            }
            mockSecretGenerator.stub {
                onBlocking { generatePassword(any()) }.thenReturn(
                    SecretGenerator.SecretGenerationResult.Success(generatedCodepoints, 100.0),
                )
            }

            viewModel.onIntent(GeneratePassword)
            advanceUntilIdle()

            val state = viewModel.viewState.value
            assertThat(state.passwordData.password).isEqualTo("GeneratedPass1!")
            assertThat(state.passwordData.passwordEntropyBits).isEqualTo(100.0)
            assertThat(state.passwordData.passwordStrength).isEqualTo(PasswordStrength.Fair)
        }

    @Test
    fun `generate password should show toast on low entropy failure`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                onBlocking { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = ContentType.V5Default,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                onBlocking { getSecretEntropy(any()) }.thenReturn(0.0)
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.PASSWORD,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }

            advanceUntilIdle()

            mockGetPasswordPoliciesUseCase.stub {
                onBlocking { execute(any()) }.thenReturn(MOCK_PASSWORD_POLICIES)
            }
            mockSecretGenerator.stub {
                onBlocking { generatePassword(any()) }.thenReturn(
                    SecretGenerator.SecretGenerationResult.FailedToGenerateLowEntropy(80),
                )
            }

            viewModel.sideEffect.test {
                viewModel.onIntent(GeneratePassword)
                advanceUntilIdle()
                val sideEffect = awaitItem()
                assertIs<ShowToast>(sideEffect)
                assertThat(sideEffect.type).isEqualTo(ToastMessage.UNABLE_TO_GENERATE_PASSWORD)
            }
        }

    @Test
    fun `name text change should update state`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                onBlocking { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = ContentType.V5Default,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                onBlocking { getSecretEntropy(any()) }.thenReturn(0.0)
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.PASSWORD,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }

            advanceUntilIdle()
            viewModel.onIntent(NameTextChanged("My Resource"))
            advanceUntilIdle()

            val state = viewModel.viewState.value
            assertThat(state.name).isEqualTo("My Resource")
        }

    @Test
    fun `totp secret change should update state`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                onBlocking { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = ContentType.V5TotpStandalone,
                    ),
                )
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.TOTP,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }

            advanceUntilIdle()
            viewModel.onIntent(TotpSecretChanged("JBSWY3DPEHPK3PXP"))
            advanceUntilIdle()

            val state = viewModel.viewState.value
            assertThat(state.totpData.totpSecret).isEqualTo("JBSWY3DPEHPK3PXP")
            assertThat(state.totpData.totpSecretError).isNull()
        }

    @Test
    fun `totp secret change should clear previous error`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                onBlocking { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = ContentType.V5TotpStandalone,
                    ),
                )
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.TOTP,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }

            advanceUntilIdle()

            viewModel.onIntent(ResourceFormIntent.CreateResource)
            advanceUntilIdle()
            assertIs<TotpSecretValidationError.MustNotBeEmpty>(viewModel.viewState.value.totpData.totpSecretError)

            viewModel.onIntent(TotpSecretChanged("AAAAAAAA"))
            advanceUntilIdle()

            assertThat(viewModel.viewState.value.totpData.totpSecretError).isNull()
        }

    @Test
    fun `totp url change should update state`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                onBlocking { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = ContentType.V5TotpStandalone,
                    ),
                )
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.TOTP,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }

            advanceUntilIdle()
            viewModel.onIntent(TotpUrlChanged("https://totp-issuer.com"))
            advanceUntilIdle()

            val state = viewModel.viewState.value
            assertThat(state.totpData.totpIssuer).isEqualTo("https://totp-issuer.com")
        }

    @Test
    fun `note change should update state`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                onBlocking { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = ContentType.V5Note,
                    ),
                )
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.STANDALONE_NOTE,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }

            advanceUntilIdle()
            viewModel.onIntent(NoteChanged("My secret note"))
            advanceUntilIdle()

            val state = viewModel.viewState.value
            assertThat(state.noteData.note).isEqualTo("My secret note")
            assertThat(state.noteData.noteError).isNull()
        }

    @Test
    fun `note change should clear previous error`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                onBlocking { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = ContentType.V5Note,
                    ),
                )
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.STANDALONE_NOTE,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }

            advanceUntilIdle()

            val tooLongNote = "a".repeat(50_001)
            viewModel.onIntent(NoteChanged(tooLongNote))
            advanceUntilIdle()
            viewModel.onIntent(ResourceFormIntent.CreateResource)
            advanceUntilIdle()
            assertIs<NoteValidationError.MaxLengthExceeded>(viewModel.viewState.value.noteData.noteError)

            viewModel.onIntent(NoteChanged("short note"))
            advanceUntilIdle()

            assertThat(viewModel.viewState.value.noteData.noteError).isNull()
        }

    @Test
    fun `create resource with empty totp secret should show must not be empty error`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                onBlocking { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = ContentType.V5TotpStandalone,
                    ),
                )
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.TOTP,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }

            advanceUntilIdle()

            viewModel.onIntent(ResourceFormIntent.CreateResource)
            advanceUntilIdle()

            val state = viewModel.viewState.value
            assertIs<TotpSecretValidationError.MustNotBeEmpty>(state.totpData.totpSecretError)
        }

    @Test
    fun `create resource with non base32 totp secret should show must be base32 error`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                onBlocking { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = ContentType.V5TotpStandalone,
                    ),
                )
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.TOTP,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }

            advanceUntilIdle()

            viewModel.onIntent(TotpSecretChanged("invalid!@#\$%"))
            advanceUntilIdle()
            viewModel.onIntent(ResourceFormIntent.CreateResource)
            advanceUntilIdle()

            val state = viewModel.viewState.value
            assertIs<TotpSecretValidationError.MustBeBase32>(state.totpData.totpSecretError)
        }

    @Test
    fun `create resource with note exceeding max length should show error`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                onBlocking { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = ContentType.V5Note,
                    ),
                )
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.STANDALONE_NOTE,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }

            advanceUntilIdle()

            val tooLongNote = "a".repeat(50_001)
            viewModel.onIntent(NoteChanged(tooLongNote))
            advanceUntilIdle()
            viewModel.onIntent(ResourceFormIntent.CreateResource)
            advanceUntilIdle()

            val state = viewModel.viewState.value
            assertIs<NoteValidationError.MaxLengthExceeded>(state.noteData.noteError)
        }

    @Test
    fun `go back should emit navigate back side effect`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                onBlocking { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = ContentType.V5Default,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                onBlocking { getSecretEntropy(any()) }.thenReturn(0.0)
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.PASSWORD,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }

            advanceUntilIdle()

            viewModel.sideEffect.test {
                viewModel.onIntent(GoBack)
                advanceUntilIdle()
                assertIs<NavigateBack>(awaitItem())
            }
        }

    @Test
    fun `scan totp should emit navigate to scan otp side effect`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                onBlocking { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = ContentType.V5TotpStandalone,
                    ),
                )
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.TOTP,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }

            advanceUntilIdle()

            viewModel.sideEffect.test {
                viewModel.onIntent(ScanTotp)
                advanceUntilIdle()
                assertIs<NavigateToScanOtp>(awaitItem())
            }
        }

    @Test
    fun `go to additional note should emit navigate to note side effect`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                onBlocking { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = ContentType.V5Default,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                onBlocking { getSecretEntropy(any()) }.thenReturn(0.0)
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.PASSWORD,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }

            advanceUntilIdle()

            viewModel.sideEffect.test {
                viewModel.onIntent(GoToAdditionalNote)
                advanceUntilIdle()
                val sideEffect = awaitItem()
                assertIs<NavigateToNote>(sideEffect)
                assertIs<ResourceFormMode.Create>(sideEffect.mode)
            }
        }

    @Test
    fun `go to additional password should emit navigate to password side effect`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                onBlocking { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = ContentType.V5TotpStandalone,
                    ),
                )
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.TOTP,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }

            advanceUntilIdle()

            viewModel.sideEffect.test {
                viewModel.onIntent(GoToAdditionalPassword)
                advanceUntilIdle()
                val sideEffect = awaitItem()
                assertIs<NavigateToPassword>(sideEffect)
                assertIs<ResourceFormMode.Create>(sideEffect.mode)
            }
        }

    @Test
    fun `go to additional totp should emit navigate to totp side effect`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                onBlocking { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = ContentType.V5Default,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                onBlocking { getSecretEntropy(any()) }.thenReturn(0.0)
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.PASSWORD,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }

            advanceUntilIdle()

            viewModel.sideEffect.test {
                viewModel.onIntent(GoToAdditionalTotp)
                advanceUntilIdle()
                val sideEffect = awaitItem()
                assertIs<NavigateToTotp>(sideEffect)
                assertIs<ResourceFormMode.Create>(sideEffect.mode)
            }
        }

    @Test
    fun `go to totp more settings should emit navigate to totp advanced settings`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                onBlocking { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = ContentType.V5TotpStandalone,
                    ),
                )
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.TOTP,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }

            advanceUntilIdle()

            viewModel.sideEffect.test {
                viewModel.onIntent(GoToTotpMoreSettings)
                advanceUntilIdle()
                val sideEffect = awaitItem()
                assertIs<NavigateToTotpAdvancedSettings>(sideEffect)
                assertIs<ResourceFormMode.Create>(sideEffect.mode)
            }
        }

    @Test
    fun `go to metadata description should emit navigate to description side effect`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                onBlocking { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = ContentType.V5Default,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                onBlocking { getSecretEntropy(any()) }.thenReturn(0.0)
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.PASSWORD,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }

            advanceUntilIdle()

            viewModel.sideEffect.test {
                viewModel.onIntent(GoToMetadataDescription)
                advanceUntilIdle()
                val sideEffect = awaitItem()
                assertIs<NavigateToDescription>(sideEffect)
                assertIs<ResourceFormMode.Create>(sideEffect.mode)
            }
        }

    @Test
    fun `go to appearance should emit navigate to appearance side effect`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                onBlocking { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = ContentType.V5Default,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                onBlocking { getSecretEntropy(any()) }.thenReturn(0.0)
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.PASSWORD,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }

            advanceUntilIdle()

            viewModel.sideEffect.test {
                viewModel.onIntent(GoToAppearance)
                advanceUntilIdle()
                val sideEffect = awaitItem()
                assertIs<NavigateToAppearance>(sideEffect)
                assertIs<ResourceFormMode.Create>(sideEffect.mode)
            }
        }

    @Test
    fun `go to additional uris should emit navigate to additional uris side effect`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                onBlocking { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = ContentType.V5Default,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                onBlocking { getSecretEntropy(any()) }.thenReturn(0.0)
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.PASSWORD,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }

            advanceUntilIdle()

            viewModel.sideEffect.test {
                viewModel.onIntent(GoToAdditionalUris)
                advanceUntilIdle()
                val sideEffect = awaitItem()
                assertIs<NavigateToAdditionalUris>(sideEffect)
                assertIs<ResourceFormMode.Create>(sideEffect.mode)
            }
        }

    @Test
    fun `go to custom fields should emit navigate to custom fields side effect`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                onBlocking { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = ContentType.V5Default,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                onBlocking { getSecretEntropy(any()) }.thenReturn(0.0)
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.PASSWORD,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }

            advanceUntilIdle()

            viewModel.sideEffect.test {
                viewModel.onIntent(GoToCustomFields)
                advanceUntilIdle()
                val sideEffect = awaitItem()
                assertIs<ResourceFormSideEffect.NavigateToCustomFields>(sideEffect)
                assertIs<ResourceFormMode.Create>(sideEffect.mode)
            }
        }

    @Test
    fun `scan otp result should update state with scanned totp data`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                onBlocking { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = ContentType.V5TotpStandalone,
                    ),
                )
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.TOTP,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }

            advanceUntilIdle()

            val scannedTotp =
                OtpParseResult.OtpQr.TotpQr(
                    label = "TestLabel",
                    secret = "JBSWY3DPEHPK3PXP",
                    issuer = "TestIssuer",
                    algorithm = OtpParseResult.OtpQr.Algorithm.SHA1,
                    digits = 6,
                    period = 30,
                )

            viewModel.onIntent(ScanOtpResult(isManualCreationChosen = false, scannedTotp))
            advanceUntilIdle()

            val state = viewModel.viewState.value
            assertThat(state.name).isEqualTo("TestLabel")
            assertThat(state.totpData.totpSecret).isEqualTo("JBSWY3DPEHPK3PXP")
            assertThat(state.totpData.totpIssuer).isEqualTo("TestIssuer")
        }

    @Test
    fun `scan otp result with manual creation chosen should not update state`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                onBlocking { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = ContentType.V5TotpStandalone,
                    ),
                )
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.TOTP,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }

            advanceUntilIdle()

            val scannedTotp =
                OtpParseResult.OtpQr.TotpQr(
                    label = "TestLabel",
                    secret = "JBSWY3DPEHPK3PXP",
                    issuer = "TestIssuer",
                    algorithm = OtpParseResult.OtpQr.Algorithm.SHA1,
                    digits = 6,
                    period = 30,
                )

            viewModel.onIntent(ScanOtpResult(isManualCreationChosen = true, scannedTotp))
            advanceUntilIdle()

            val state = viewModel.viewState.value
            assertThat(state.name).isEqualTo("")
            assertThat(state.totpData.totpSecret).isEqualTo("")
        }

    @Test
    fun `dismiss metadata key dialog should clear both dialog states`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                onBlocking { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = ContentType.V5Default,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                onBlocking { getSecretEntropy(any()) }.thenReturn(0.0)
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.PASSWORD,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }

            advanceUntilIdle()

            viewModel.onIntent(DismissMetadataKeyDialog)
            advanceUntilIdle()

            val state = viewModel.viewState.value
            assertThat(state.metadataKeyModifiedDialog).isNull()
            assertThat(state.metadataKeyDeletedDialog).isNull()
        }

    private companion object {
        val MOCK_PASSWORD_POLICIES =
            com.passbolt.mobile.android.ui.PasswordPolicies(
                defaultGenerator = PasswordGeneratorTypeModel.PASSWORD,
                passwordGeneratorSettings =
                    com.passbolt.mobile.android.ui.PasswordGeneratorSettingsModel(
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
                    com.passbolt.mobile.android.ui.PassphraseGeneratorSettingsModel(
                        words = 9,
                        wordSeparator = " ",
                        wordCase = com.passbolt.mobile.android.ui.CaseTypeModel.LOWERCASE,
                    ),
                isExternalDictionaryCheckEnabled = true,
            )
    }
}
