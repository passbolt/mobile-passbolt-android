package com.passbolt.mobile.android.feature.resourceform.main

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.architecture.result.DomainResult
import com.passbolt.mobile.android.core.architecture.result.DomainResult.Incomplete.Error.Reason.UNKNOWN
import com.passbolt.mobile.android.core.passphrasememorycache.PassphraseMemoryCache
import com.passbolt.mobile.android.core.passwordgenerator.SecretGenerator
import com.passbolt.mobile.android.core.passwordgenerator.codepoints.Codepoint
import com.passbolt.mobile.android.core.passwordgenerator.usecase.CheckPasswordPropertiesUseCase
import com.passbolt.mobile.android.domain.metadata.interactor.MetadataPrivateKeysHelperInteractor
import com.passbolt.mobile.android.domain.metadata.usecase.GetMetadataTypesSettingsUseCase
import com.passbolt.mobile.android.domain.passwordexpiry.model.PasswordExpirySettings
import com.passbolt.mobile.android.domain.passwordexpiry.usecase.PasswordExpiryPoliciesInteractor
import com.passbolt.mobile.android.domain.passwordpolicies.usecase.PasswordPoliciesInteractor
import com.passbolt.mobile.android.domain.resources.actions.ResourceCreateActionResult
import com.passbolt.mobile.android.domain.resources.actions.ResourceUpdateActionResult
import com.passbolt.mobile.android.domain.resources.actions.ResourceUpdateActionResult.CannotUpdateWithCurrentConfig
import com.passbolt.mobile.android.domain.resources.actions.ResourceUpdateActionResult.Failure
import com.passbolt.mobile.android.domain.resources.actions.ResourceUpdateActionsInteractor
import com.passbolt.mobile.android.domain.resources.actions.SecretPropertiesActionsInteractor
import com.passbolt.mobile.android.domain.resources.actions.SecretPropertyActionResult
import com.passbolt.mobile.android.domain.resources.usecase.GetDefaultCreateContentTypeUseCase
import com.passbolt.mobile.android.domain.resources.usecase.GetEditContentTypeUseCase
import com.passbolt.mobile.android.domain.resources.usecase.db.GetLocalResourceUseCase
import com.passbolt.mobile.android.domain.secrets.model.SecretJsonModel
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetSessionExpiryUseCase
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.note.NoteValidationError
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpSecretValidationError
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.CreateResource
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.DismissMetadataKeyDialog
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.DismissPasswordWarning
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.ExpandAdvancedSettings
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.GeneratePassword
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.GeneratePinCode
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.GoBack
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.GoToAdditionalNote
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.GoToAdditionalPassword
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.GoToAdditionalPinCode
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.GoToAdditionalTotp
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.GoToAdditionalUris
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.GoToAppearance
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.GoToCustomFields
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.GoToMetadataDescription
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.GoToPinCodeAdvancedGeneration
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.GoToTotpMoreSettings
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.LearnMoreAboutUpgrade
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.NameTextChanged
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.NoteChanged
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.PasswordMainUriTextChanged
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.PasswordTextChanged
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.PasswordUsernameTextChanged
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.PinCodeAdvancedGenerationResult
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.ProceedWithPasswordWarning
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.ScanOtpResult
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.ScanTotp
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.TotpSecretChanged
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.TotpUrlChanged
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.TrustNewMetadataKey
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.TrustedMetadataKeyDeleted
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.UpgradeResource
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateBack
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateToAdditionalUris
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateToAppearance
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateToDescription
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateToNote
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateToPassword
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateToPinCode
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateToPinCodeAdvancedGeneration
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateToScanOtp
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateToTotp
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateToTotpAdvancedSettings
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.OpenWebsite
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.ShowSnackbar
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.ShowToast
import com.passbolt.mobile.android.feature.resourceform.main.SnackbarMessage.CANNOT_CREATE_RESOURCE_WITH_CURRENT_CONFIG
import com.passbolt.mobile.android.feature.resourceform.main.SnackbarMessage.COMMON_FAILURE
import com.passbolt.mobile.android.feature.resourceform.navigation.AdvancedSecretGenerationFormResult
import com.passbolt.mobile.android.featureflags.usecase.GetFeatureFlagsUseCase
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.PasswordAndDescription
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.V5Default
import com.passbolt.mobile.android.ui.CaseTypeUiModel
import com.passbolt.mobile.android.ui.CaseTypeUiModel.LOWERCASE
import com.passbolt.mobile.android.ui.LeadingContentType
import com.passbolt.mobile.android.ui.MetadataJsonModel
import com.passbolt.mobile.android.ui.MetadataKeyModification
import com.passbolt.mobile.android.ui.MetadataKeyTypeModel.PERSONAL
import com.passbolt.mobile.android.ui.MetadataTypeModel
import com.passbolt.mobile.android.ui.MetadataTypeModel.V4
import com.passbolt.mobile.android.ui.NewMetadataKeyToTrustModel
import com.passbolt.mobile.android.ui.OtpParseResult
import com.passbolt.mobile.android.ui.ParsedMetadataPrivateKeyModel
import com.passbolt.mobile.android.ui.PassphraseGeneratorSettingsUiModel
import com.passbolt.mobile.android.ui.PasswordGeneratorSettingsUiModel
import com.passbolt.mobile.android.ui.PasswordGeneratorTypeUiModel
import com.passbolt.mobile.android.ui.PasswordPoliciesUiModel
import com.passbolt.mobile.android.ui.PasswordStrength
import com.passbolt.mobile.android.ui.PinCodeUiModel
import com.passbolt.mobile.android.ui.ResourceFormMode
import com.passbolt.mobile.android.ui.ResourceFormUiModel.Metadata.ADDITIONAL_URIS
import com.passbolt.mobile.android.ui.ResourceFormUiModel.Metadata.APPEARANCE
import com.passbolt.mobile.android.ui.ResourceFormUiModel.Metadata.DESCRIPTION
import com.passbolt.mobile.android.ui.ResourceFormUiModel.Secret.NOTE
import com.passbolt.mobile.android.ui.ResourceFormUiModel.Secret.PASSWORD
import com.passbolt.mobile.android.ui.ResourceFormUiModel.Secret.TOTP
import com.passbolt.mobile.android.ui.ResourcePermission.OWNER
import com.passbolt.mobile.android.ui.ResourceUiModel
import com.passbolt.mobile.android.ui.TrustedKeyDeletedModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOf
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
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.time.ZonedDateTime
import java.util.UUID
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

        reset(mockGetFeatureFlagsUseCase, mockPasswordPoliciesInteractor, mockPasswordExpiryPoliciesInteractor)
        mockGetFeatureFlagsUseCase.stub {
            on { execute(Unit) }.thenReturn(GetFeatureFlagsUseCase.Output(DEFAULT_TEST_FEATURE_FLAGS))
        }

        val passphraseMemoryCache: PassphraseMemoryCache = get()
        whenever(passphraseMemoryCache.getSessionDurationSeconds()) doReturn 5 * 60

        val getSessionExpiryUseCase: GetSessionExpiryUseCase = get()
        whenever(getSessionExpiryUseCase.execute(Unit)) doReturn
            GetSessionExpiryUseCase.Output.JwtWillExpire(ZonedDateTime.now().plusMinutes(5))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        reset(mockGetFeatureFlagsUseCase, mockPasswordPoliciesInteractor, mockPasswordExpiryPoliciesInteractor)
        mockGetFeatureFlagsUseCase.stub {
            on { execute(Unit) }.thenReturn(GetFeatureFlagsUseCase.Output(DEFAULT_TEST_FEATURE_FLAGS))
        }
    }

    @Test
    fun `view should show correct ui for create totp`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                on { execute(any()) }.thenReturn(
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
                on { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = V5Default,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                on { getSecretEntropy(any()) }.thenReturn(0.0)
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
                on { execute(any()) }.thenReturn(
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
    fun `view should show correct ui for create pin code`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                on { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = ContentType.V5PinCodeStandalone,
                    ),
                )
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.PIN_CODE,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }

            advanceUntilIdle()

            val state = viewModel.viewState.value
            assertThat(state.shouldShowScreenProgress).isFalse()
            assertThat(state.leadingContentType).isEqualTo(LeadingContentType.PIN_CODE)
            assertThat(state.isPrimaryButtonVisible).isTrue()
            assertThat(state.pinCodeData.pinCode).isEqualTo("")
            assertThat(state.pinCodeData.length).isEqualTo(PinCodeUiModel.DEFAULT_LENGTH)
        }

    @Test
    fun `view should show correct initial mode in state`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                on { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = V5Default,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                on { getSecretEntropy(any()) }.thenReturn(0.0)
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
                on { execute(any()) }.thenReturn(
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
                on { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = V5Default,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                on { getSecretEntropy(any()) }.thenReturn(0.0)
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
                on { execute(any()) }.thenReturn(
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
                on { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = V5Default,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                on { getSecretEntropy(any()) }.thenReturn(0.0)
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
                on { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = V5Default,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                on { getSecretEntropy(any()) }.thenReturn(0.0)
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.PASSWORD,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }

            advanceUntilIdle()

            mockEntropyCalculator.stub {
                on { getSecretEntropy("t") }.thenReturn(5.0)
                on { getSecretEntropy("te") }.thenReturn(10.0)
                on { getSecretEntropy("tes") }.thenReturn(15.0)
                on { getSecretEntropy("test") }.thenReturn(20.0)
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
                on { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = V5Default,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                on { getSecretEntropy(any()) }.thenReturn(0.0)
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.PASSWORD,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }

            advanceUntilIdle()

            mockEntropyCalculator.stub {
                on { getSecretEntropy("strongpassword") }.thenReturn(130.0)
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
                on { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = V5Default,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                on { getSecretEntropy(any()) }.thenReturn(0.0)
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
                on { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = V5Default,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                on { getSecretEntropy(any()) }.thenReturn(0.0)
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
                on { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = V5Default,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                on { getSecretEntropy(any()) }.thenReturn(0.0)
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
                on { execute(any()) }.thenReturn(MOCK_PASSWORD_POLICIES)
            }
            mockSecretGenerator.stub {
                on { generatePassword(any()) }.thenReturn(
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
    fun `generate password should show unable to generate dialog on low entropy failure`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                on { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = V5Default,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                on { getSecretEntropy(any()) }.thenReturn(0.0)
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.PASSWORD,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }

            advanceUntilIdle()

            mockGetPasswordPoliciesUseCase.stub {
                on { execute(any()) }.thenReturn(MOCK_PASSWORD_POLICIES)
            }
            mockSecretGenerator.stub {
                on { generatePassword(any()) }.thenReturn(
                    SecretGenerator.SecretGenerationResult.FailedToGenerateLowEntropy(80),
                )
            }

            viewModel.onIntent(GeneratePassword)
            advanceUntilIdle()

            val state = viewModel.viewState.value
            assertThat(state.isUnableToGeneratePasswordDialogVisible).isTrue()
            assertThat(state.minimumEntropyBits).isEqualTo(80)
        }

    @Test
    fun `name text change should update state`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                on { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = V5Default,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                on { getSecretEntropy(any()) }.thenReturn(0.0)
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
                on { execute(any()) }.thenReturn(
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
                on { execute(any()) }.thenReturn(
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
                on { execute(any()) }.thenReturn(
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
                on { execute(any()) }.thenReturn(
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
                on { execute(any()) }.thenReturn(
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
                on { execute(any()) }.thenReturn(
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
                on { execute(any()) }.thenReturn(
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
                on { execute(any()) }.thenReturn(
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
                on { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = V5Default,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                on { getSecretEntropy(any()) }.thenReturn(0.0)
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
                on { execute(any()) }.thenReturn(
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
                on { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = V5Default,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                on { getSecretEntropy(any()) }.thenReturn(0.0)
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
                on { execute(any()) }.thenReturn(
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
                on { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = V5Default,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                on { getSecretEntropy(any()) }.thenReturn(0.0)
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
    fun `go to additional pin code should emit navigate to pin code side effect`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                on { execute(any()) }.thenReturn(
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

            viewModel.sideEffect.test {
                viewModel.onIntent(GoToAdditionalPinCode)
                advanceUntilIdle()
                val sideEffect = awaitItem()
                assertIs<NavigateToPinCode>(sideEffect)
                assertIs<ResourceFormMode.Create>(sideEffect.mode)
            }
        }

    @Test
    fun `go to pin code advanced generation should emit navigate side effect`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                on { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = ContentType.V5PinCodeStandalone,
                    ),
                )
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.PIN_CODE,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }

            advanceUntilIdle()

            viewModel.sideEffect.test {
                viewModel.onIntent(GoToPinCodeAdvancedGeneration)
                advanceUntilIdle()
                val sideEffect = awaitItem()
                assertIs<NavigateToPinCodeAdvancedGeneration>(sideEffect)
            }
        }

    @Test
    fun `generate pin code should update state with generated pin`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                on { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = ContentType.V5PinCodeStandalone,
                    ),
                )
            }
            whenever(mockPinCodeGenerator.generate(PinCodeUiModel.DEFAULT_LENGTH)).thenReturn("4242")

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.PIN_CODE,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }

            advanceUntilIdle()
            viewModel.onIntent(GeneratePinCode)
            advanceUntilIdle()

            val state = viewModel.viewState.value
            assertThat(state.pinCodeData.pinCode).isEqualTo("4242")
            assertThat(state.pinCodeData.length).isEqualTo(PinCodeUiModel.DEFAULT_LENGTH)
        }

    @Test
    fun `pin code advanced generation result should regenerate with new length`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                on { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = ContentType.V5PinCodeStandalone,
                    ),
                )
            }
            whenever(mockPinCodeGenerator.generate(8)).thenReturn("12345678")

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.PIN_CODE,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }

            advanceUntilIdle()
            viewModel.onIntent(PinCodeAdvancedGenerationResult(PinCodeUiModel(pinCode = "0000", length = 8)))
            advanceUntilIdle()

            val state = viewModel.viewState.value
            assertThat(state.pinCodeData.pinCode).isEqualTo("12345678")
            assertThat(state.pinCodeData.length).isEqualTo(8)
        }

    @Test
    fun `go to totp more settings should emit navigate to totp advanced settings`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                on { execute(any()) }.thenReturn(
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
                on { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = V5Default,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                on { getSecretEntropy(any()) }.thenReturn(0.0)
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
                on { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = V5Default,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                on { getSecretEntropy(any()) }.thenReturn(0.0)
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
                on { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = V5Default,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                on { getSecretEntropy(any()) }.thenReturn(0.0)
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
                on { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = V5Default,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                on { getSecretEntropy(any()) }.thenReturn(0.0)
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
    fun `open advanced secret generation loads policies and emits navigate side effect`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                on { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = V5Default,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                on { getSecretEntropy(any()) }.thenReturn(0.0)
            }
            mockGetPasswordPoliciesUseCase.stub {
                on { execute(any()) }.thenReturn(MOCK_PASSWORD_POLICIES)
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.PASSWORD,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }
            advanceUntilIdle()

            viewModel.sideEffect.test {
                viewModel.onIntent(ResourceFormIntent.OpenAdvancedSecretGeneration)
                advanceUntilIdle()

                val sideEffect = awaitItem()
                assertIs<ResourceFormSideEffect.NavigateToAdvancedSecretGeneration>(sideEffect)
                assertThat(sideEffect.selectedTab).isEqualTo(MOCK_PASSWORD_POLICIES.defaultGenerator)
                assertThat(sideEffect.passwordSettings).isEqualTo(MOCK_PASSWORD_POLICIES.passwordGeneratorSettings)
                assertThat(sideEffect.passphraseSettings).isEqualTo(MOCK_PASSWORD_POLICIES.passphraseGeneratorSettings)
            }

            val state = viewModel.viewState.value
            assertThat(state.generatorType).isEqualTo(MOCK_PASSWORD_POLICIES.defaultGenerator)
            assertThat(state.passwordGeneratorSettings).isEqualTo(MOCK_PASSWORD_POLICIES.passwordGeneratorSettings)
            assertThat(state.passphraseGeneratorSettings).isEqualTo(MOCK_PASSWORD_POLICIES.passphraseGeneratorSettings)
        }

    @Test
    fun `open advanced secret generation reuses cached settings when present`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                on { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = V5Default,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                on { getSecretEntropy(any()) }.thenReturn(75.0)
            }
            mockGetPasswordPoliciesUseCase.stub {
                on { execute(any()) }.thenReturn(MOCK_PASSWORD_POLICIES)
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.PASSWORD,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }
            advanceUntilIdle()

            val result =
                AdvancedSecretGenerationFormResult(
                    passwordSettings = CUSTOM_PASSWORD_SETTINGS,
                    passphraseSettings = CUSTOM_PASSPHRASE_SETTINGS,
                    selectedTab = PasswordGeneratorTypeUiModel.PASSPHRASE,
                    generatedSecret = "cached secret",
                )
            viewModel.onIntent(ResourceFormIntent.AdvancedSecretGenerationResult(result))
            advanceUntilIdle()
            reset(mockGetPasswordPoliciesUseCase)

            viewModel.sideEffect.test {
                viewModel.onIntent(ResourceFormIntent.OpenAdvancedSecretGeneration)
                advanceUntilIdle()

                val sideEffect = awaitItem()
                assertIs<ResourceFormSideEffect.NavigateToAdvancedSecretGeneration>(sideEffect)
                assertThat(sideEffect.selectedTab).isEqualTo(PasswordGeneratorTypeUiModel.PASSPHRASE)
                assertThat(sideEffect.passwordSettings).isEqualTo(CUSTOM_PASSWORD_SETTINGS)
                assertThat(sideEffect.passphraseSettings).isEqualTo(CUSTOM_PASSPHRASE_SETTINGS)
            }

            verifyNoInteractions(mockGetPasswordPoliciesUseCase)
        }

    @Test
    fun `advanced secret generation result stores settings and applies generated password`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                on { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = V5Default,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                on { getSecretEntropy(any()) }.thenReturn(0.0)
                on { getSecretEntropy("generated secret") }.thenReturn(150.0)
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.PASSWORD,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }
            advanceUntilIdle()

            val result =
                AdvancedSecretGenerationFormResult(
                    passwordSettings = CUSTOM_PASSWORD_SETTINGS,
                    passphraseSettings = CUSTOM_PASSPHRASE_SETTINGS,
                    selectedTab = PasswordGeneratorTypeUiModel.PASSWORD,
                    generatedSecret = "generated secret",
                )
            viewModel.onIntent(ResourceFormIntent.AdvancedSecretGenerationResult(result))
            advanceUntilIdle()

            val state = viewModel.viewState.value
            assertThat(state.generatorType).isEqualTo(PasswordGeneratorTypeUiModel.PASSWORD)
            assertThat(state.passwordGeneratorSettings).isEqualTo(CUSTOM_PASSWORD_SETTINGS)
            assertThat(state.passphraseGeneratorSettings).isEqualTo(CUSTOM_PASSPHRASE_SETTINGS)
            assertThat(state.passwordData.password).isEqualTo("generated secret")
            assertThat(state.passwordData.passwordEntropyBits).isEqualTo(150.0)
            assertThat(state.passwordData.passwordStrength).isEqualTo(PasswordStrength.VeryStrong)
        }

    @Test
    fun `scan otp result should update state with scanned totp data`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                on { execute(any()) }.thenReturn(
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
                on { execute(any()) }.thenReturn(
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
                on { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = V5Default,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                on { getSecretEntropy(any()) }.thenReturn(0.0)
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

    @Test
    fun `trust new metadata key should dismiss dialog and show trusted snackbar`() =
        runTest {
            stubCreateResultingIn(ResourceCreateActionResult.MetadataKeyModified(NEW_METADATA_KEY_TO_TRUST))
            whenever(mockMetadataPrivateKeysHelperInteractor.trustNewKey(any()))
                .thenReturn(MetadataPrivateKeysHelperInteractor.Output.Success)

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.PASSWORD,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }
            advanceUntilIdle()

            viewModel.onIntent(PasswordTextChanged("strongpassword123!"))
            viewModel.onIntent(CreateResource)
            advanceUntilIdle()
            assertThat(viewModel.viewState.value.metadataKeyModifiedDialog).isEqualTo(NEW_METADATA_KEY_TO_TRUST)

            viewModel.sideEffect.test {
                viewModel.onIntent(TrustNewMetadataKey(NEW_METADATA_KEY_TO_TRUST))
                advanceUntilIdle()

                val sideEffect = awaitItem()
                assertIs<ShowSnackbar>(sideEffect)
                assertThat(sideEffect.type).isEqualTo(SnackbarMessage.METADATA_KEY_IS_TRUSTED)
            }
            val state = viewModel.viewState.value
            assertThat(state.metadataKeyModifiedDialog).isNull()
            assertThat(state.shouldShowDialogProgress).isFalse()
        }

    @Test
    fun `trusted metadata key deleted should dismiss dialog and forget the trusted key`() =
        runTest {
            stubCreateResultingIn(ResourceCreateActionResult.MetadataKeyDeleted(TRUSTED_KEY_DELETED))

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.PASSWORD,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }
            advanceUntilIdle()

            viewModel.onIntent(PasswordTextChanged("strongpassword123!"))
            viewModel.onIntent(CreateResource)
            advanceUntilIdle()
            assertThat(viewModel.viewState.value.metadataKeyDeletedDialog).isEqualTo(TRUSTED_KEY_DELETED)

            viewModel.onIntent(TrustedMetadataKeyDeleted)
            advanceUntilIdle()

            assertThat(viewModel.viewState.value.metadataKeyDeletedDialog).isNull()
            verify(mockMetadataPrivateKeysHelperInteractor).deletedTrustedMetadataPrivateKey()
        }

    @Test
    fun `create resource with pwned password should show data breach warning`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                on { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = V5Default,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                on { getSecretEntropy(any()) }.thenReturn(0.0)
            }
            mockGetPasswordPoliciesUseCase.stub {
                on { execute(any()) }.thenReturn(MOCK_PASSWORD_POLICIES)
            }
            mockCheckPasswordPropertiesUseCase.stub {
                on { execute(any()) }.thenReturn(
                    CheckPasswordPropertiesUseCase.Output.Pwned(dataBreachesCount = 10),
                )
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.PASSWORD,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }
            advanceUntilIdle()

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(PasswordTextChanged("breachedpassword"))
                viewModel.onIntent(CreateResource)
                advanceUntilIdle()

                val state = expectMostRecentItem()
                assertThat(state.showPasswordWarningDialog).isTrue()
                assertThat(state.passwordWarningType).isEqualTo(PasswordWarningType.DATA_BREACH)
            }
        }

    @Test
    fun `create resource with weak password should show low entropy warning`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                on { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = V5Default,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                on { getSecretEntropy(any()) }.thenReturn(0.0)
            }
            mockGetPasswordPoliciesUseCase.stub {
                on { execute(any()) }.thenReturn(MOCK_PASSWORD_POLICIES)
            }
            mockCheckPasswordPropertiesUseCase.stub {
                on { execute(any()) }.thenReturn(
                    CheckPasswordPropertiesUseCase.Output.Weak,
                )
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.PASSWORD,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }
            advanceUntilIdle()

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(PasswordTextChanged("weak"))
                viewModel.onIntent(CreateResource)
                advanceUntilIdle()

                val state = expectMostRecentItem()
                assertThat(state.showPasswordWarningDialog).isTrue()
                assertThat(state.passwordWarningType).isEqualTo(PasswordWarningType.LOW_ENTROPY)
            }
        }

    @Test
    fun `create resource with fine password should not show warning`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                on { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = V5Default,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                on { getSecretEntropy(any()) }.thenReturn(0.0)
            }
            mockGetPasswordPoliciesUseCase.stub {
                on { execute(any()) }.thenReturn(MOCK_PASSWORD_POLICIES)
            }
            mockCheckPasswordPropertiesUseCase.stub {
                on { execute(any()) }.thenReturn(
                    CheckPasswordPropertiesUseCase.Output.Fine,
                )
            }
            mockResourceCreateActionsInteractor.stub {
                on { createGenericResource(any(), anyOrNull(), any(), any()) }.thenReturn(
                    flowOf(ResourceCreateActionResult.Success("id", "name")),
                )
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.PASSWORD,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }
            advanceUntilIdle()

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(PasswordTextChanged("strongpassword123!"))
                viewModel.onIntent(CreateResource)
                advanceUntilIdle()

                val state = expectMostRecentItem()
                assertThat(state.showPasswordWarningDialog).isFalse()
                assertThat(state.passwordWarningType).isNull()
            }
        }

    @Test
    fun `password check failure should not show warning`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                on { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = V5Default,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                on { getSecretEntropy(any()) }.thenReturn(0.0)
            }
            mockGetPasswordPoliciesUseCase.stub {
                on { execute(any()) }.thenReturn(MOCK_PASSWORD_POLICIES)
            }
            mockCheckPasswordPropertiesUseCase.stub {
                on { execute(any()) }.thenReturn(
                    CheckPasswordPropertiesUseCase.Output.Failure,
                )
            }
            mockResourceCreateActionsInteractor.stub {
                on { createGenericResource(any(), anyOrNull(), any(), any()) }.thenReturn(
                    flowOf(ResourceCreateActionResult.Success("id", "name")),
                )
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.PASSWORD,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }
            advanceUntilIdle()

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(PasswordTextChanged("somepassword"))
                viewModel.onIntent(CreateResource)
                advanceUntilIdle()

                val state = expectMostRecentItem()
                assertThat(state.showPasswordWarningDialog).isFalse()
                assertThat(state.passwordWarningType).isNull()
            }
        }

    @Test
    fun `password check should be skipped when external dictionary check is disabled`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                on { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = V5Default,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                on { getSecretEntropy(any()) }.thenReturn(0.0)
            }
            mockGetPasswordPoliciesUseCase.stub {
                on { execute(any()) }.thenReturn(MOCK_PASSWORD_POLICIES_DICTIONARY_CHECK_DISABLED)
            }
            mockCheckPasswordPropertiesUseCase.stub {
                on { execute(any()) }.thenReturn(
                    CheckPasswordPropertiesUseCase.Output.Pwned(dataBreachesCount = 10),
                )
            }
            mockResourceCreateActionsInteractor.stub {
                on { createGenericResource(any(), anyOrNull(), any(), any()) }.thenReturn(
                    flowOf(ResourceCreateActionResult.Success("id", "name")),
                )
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.PASSWORD,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }
            advanceUntilIdle()

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(PasswordTextChanged("breachedpassword"))
                viewModel.onIntent(CreateResource)
                advanceUntilIdle()

                val state = expectMostRecentItem()
                assertThat(state.showPasswordWarningDialog).isFalse()
                assertThat(state.passwordWarningType).isNull()
            }
        }

    @Test
    fun `proceed with password warning should clear warning state`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                on { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = V5Default,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                on { getSecretEntropy(any()) }.thenReturn(0.0)
            }
            mockGetPasswordPoliciesUseCase.stub {
                on { execute(any()) }.thenReturn(MOCK_PASSWORD_POLICIES)
            }
            mockCheckPasswordPropertiesUseCase.stub {
                on { execute(any()) }.thenReturn(
                    CheckPasswordPropertiesUseCase.Output.Pwned(dataBreachesCount = 5),
                )
            }
            mockResourceCreateActionsInteractor.stub {
                on { createGenericResource(any(), anyOrNull(), any(), any()) }.thenReturn(
                    flowOf(ResourceCreateActionResult.Success("id", "name")),
                )
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.PASSWORD,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }
            advanceUntilIdle()

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(PasswordTextChanged("breachedpassword"))
                viewModel.onIntent(CreateResource)
                advanceUntilIdle()

                assertThat(expectMostRecentItem().passwordWarningType).isEqualTo(PasswordWarningType.DATA_BREACH)

                viewModel.onIntent(ProceedWithPasswordWarning)

                val state = expectMostRecentItem()
                assertThat(state.showPasswordWarningDialog).isFalse()
                assertThat(state.passwordWarningType).isNull()
            }
        }

    @Test
    fun `dismiss password warning should clear warning state`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                on { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = V5Default,
                    ),
                )
            }
            mockEntropyCalculator.stub {
                on { getSecretEntropy(any()) }.thenReturn(0.0)
            }
            mockGetPasswordPoliciesUseCase.stub {
                on { execute(any()) }.thenReturn(MOCK_PASSWORD_POLICIES)
            }
            mockCheckPasswordPropertiesUseCase.stub {
                on { execute(any()) }.thenReturn(
                    CheckPasswordPropertiesUseCase.Output.Weak,
                )
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.PASSWORD,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }
            advanceUntilIdle()

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(PasswordTextChanged("weak"))
                viewModel.onIntent(CreateResource)
                advanceUntilIdle()

                assertThat(expectMostRecentItem().passwordWarningType).isEqualTo(PasswordWarningType.LOW_ENTROPY)

                viewModel.onIntent(DismissPasswordWarning)

                val state = expectMostRecentItem()
                assertThat(state.showPasswordWarningDialog).isFalse()
                assertThat(state.passwordWarningType).isNull()
            }
        }

    @Test
    fun `password expiry is not fetched when feature flag is off`() =
        runTest {
            stubCreatePasswordMode()

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.PASSWORD,
                    parentFolderId = null,
                )
            get<ResourceFormViewModel> { parametersOf(mode) }
            advanceUntilIdle()

            verify(mockPasswordExpiryPoliciesInteractor, never()).fetchAndSavePasswordExpiryPolicies()
        }

    @Test
    fun `password expiry is fetched when feature flag is on and fetch succeeds`() =
        runTest {
            stubCreatePasswordMode()
            mockGetFeatureFlagsUseCase.stub {
                on { execute(Unit) }
                    .thenReturn(GetFeatureFlagsUseCase.Output(FEATURE_FLAGS_WITH_PASSWORD_EXPIRY))
            }
            mockPasswordExpiryPoliciesInteractor.stub {
                on { fetchAndSavePasswordExpiryPolicies() }
                    .thenReturn(PasswordExpiryPoliciesInteractor.Output.Success(MOCK_PASSWORD_EXPIRY_SETTINGS))
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.PASSWORD,
                    parentFolderId = null,
                )
            get<ResourceFormViewModel> { parametersOf(mode) }
            advanceUntilIdle()

            verify(mockPasswordExpiryPoliciesInteractor).fetchAndSavePasswordExpiryPolicies()
        }

    @Test
    fun `snackbar is emitted when password expiry fetch fails`() =
        runTest {
            stubCreatePasswordMode()
            mockGetFeatureFlagsUseCase.stub {
                on { execute(Unit) }
                    .thenReturn(GetFeatureFlagsUseCase.Output(FEATURE_FLAGS_WITH_PASSWORD_EXPIRY))
            }
            mockPasswordExpiryPoliciesInteractor.stub {
                on { fetchAndSavePasswordExpiryPolicies() }
                    .thenReturn(
                        PasswordExpiryPoliciesInteractor.Output.Failure.FetchFailure(
                            DomainResult.Incomplete.Error(UNKNOWN, "boom"),
                        ),
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
                val sideEffect = awaitItem()
                assertIs<ShowSnackbar>(sideEffect)
                assertThat(sideEffect.type).isEqualTo(SnackbarMessage.PASSWORD_EXPIRY_FETCH_FAILED)
            }
        }

    @Test
    fun `password policies are not fetched when feature flag is off`() =
        runTest {
            stubCreatePasswordMode()

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.PASSWORD,
                    parentFolderId = null,
                )
            get<ResourceFormViewModel> { parametersOf(mode) }
            advanceUntilIdle()

            verify(mockPasswordPoliciesInteractor, never()).fetchAndSavePasswordPolicies()
        }

    @Test
    fun `password policies are fetched when feature flag is on and fetch succeeds`() =
        runTest {
            stubCreatePasswordMode()
            mockGetFeatureFlagsUseCase.stub {
                on { execute(Unit) }
                    .thenReturn(GetFeatureFlagsUseCase.Output(FEATURE_FLAGS_WITH_PASSWORD_POLICIES))
            }
            mockPasswordPoliciesInteractor.stub {
                on { fetchAndSavePasswordPolicies() }
                    .thenReturn(PasswordPoliciesInteractor.Output.Success(MOCK_PASSWORD_POLICIES))
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.PASSWORD,
                    parentFolderId = null,
                )
            get<ResourceFormViewModel> { parametersOf(mode) }
            advanceUntilIdle()

            verify(mockPasswordPoliciesInteractor).fetchAndSavePasswordPolicies()
        }

    @Test
    fun `snackbar is emitted when password policies fetch fails`() =
        runTest {
            stubCreatePasswordMode()
            mockGetFeatureFlagsUseCase.stub {
                on { execute(Unit) }
                    .thenReturn(GetFeatureFlagsUseCase.Output(FEATURE_FLAGS_WITH_PASSWORD_POLICIES))
            }
            mockPasswordPoliciesInteractor.stub {
                on { fetchAndSavePasswordPolicies() }
                    .thenReturn(PasswordPoliciesInteractor.Output.Failure.ValidationFailure)
            }

            val mode =
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.PASSWORD,
                    parentFolderId = null,
                )
            val viewModel: ResourceFormViewModel = get { parametersOf(mode) }

            viewModel.sideEffect.test {
                advanceUntilIdle()
                val sideEffect = awaitItem()
                assertIs<ShowSnackbar>(sideEffect)
                assertThat(sideEffect.type).isEqualTo(SnackbarMessage.PASSWORD_POLICIES_FETCH_FAILED)
            }
        }

    private fun stubCreatePasswordMode() {
        mockGetDefaultCreateContentTypeUseCase.stub {
            on { execute(any()) }.thenReturn(
                GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                    metadataType = MetadataTypeModel.V5,
                    contentType = V5Default,
                ),
            )
        }
        mockEntropyCalculator.stub {
            on { getSecretEntropy(any()) }.thenReturn(0.0)
        }
    }

    private fun stubCreateResultingIn(result: ResourceCreateActionResult) {
        stubCreatePasswordMode()
        mockGetPasswordPoliciesUseCase.stub {
            on { execute(any()) }.thenReturn(MOCK_PASSWORD_POLICIES)
        }
        mockCheckPasswordPropertiesUseCase.stub {
            on { execute(any()) }.thenReturn(CheckPasswordPropertiesUseCase.Output.Fine)
        }
        mockResourceCreateActionsInteractor.stub {
            on { createGenericResource(any(), anyOrNull(), any(), any()) }.thenReturn(flowOf(result))
        }
    }

    @Test
    fun `upgrade panel should be shown when feature flag, settings and v4 resource all allow it`() =
        runTest {
            stubEditModeFor(slug = PasswordAndDescription.slug, contentType = PasswordAndDescription)
            stubFeatureFlagsAndSettings(isV5MetadataAvailable = true, allowV4V5Upgrade = true, allowCreationOfV5Resources = true)

            val viewModel: ResourceFormViewModel = get { parametersOf(EDIT_MODE) }
            advanceUntilIdle()

            assertThat(viewModel.viewState.value.showUpgradePanel).isTrue()
        }

    @Test
    fun `upgrade panel should be hidden when v5 metadata feature flag is off`() =
        runTest {
            stubEditModeFor(slug = PasswordAndDescription.slug, contentType = PasswordAndDescription)
            stubFeatureFlagsAndSettings(isV5MetadataAvailable = false, allowV4V5Upgrade = true, allowCreationOfV5Resources = true)

            val viewModel: ResourceFormViewModel = get { parametersOf(EDIT_MODE) }
            advanceUntilIdle()

            assertThat(viewModel.viewState.value.showUpgradePanel).isFalse()
        }

    @Test
    fun `upgrade panel should be hidden when v4 to v5 upgrade is not allowed`() =
        runTest {
            stubEditModeFor(slug = PasswordAndDescription.slug, contentType = PasswordAndDescription)
            stubFeatureFlagsAndSettings(isV5MetadataAvailable = true, allowV4V5Upgrade = false, allowCreationOfV5Resources = true)

            val viewModel: ResourceFormViewModel = get { parametersOf(EDIT_MODE) }
            advanceUntilIdle()

            assertThat(viewModel.viewState.value.showUpgradePanel).isFalse()
        }

    @Test
    fun `upgrade panel should be hidden when v5 resource creation is not allowed`() =
        runTest {
            stubEditModeFor(slug = PasswordAndDescription.slug, contentType = PasswordAndDescription)
            stubFeatureFlagsAndSettings(isV5MetadataAvailable = true, allowV4V5Upgrade = true, allowCreationOfV5Resources = false)

            val viewModel: ResourceFormViewModel = get { parametersOf(EDIT_MODE) }
            advanceUntilIdle()

            assertThat(viewModel.viewState.value.showUpgradePanel).isFalse()
        }

    @Test
    fun `upgrade panel should be hidden when resource is already v5`() =
        runTest {
            stubEditModeFor(slug = V5Default.slug, contentType = V5Default)
            stubFeatureFlagsAndSettings(isV5MetadataAvailable = true, allowV4V5Upgrade = true, allowCreationOfV5Resources = true)

            val viewModel: ResourceFormViewModel = get { parametersOf(EDIT_MODE) }
            advanceUntilIdle()

            assertThat(viewModel.viewState.value.showUpgradePanel).isFalse()
        }

    @Test
    fun `upgrade resource should emit resource upgraded snackbar on success`() =
        runTest {
            stubEditModeFor(slug = PasswordAndDescription.slug, contentType = PasswordAndDescription)
            stubFeatureFlagsAndSettings(isV5MetadataAvailable = true, allowV4V5Upgrade = true, allowCreationOfV5Resources = true)
            stubUpgradeResult(ResourceUpdateActionResult.Success(resourceId = "id", resourceName = "name"))

            val viewModel: ResourceFormViewModel = get { parametersOf(EDIT_MODE) }
            advanceUntilIdle()

            viewModel.sideEffect.test {
                viewModel.onIntent(UpgradeResource)
                advanceUntilIdle()
                val sideEffect = awaitItem()
                assertIs<ShowSnackbar>(sideEffect)
                assertThat(sideEffect.type).isEqualTo(SnackbarMessage.RESOURCE_UPGRADED)
            }
        }

    @Test
    fun `upgrade resource should emit common failure snackbar when update fails`() =
        runTest {
            stubEditModeFor(slug = PasswordAndDescription.slug, contentType = PasswordAndDescription)
            stubFeatureFlagsAndSettings(isV5MetadataAvailable = true, allowV4V5Upgrade = true, allowCreationOfV5Resources = true)
            stubUpgradeResult(Failure())

            val viewModel: ResourceFormViewModel = get { parametersOf(EDIT_MODE) }
            advanceUntilIdle()

            viewModel.sideEffect.test {
                viewModel.onIntent(UpgradeResource)
                advanceUntilIdle()
                val sideEffect = awaitItem()
                assertIs<ShowSnackbar>(sideEffect)
                assertThat(sideEffect.type).isEqualTo(COMMON_FAILURE)
            }
        }

    @Test
    fun `upgrade resource should emit cannot create snackbar when config disallows update`() =
        runTest {
            stubEditModeFor(slug = PasswordAndDescription.slug, contentType = PasswordAndDescription)
            stubFeatureFlagsAndSettings(isV5MetadataAvailable = true, allowV4V5Upgrade = true, allowCreationOfV5Resources = true)
            stubUpgradeResult(CannotUpdateWithCurrentConfig)

            val viewModel: ResourceFormViewModel = get { parametersOf(EDIT_MODE) }
            advanceUntilIdle()

            viewModel.sideEffect.test {
                viewModel.onIntent(UpgradeResource)
                advanceUntilIdle()
                val sideEffect = awaitItem()
                assertIs<ShowSnackbar>(sideEffect)
                assertThat(sideEffect.type).isEqualTo(CANNOT_CREATE_RESOURCE_WITH_CURRENT_CONFIG)
            }
        }

    @Test
    fun `learn more about upgrade should emit open website side effect`() =
        runTest {
            stubEditModeFor(slug = PasswordAndDescription.slug, contentType = PasswordAndDescription)
            stubFeatureFlagsAndSettings(isV5MetadataAvailable = true, allowV4V5Upgrade = true, allowCreationOfV5Resources = true)

            val viewModel: ResourceFormViewModel = get { parametersOf(EDIT_MODE) }
            advanceUntilIdle()

            viewModel.sideEffect.test {
                viewModel.onIntent(LearnMoreAboutUpgrade)
                advanceUntilIdle()
                val sideEffect = awaitItem()
                assertIs<OpenWebsite>(sideEffect)
                assertThat(sideEffect.url).isNotEmpty()
            }
        }

    private fun stubEditModeFor(
        slug: String,
        contentType: ContentType,
    ) {
        val resource = createResourceModel(slug = slug)
        mockGetLocalResourceUseCase.stub {
            on { execute(any()) }.thenReturn(GetLocalResourceUseCase.Output(resource))
        }
        mockGetEditContentTypeUseCase.stub {
            on { execute(any()) }.thenReturn(
                GetEditContentTypeUseCase.Output(contentType = contentType, metadataType = V4),
            )
        }
        mockEntropyCalculator.stub {
            on { getSecretEntropy(any()) }.thenReturn(0.0)
        }
        val secretInteractorMock = mock<SecretPropertiesActionsInteractor>()
        secretInteractorMock.stub {
            on { provideDecryptedSecret() }.thenReturn(
                flowOf(
                    SecretPropertyActionResult.Success(
                        label = "secret",
                        isSecret = true,
                        result = SecretJsonModel("""{"password": ""}"""),
                    ),
                ),
            )
        }
        mockSecretPropertiesActionsInteractorSecretPropertiesActionsInteractorFactory.stub {
            on { create(any()) }.thenReturn(secretInteractorMock)
        }
    }

    private fun stubFeatureFlagsAndSettings(
        isV5MetadataAvailable: Boolean,
        allowV4V5Upgrade: Boolean,
        allowCreationOfV5Resources: Boolean,
    ) {
        mockGetFeatureFlagsUseCase.stub {
            on { execute(Unit) }.thenReturn(
                GetFeatureFlagsUseCase.Output(
                    DEFAULT_FEATURE_FLAGS.copy(isV5MetadataAvailable = isV5MetadataAvailable),
                ),
            )
        }
        mockGetMetadataTypesSettingsUseCase.stub {
            on { execute(Unit) }.thenReturn(
                GetMetadataTypesSettingsUseCase.Output(
                    DEFAULT_METADATA_TYPES_SETTINGS.copy(
                        allowV4V5Upgrade = allowV4V5Upgrade,
                        allowCreationOfV5Resources = allowCreationOfV5Resources,
                    ),
                ),
            )
        }
    }

    private fun stubUpgradeResult(result: ResourceUpdateActionResult) {
        val upgradeInteractor = mock<ResourceUpdateActionsInteractor>()
        upgradeInteractor.stub {
            on { upgradeToV5() }.thenReturn(flowOf(result))
        }
        mockResourceUpdateActionsInteractorFactory.stub {
            on { create(any()) }.thenReturn(upgradeInteractor)
        }
    }

    private fun createResourceModel(slug: String): ResourceUiModel =
        ResourceUiModel(
            resourceId = "resourceId",
            resourceTypeId = "resourceTypeId",
            slug = slug,
            folderId = null,
            permission = OWNER,
            favouriteId = null,
            modified = ZonedDateTime.now(),
            expiry = null,
            metadataKeyId = null,
            metadataKeyType = PERSONAL,
            metadataJsonModel = MetadataJsonModel("""{"name": "Test"}"""),
        )

    private companion object {
        val FEATURE_FLAGS_WITH_PASSWORD_POLICIES =
            DEFAULT_TEST_FEATURE_FLAGS.copy(arePasswordPoliciesAvailable = true)

        val EDIT_MODE = ResourceFormMode.Edit(resourceId = "resourceId", resourceName = "Test")

        val MOCK_PASSWORD_POLICIES =
            PasswordPoliciesUiModel(
                defaultGenerator = PasswordGeneratorTypeUiModel.PASSWORD,
                passwordGeneratorSettings =
                    PasswordGeneratorSettingsUiModel(
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
                    PassphraseGeneratorSettingsUiModel(
                        words = 9,
                        wordSeparator = " ",
                        wordCase = LOWERCASE,
                    ),
                isExternalDictionaryCheckEnabled = true,
            )

        val MOCK_PASSWORD_POLICIES_DICTIONARY_CHECK_DISABLED =
            MOCK_PASSWORD_POLICIES.copy(isExternalDictionaryCheckEnabled = false)

        val CUSTOM_PASSWORD_SETTINGS =
            PasswordGeneratorSettingsUiModel(
                length = 24,
                maskUpper = true,
                maskLower = true,
                maskDigit = true,
                maskParenthesis = false,
                maskEmoji = false,
                maskChar1 = false,
                maskChar2 = false,
                maskChar3 = false,
                maskChar4 = false,
                maskChar5 = false,
                excludeLookAlikeChars = false,
            )

        val CUSTOM_PASSPHRASE_SETTINGS =
            PassphraseGeneratorSettingsUiModel(
                words = 7,
                wordSeparator = "-",
                wordCase = CaseTypeUiModel.UPPERCASE,
            )

        val FEATURE_FLAGS_WITH_PASSWORD_EXPIRY =
            DEFAULT_TEST_FEATURE_FLAGS.copy(isPasswordExpiryAvailable = true)

        val MOCK_PASSWORD_EXPIRY_SETTINGS =
            PasswordExpirySettings(
                automaticExpiry = true,
                automaticUpdate = true,
                defaultExpiryPeriodDays = 90,
            )

        val NEW_METADATA_KEY_TO_TRUST =
            NewMetadataKeyToTrustModel(
                id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
                signedUsername = "ada@passbolt.com",
                signedName = "Ada Lovelace",
                signatureCreationTimestampSeconds = 0L,
                signatureKeyFingerprint = "signatureFingerprint",
                metadataPrivateKey =
                    ParsedMetadataPrivateKeyModel(
                        id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
                        userId = UUID.fromString("00000000-0000-0000-0000-000000000002"),
                        keyData = "keyData",
                        passphrase = "",
                        created = ZonedDateTime.now(),
                        createdBy = null,
                        modified = ZonedDateTime.now(),
                        modifiedBy = null,
                        fingerprint = "keyFingerprint",
                        domain = "https://passbolt.test",
                        pgpMessage = "pgpMessage",
                    ),
                modificationKind = MetadataKeyModification.ROTATION,
            )

        val TRUSTED_KEY_DELETED =
            TrustedKeyDeletedModel(
                keyFingerprint = "keyFingerprint",
                signedUsername = "ada@passbolt.com",
                signedName = "Ada Lovelace",
            )
    }
}
