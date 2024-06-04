package com.passbolt.mobile.android.feature.authentication.auth.presenter

import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.networking.NetworkResult
import com.passbolt.mobile.android.core.policies.usecase.PasswordExpiryPoliciesInteractor
import com.passbolt.mobile.android.core.policies.usecase.PasswordPoliciesInteractor
import com.passbolt.mobile.android.core.rbac.usecase.RbacInteractor
import com.passbolt.mobile.android.core.users.profile.UserProfileInteractor
import com.passbolt.mobile.android.dto.response.ChallengeResponseDto
import com.passbolt.mobile.android.entity.featureflags.FeatureFlagsModel
import com.passbolt.mobile.android.feature.authentication.auth.AuthContract
import com.passbolt.mobile.android.feature.authentication.auth.challenge.ChallengeDecryptor
import com.passbolt.mobile.android.feature.authentication.auth.challenge.ChallengeProvider
import com.passbolt.mobile.android.feature.authentication.auth.challenge.ChallengeVerifier
import com.passbolt.mobile.android.feature.authentication.auth.challenge.MfaStatus
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetServerPublicPgpKeyUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetServerPublicRsaKeyUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GopenPgpTimeUpdater
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignInFailureType
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignInUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.VerifyPassphraseUseCase
import com.passbolt.mobile.android.featureflags.usecase.FeatureFlagsInteractor
import com.passbolt.mobile.android.storage.usecase.accountdata.IsServerFingerprintCorrectUseCase
import com.passbolt.mobile.android.storage.usecase.passphrase.CheckIfPassphraseFileExistsUseCase
import com.passbolt.mobile.android.storage.usecase.preferences.GetGlobalPreferencesUseCase
import com.passbolt.mobile.android.storage.usecase.session.GetSessionUseCase
import com.passbolt.mobile.android.ui.CaseTypeModel
import com.passbolt.mobile.android.ui.PassphraseGeneratorSettingsModel
import com.passbolt.mobile.android.ui.PasswordExpirySettings
import com.passbolt.mobile.android.ui.PasswordGeneratorSettingsModel
import com.passbolt.mobile.android.ui.PasswordGeneratorTypeModel
import com.passbolt.mobile.android.ui.PasswordPolicies
import com.passbolt.mobile.android.ui.RbacModel
import com.passbolt.mobile.android.ui.RbacRuleModel.ALLOW
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.core.parameter.parametersOf
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.Instant

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

@ExperimentalCoroutinesApi
class SignInPresenterTest : KoinTest {

    private val presenter: AuthContract.Presenter by inject {
        parametersOf(ActivityIntents.AuthConfig.SignIn)
    }
    private val mockView = mock<AuthContract.View>()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(testAuthModule)
    }

    @Before
    fun setup() {
        whenever(mockGetGlobalPreferencesUseCase.execute(Unit))
            .doReturn(
                GetGlobalPreferencesUseCase.Output(
                    areDebugLogsEnabled = false, debugLogFileCreationDateTime = null,
                    isDeveloperModeEnabled = false, isHideRootDialogEnabled = false
                )
            )
        whenever(mockGopenPgpTimeUpdater.updateTimeIfNeeded(any(), any()))
            .thenReturn(GopenPgpTimeUpdater.Result.TIME_SYNCED)
        mockPasswordExpiryPoliciesInteractor.stub {
            onBlocking { fetchAndSavePasswordExpiryPolicies() }.doReturn(
                PasswordExpiryPoliciesInteractor.Output.Success(
                    PasswordExpirySettings(
                        automaticExpiry = true,
                        automaticUpdate = true,
                        defaultExpiryPeriodDays = 90
                    )
                )
            )
        }
        mockPasswordPoliciesInteractor.stub {
            onBlocking { fetchAndSavePasswordPolicies() }.doReturn(
                PasswordPoliciesInteractor.Output.Success(
                    PasswordPolicies(
                        defaultGenerator = PasswordGeneratorTypeModel.PASSWORD,
                        passwordGeneratorSettings = PasswordGeneratorSettingsModel(
                            length = 12,
                            maskUpper = true,
                            maskLower = true,
                            maskDigit = true,
                            maskEmoji = true,
                            maskParenthesis = true,
                            maskChar1 = true,
                            maskChar2 = true,
                            maskChar3 = true,
                            maskChar4 = true,
                            maskChar5 = true,
                            excludeLookAlikeChars = true
                        ),
                        passphraseGeneratorSettings = PassphraseGeneratorSettingsModel(
                            words = 4,
                            wordSeparator = "-",
                            wordCase = CaseTypeModel.LOWERCASE

                        ),
                        isExternalDictionaryCheckEnabled = false
                    )
                )
            )
        }
    }

    @Test
    fun `view should show error when server public keys cannot be fetched`() {
        mockVerifyPassphraseUseCase.stub {
            onBlocking { execute(any()) }.doReturn(VerifyPassphraseUseCase.Output(true))
        }
        mockGetServerPublicPgpKeyUseCase.stub {
            onBlocking { execute(any()) }.thenReturn(
                GetServerPublicPgpKeyUseCase.Output.Failure(
                    NetworkResult.Failure.NetworkError(UnknownHostException(), "")
                )
            )
        }
        whenever(mockCheckIfPassphraseExistsUseCase.execute(anyOrNull()))
            .doReturn(CheckIfPassphraseFileExistsUseCase.Output(passphraseFileExists = false))

        presenter.argsRetrieved(ActivityIntents.AuthConfig.SignIn, ACCOUNT)
        presenter.attach(mockView)
        presenter.signInClick(SAMPLE_PASSPHRASE)

        verify(mockView).showTitle()
        verify(mockView).hideKeyboard()
        verify(mockView).showAuthenticationReason(AuthContract.View.RefreshAuthReason.SESSION)
        verify(mockView).showProgress()
        verify(mockView).hideProgress()
        verify(mockView).showGenericError()
        verifyNoMoreInteractions(mockView)
    }

    @Test
    fun `view should show server not reachable when cannot fetch public keys`() {
        mockVerifyPassphraseUseCase.stub {
            onBlocking { execute(any()) }.doReturn(VerifyPassphraseUseCase.Output(true))
        }
        mockGetServerPublicPgpKeyUseCase.stub {
            onBlocking { execute(any()) }.thenReturn(
                GetServerPublicPgpKeyUseCase.Output.Failure(
                    NetworkResult.Failure.ServerError(SocketTimeoutException(), headerMessage = "")
                )
            )
        }
        whenever(mockCheckIfPassphraseExistsUseCase.execute(anyOrNull()))
            .doReturn(CheckIfPassphraseFileExistsUseCase.Output(passphraseFileExists = false))

        presenter.argsRetrieved(ActivityIntents.AuthConfig.SignIn, ACCOUNT)
        presenter.attach(mockView)
        presenter.signInClick(SAMPLE_PASSPHRASE)

        verify(mockView).showTitle()
        verify(mockView).hideKeyboard()
        verify(mockView).showAuthenticationReason(AuthContract.View.RefreshAuthReason.SESSION)
        verify(mockView).showProgress()
        verify(mockView).hideProgress()
        verify(mockView).showServerNotReachable(any())
        verifyNoMoreInteractions(mockView)
    }

    @Test
    fun `view should show auth success when sign in in succeeds`() {
        mockVerifyPassphraseUseCase.stub {
            onBlocking { execute(any()) }.doReturn(VerifyPassphraseUseCase.Output(true))
        }
        mockGetServerPublicPgpKeyUseCase.stub {
            onBlocking { execute(any()) }.thenReturn(
                GetServerPublicPgpKeyUseCase.Output.Success(
                    "publicKey",
                    "fingerprint",
                    Instant.now().epochSecond
                )
            )
        }
        mockGetServerPublicRsaKeyUseCase.stub {
            onBlocking { execute(any()) }.thenReturn(GetServerPublicRsaKeyUseCase.Output.Success("publicRsa"))
        }
        whenever(mockIsServerFingerprintCorrectUseCase.execute(any()))
            .doReturn(IsServerFingerprintCorrectUseCase.Output(true))
        mockChallengeProvider.stub {
            onBlocking { get(any(), any(), any(), any()) }.doReturn(
                ChallengeProvider.Output.Success("challenge")
            )
        }
        mockSignInUseCase.stub {
            onBlocking { execute(any()) }
                .doReturn(SignInUseCase.Output.Success("challenge", "mfa"))
        }
        mockChallengeDecryptor.stub {
            onBlocking { decrypt(any(), any(), any(), any()) }.doReturn(
                ChallengeDecryptor.Output.DecryptedChallenge(
                    ChallengeResponseDto("1", "domain", "token", "accessToken", "refreshToken", null)
                )
            )
        }
        mockChallengeVerifier.stub {
            onBlocking { verify(any(), any()) }.doReturn(
                ChallengeVerifier.Output.Verified("accessToken", "refreshToken")
            )
        }
        whenever(mockCheckIfPassphraseExistsUseCase.execute(anyOrNull()))
            .doReturn(CheckIfPassphraseFileExistsUseCase.Output(passphraseFileExists = false))
        mockFeatureFlagsInteractor.stub {
            onBlocking { fetchAndSaveFeatureFlags() }.doReturn(
                FeatureFlagsInteractor.Output.Success(
                    FeatureFlagsModel(
                        privacyPolicyUrl = null,
                        termsAndConditionsUrl = null,
                        isPreviewPasswordAvailable = true,
                        areFoldersAvailable = false,
                        areTagsAvailable = true,
                        isTotpAvailable = true,
                        isRbacAvailable = true,
                        isPasswordExpiryAvailable = true,
                        arePasswordPoliciesAvailable = true,
                        canUpdatePasswordPolicies = true
                    )
                )
            )
        }
        mockRbacInteractor.stub {
            onBlocking { fetchAndSaveRbacRulesFlags() }.doReturn(
                RbacInteractor.Output.Success(
                    RbacModel(
                        passwordPreviewRule = ALLOW,
                        passwordCopyRule = ALLOW,
                        tagsUseRule = ALLOW,
                        shareViewRule = ALLOW,
                        foldersUseRule = ALLOW
                    )
                )
            )
        }
        whenever(mockMfaStatusProvider.provideMfaStatus()).doReturn(MfaStatus.NOT_REQUIRED)
        whenever(mockGetSessionUseCase.execute(anyOrNull()))
            .doReturn(GetSessionUseCase.Output("access", "refresh", "mfa"))
        mockProfileInteractor.stub {
            onBlocking { fetchAndUpdateUserProfile() }.doReturn(UserProfileInteractor.Output.Success)
        }

        presenter.argsRetrieved(ActivityIntents.AuthConfig.SignIn, ACCOUNT)
        presenter.attach(mockView)
        presenter.signInClick(SAMPLE_PASSPHRASE)

        verify(mockInAppReviewInteractor).processSuccessfulSignIn()
        verify(mockView).showProgress()
        verify(mockView).hideProgress()
        verify(mockView).authSuccess()
    }

    @Test
    fun `view should show wrong passphrase when challenge provider cannot encrypt challenge`() {
        mockVerifyPassphraseUseCase.stub {
            onBlocking { execute(any()) }.doReturn(VerifyPassphraseUseCase.Output(true))
        }
        mockGetServerPublicPgpKeyUseCase.stub {
            onBlocking { execute(any()) }.thenReturn(
                GetServerPublicPgpKeyUseCase.Output.Success(
                    "publickKey",
                    "fingerprint",
                    Instant.now().epochSecond
                )
            )
        }
        mockGetServerPublicRsaKeyUseCase.stub {
            onBlocking { execute(any()) }.thenReturn(GetServerPublicRsaKeyUseCase.Output.Success("publicRsa"))
        }
        mockChallengeProvider.stub {
            onBlocking { get(any(), any(), any(), any()) }.doReturn(
                ChallengeProvider.Output.WrongPassphrase
            )
        }
        whenever(mockCheckIfPassphraseExistsUseCase.execute(anyOrNull()))
            .doReturn(CheckIfPassphraseFileExistsUseCase.Output(passphraseFileExists = false))

        presenter.argsRetrieved(ActivityIntents.AuthConfig.SignIn, ACCOUNT)
        presenter.attach(mockView)
        presenter.signInClick(SAMPLE_PASSPHRASE)

        verify(mockView).showProgress()
        verify(mockView).hideProgress()
        verify(mockView).showWrongPassphrase()
    }

    @Test
    fun `view should show decryption error if challenge cannot be decrypted`() {
        val errorMessage = "error message"
        mockVerifyPassphraseUseCase.stub {
            onBlocking { execute(any()) }.doReturn(VerifyPassphraseUseCase.Output(true))
        }
        mockGetServerPublicPgpKeyUseCase.stub {
            onBlocking { execute(any()) }.thenReturn(
                GetServerPublicPgpKeyUseCase.Output.Success(
                    "publickKey",
                    "fingerprint",
                    Instant.now().epochSecond
                )
            )
        }
        mockGetServerPublicRsaKeyUseCase.stub {
            onBlocking { execute(any()) }.thenReturn(GetServerPublicRsaKeyUseCase.Output.Success("publicRsa"))
        }
        mockChallengeProvider.stub {
            onBlocking { get(any(), any(), any(), any()) }.doReturn(
                ChallengeProvider.Output.Success("challenge")
            )
        }
        mockSignInUseCase.stub {
            onBlocking { execute(any()) }
                .doReturn(SignInUseCase.Output.Success("challenge", "mfa"))
        }
        mockChallengeDecryptor.stub {
            onBlocking { decrypt(any(), any(), any(), any()) }.doReturn(
                ChallengeDecryptor.Output.DecryptionError(errorMessage)
            )
        }
        mockChallengeVerifier.stub {
            onBlocking { verify(any(), any()) }.doReturn(
                ChallengeVerifier.Output.Failure
            )
        }
        whenever(mockCheckIfPassphraseExistsUseCase.execute(anyOrNull()))
            .doReturn(CheckIfPassphraseFileExistsUseCase.Output(passphraseFileExists = false))
        whenever(mockGetSessionUseCase.execute(anyOrNull()))
            .doReturn(GetSessionUseCase.Output("access", "refresh", "mfa"))

        presenter.argsRetrieved(ActivityIntents.AuthConfig.SignIn, ACCOUNT)
        presenter.attach(mockView)
        presenter.signInClick(SAMPLE_PASSPHRASE)

        verify(mockView).showProgress()
        verify(mockView).hideProgress()
        verify(mockView).showDecryptionError(errorMessage)
    }

    @Test
    fun `view should show server error message when sign in fails`() {
        mockVerifyPassphraseUseCase.stub {
            onBlocking { execute(any()) }.doReturn(VerifyPassphraseUseCase.Output(true))
        }
        mockGetServerPublicPgpKeyUseCase.stub {
            onBlocking { execute(any()) }.thenReturn(
                GetServerPublicPgpKeyUseCase.Output.Success(
                    "publickKey",
                    "fingerprint",
                    Instant.now().epochSecond
                )
            )
        }
        mockGetServerPublicRsaKeyUseCase.stub {
            onBlocking { execute(any()) }.thenReturn(GetServerPublicRsaKeyUseCase.Output.Success("publicRsa"))
        }
        mockIsServerFingerprintCorrectUseCase.stub {
            onBlocking { execute(any()) }.thenReturn(IsServerFingerprintCorrectUseCase.Output(true))
        }
        mockChallengeProvider.stub {
            onBlocking { get(any(), any(), any(), any()) }.doReturn(
                ChallengeProvider.Output.Success("challenge")
            )
        }
        mockSignInUseCase.stub {
            onBlocking { execute(any()) }.thenReturn(
                SignInUseCase.Output.Failure(ERROR_MESSAGE, SignInFailureType.OTHER)
            )
        }
        whenever(mockCheckIfPassphraseExistsUseCase.execute(anyOrNull()))
            .doReturn(CheckIfPassphraseFileExistsUseCase.Output(passphraseFileExists = false))
        whenever(mockGetSessionUseCase.execute(anyOrNull()))
            .doReturn(GetSessionUseCase.Output("access", "refresh", "mfa"))

        presenter.argsRetrieved(ActivityIntents.AuthConfig.SignIn, ACCOUNT)
        presenter.attach(mockView)
        presenter.signInClick(SAMPLE_PASSPHRASE)

        verify(mockView).showProgress()
        verify(mockView).hideProgress()
        verify(mockView).showError(ERROR_MESSAGE)
    }

    @Test
    fun `view should show server fingerprint changed error message when fingerprint changed`() {
        mockVerifyPassphraseUseCase.stub {
            onBlocking { execute(any()) }.doReturn(VerifyPassphraseUseCase.Output(true))
        }
        mockGetServerPublicPgpKeyUseCase.stub {
            onBlocking { execute(any()) }.thenReturn(
                GetServerPublicPgpKeyUseCase.Output.Success(
                    "publickKey",
                    "fingerprint",
                    Instant.now().epochSecond
                )
            )
        }
        mockGetServerPublicRsaKeyUseCase.stub {
            onBlocking { execute(any()) }.thenReturn(GetServerPublicRsaKeyUseCase.Output.Success("publicRsa"))
        }
        mockIsServerFingerprintCorrectUseCase.stub {
            onBlocking { execute(any()) }.thenReturn(IsServerFingerprintCorrectUseCase.Output(false))
        }
        mockChallengeProvider.stub {
            onBlocking { get(any(), any(), any(), any()) }.doReturn(
                ChallengeProvider.Output.Success("challenge")
            )
        }
        mockSignInUseCase.stub {
            onBlocking { execute(any()) }.thenReturn(
                SignInUseCase.Output.Failure(
                    ERROR_MESSAGE,
                    SignInFailureType.OTHER
                )
            )
        }
        whenever(mockCheckIfPassphraseExistsUseCase.execute(anyOrNull()))
            .doReturn(CheckIfPassphraseFileExistsUseCase.Output(passphraseFileExists = false))

        presenter.argsRetrieved(ActivityIntents.AuthConfig.SignIn, ACCOUNT)
        presenter.attach(mockView)
        presenter.signInClick(SAMPLE_PASSPHRASE)

        verify(mockView).showProgress()
        verify(mockView).hideProgress()
        verify(mockView).showServerFingerprintChanged("fingerprint")
    }

    @Test
    fun `view should show generic error when challenge cannot be verified`() {
        mockVerifyPassphraseUseCase.stub {
            onBlocking { execute(any()) }.doReturn(VerifyPassphraseUseCase.Output(true))
        }
        mockGetServerPublicPgpKeyUseCase.stub {
            onBlocking { execute(any()) }.thenReturn(
                GetServerPublicPgpKeyUseCase.Output.Success(
                    "publickKey",
                    "fingerprint",
                    Instant.now().epochSecond
                )
            )
        }
        mockGetServerPublicRsaKeyUseCase.stub {
            onBlocking { execute(any()) }.thenReturn(GetServerPublicRsaKeyUseCase.Output.Success("publicRsa"))
        }
        mockChallengeProvider.stub {
            onBlocking { get(any(), any(), any(), any()) }.doReturn(
                ChallengeProvider.Output.Success("challenge")
            )
        }
        mockSignInUseCase.stub {
            onBlocking { execute(any()) }
                .doReturn(SignInUseCase.Output.Success("challenge", "mfa"))
        }
        mockChallengeDecryptor.stub {
            onBlocking { decrypt(any(), any(), any(), any()) }.doReturn(
                ChallengeDecryptor.Output.DecryptedChallenge(
                    ChallengeResponseDto("1", "domain", "token", "accessToken", "refreshToken", null)
                )
            )
        }
        mockChallengeVerifier.stub {
            onBlocking { verify(any(), any()) }.doReturn(
                ChallengeVerifier.Output.Failure
            )
        }
        mockIsServerFingerprintCorrectUseCase.stub {
            onBlocking { execute(any()) }.thenReturn(IsServerFingerprintCorrectUseCase.Output(true))
        }
        whenever(mockGetSessionUseCase.execute(anyOrNull()))
            .doReturn(GetSessionUseCase.Output("access", "refresh", "mfa"))
        whenever(mockCheckIfPassphraseExistsUseCase.execute(anyOrNull()))
            .doReturn(CheckIfPassphraseFileExistsUseCase.Output(passphraseFileExists = false))

        presenter.argsRetrieved(ActivityIntents.AuthConfig.SignIn, ACCOUNT)
        presenter.attach(mockView)
        presenter.signInClick(SAMPLE_PASSPHRASE)

        verify(mockView).showProgress()
        verify(mockView).hideProgress()
        verify(mockView).showGenericError()
    }

    @Test
    fun `view should show account deleted and account list when sign in 404`() {
        mockVerifyPassphraseUseCase.stub {
            onBlocking { execute(any()) }.doReturn(VerifyPassphraseUseCase.Output(true))
        }
        mockGetServerPublicPgpKeyUseCase.stub {
            onBlocking { execute(any()) }.thenReturn(
                GetServerPublicPgpKeyUseCase.Output.Success(
                    "publickKey",
                    "fingerprint",
                    Instant.now().epochSecond
                )
            )
        }
        mockGetServerPublicRsaKeyUseCase.stub {
            onBlocking { execute(any()) }.thenReturn(GetServerPublicRsaKeyUseCase.Output.Success("publicRsa"))
        }
        mockChallengeProvider.stub {
            onBlocking { get(any(), any(), any(), any()) }.doReturn(
                ChallengeProvider.Output.Success("challenge")
            )
        }

        mockSignInUseCase.stub {
            onBlocking { execute(any()) }.thenReturn(
                SignInUseCase.Output.Failure(ERROR_MESSAGE, SignInFailureType.ACCOUNT_DOES_NOT_EXIST)
            )
        }

        whenever(mockCheckIfPassphraseExistsUseCase.execute(anyOrNull()))
            .doReturn(CheckIfPassphraseFileExistsUseCase.Output(passphraseFileExists = false))

        presenter.argsRetrieved(ActivityIntents.AuthConfig.SignIn, ACCOUNT)
        presenter.attach(mockView)
        presenter.signInClick(SAMPLE_PASSPHRASE)

        verify(mockView).showProgress()
        verify(mockView).hideProgress()
        verify(mockView).showAccountDoesNotExistDialog(
            MOCK_LABEL,
            MOCK_ACCOUNT_DATA_EMAIL,
            MOCK_ACCOUNT_DATA_URL
        )
    }

    private companion object {
        private const val ERROR_MESSAGE = "error"
        private val SAMPLE_PASSPHRASE = "pass".toByteArray()
    }
}
