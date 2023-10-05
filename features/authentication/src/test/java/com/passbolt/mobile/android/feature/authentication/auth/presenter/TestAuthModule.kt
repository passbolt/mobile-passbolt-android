package com.passbolt.mobile.android.feature.authentication.auth.presenter

import com.passbolt.mobile.android.common.FingerprintInformationProvider
import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.core.idlingresource.SignInIdlingResource
import com.passbolt.mobile.android.core.inappreview.InAppReviewInteractor
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.security.rootdetection.RootDetectorImpl
import com.passbolt.mobile.android.core.security.runtimeauth.RuntimeAuthenticatedFlag
import com.passbolt.mobile.android.core.users.profile.UserProfileInteractor
import com.passbolt.mobile.android.feature.authentication.auth.AuthContract
import com.passbolt.mobile.android.feature.authentication.auth.challenge.ChallengeDecryptor
import com.passbolt.mobile.android.feature.authentication.auth.challenge.ChallengeProvider
import com.passbolt.mobile.android.feature.authentication.auth.challenge.ChallengeVerifier
import com.passbolt.mobile.android.feature.authentication.auth.challenge.MfaStatusProvider
import com.passbolt.mobile.android.feature.authentication.auth.usecase.BiometryInteractor
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetAndVerifyServerKeysAndTimeInteractor
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetServerPublicPgpKeyUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetServerPublicRsaKeyUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GopenPgpTimeUpdater
import com.passbolt.mobile.android.feature.authentication.auth.usecase.RefreshSessionUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignInUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignInVerifyInteractor
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignOutUseCase
import com.passbolt.mobile.android.feature.setup.enterpassphrase.VerifyPassphraseUseCase
import com.passbolt.mobile.android.featureflags.usecase.FeatureFlagsInteractor
import com.passbolt.mobile.android.storage.cache.passphrase.PassphraseMemoryCache
import com.passbolt.mobile.android.storage.encrypted.biometric.BiometricCipher
import com.passbolt.mobile.android.storage.usecase.accountdata.GetAccountDataUseCase
import com.passbolt.mobile.android.storage.usecase.accountdata.IsServerFingerprintCorrectUseCase
import com.passbolt.mobile.android.storage.usecase.accountdata.SaveServerFingerprintUseCase
import com.passbolt.mobile.android.storage.usecase.biometrickey.RemoveBiometricKeyUseCase
import com.passbolt.mobile.android.storage.usecase.input.UserIdInput
import com.passbolt.mobile.android.storage.usecase.passphrase.CheckIfPassphraseFileExistsUseCase
import com.passbolt.mobile.android.storage.usecase.passphrase.GetPassphraseUseCase
import com.passbolt.mobile.android.storage.usecase.passphrase.RemoveAllAccountsPassphrasesUseCase
import com.passbolt.mobile.android.storage.usecase.preferences.GetGlobalPreferencesUseCase
import com.passbolt.mobile.android.storage.usecase.privatekey.GetPrivateKeyUseCase
import com.passbolt.mobile.android.storage.usecase.session.GetSessionUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.core.module.dsl.factoryOf
import org.koin.core.scope.Scope
import org.koin.dsl.bind
import org.koin.dsl.module
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import javax.crypto.Cipher

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

internal const val ACCOUNT = "accountId"
internal const val MOCK_ACCOUNT_DATA_URL = "https://www.passbolt.com"
internal const val MOCK_ACCOUNT_DATA_FIRST_NAME = "first"
internal const val MOCK_ACCOUNT_DATA_LAST_NAME = "last"
internal const val MOCK_ACCOUNT_DATA_EMAIL = "email"
internal const val MOCK_ACCOUNT_DATA_AVATAR_URL = "avatar"
internal const val MOCK_ACCOUNT_DATA_SERVER_ID = "aaa-bbb-ccc"
internal const val MOCK_LABEL = "label"
internal const val MOCK_ROLE = "user"

internal val mockGetAccountDataUseCase = mock<GetAccountDataUseCase> {
    on { execute(UserIdInput(ACCOUNT)) }.doReturn(
        GetAccountDataUseCase.Output(
            firstName = MOCK_ACCOUNT_DATA_FIRST_NAME,
            lastName = MOCK_ACCOUNT_DATA_LAST_NAME,
            email = MOCK_ACCOUNT_DATA_EMAIL,
            avatarUrl = MOCK_ACCOUNT_DATA_AVATAR_URL,
            url = MOCK_ACCOUNT_DATA_URL,
            serverId = MOCK_ACCOUNT_DATA_SERVER_ID,
            label = MOCK_LABEL,
            role = MOCK_ROLE
        )
    )
}
internal val mockPassphraseMemoryCache = mock<PassphraseMemoryCache>()
internal val mockPrivateKeyUseCase = mock<GetPrivateKeyUseCase> {
    on { execute(any()) }.doReturn(GetPrivateKeyUseCase.Output("privateKey"))
}
internal val mockVerifyPassphraseUseCase = mock<VerifyPassphraseUseCase>()
internal val mockCheckIfPassphraseExistsUseCase = mock<CheckIfPassphraseFileExistsUseCase>()

internal val mockGetServerPublicPgpKeyUseCase = mock<GetServerPublicPgpKeyUseCase>()
internal val mockRemoveAllAccountsPassphraseUseCase = mock<RemoveAllAccountsPassphrasesUseCase>()
internal val mockGetServerPublicRsaKeyUseCase = mock<GetServerPublicRsaKeyUseCase>()
internal val mockChallengeProvider = mock<ChallengeProvider>()
internal val mockSignInUseCase = mock<SignInUseCase>()
internal val mockChallengeDecryptor = mock<ChallengeDecryptor>()
internal val mockChallengeVerifier = mock<ChallengeVerifier>()
internal val mockFingerprintInformationProvider = mock<FingerprintInformationProvider>()
internal val mockFeatureFlagsInteractor = mock<FeatureFlagsInteractor>()
internal val mockIsServerFingerprintCorrectUseCase = mock<IsServerFingerprintCorrectUseCase>()
internal val mockSaveServerFingerprintUseCase = mock<SaveServerFingerprintUseCase>()
internal val mockSignOutUseCase = mock<SignOutUseCase>()
internal val mockCipher = mock<Cipher> {
    on { iv }.doReturn(ByteArray(0))
}
internal val mockBiometricCipher = mock<BiometricCipher> {
    on { getBiometricDecryptCipher(any()) }.doReturn(mockCipher)
}
internal val mockGetPassphraseUseCase = mock<GetPassphraseUseCase>()
internal val mockRemoveBiometricKeyUseCase = mock<RemoveBiometricKeyUseCase>()
internal val authReasonMapper = AuthReasonMapper()
internal val mockMfaStatusProvider = mock<MfaStatusProvider>()
internal val mockGetSessionUseCase = mock<GetSessionUseCase>()
internal val mockRefreshSessionUseCase = mock<RefreshSessionUseCase>()
internal val mockRootDetector = mock<RootDetectorImpl>()
internal val mockProfileInteractor = mock<UserProfileInteractor>()
internal val mockInAppReviewInteractor = mock<InAppReviewInteractor>()
internal val mockGetGlobalPreferencesUseCase = mock<GetGlobalPreferencesUseCase>()
internal val mockGopenPgpTimeUpdater = mock<GopenPgpTimeUpdater>()

@ExperimentalCoroutinesApi
val testAuthModule = module {
    factory {
        GetAndVerifyServerKeysAndTimeInteractor(
            getServerPublicPgpKeyUseCase = mockGetServerPublicPgpKeyUseCase,
            getServerPublicRsaKeyUseCase = mockGetServerPublicRsaKeyUseCase,
            getAccountDataUseCase = mockGetAccountDataUseCase,
            isServerFingerprintCorrectUseCase = mockIsServerFingerprintCorrectUseCase,
            gopenPgpTimeUpdater = mockGopenPgpTimeUpdater
        )
    }
    factory {
        SignInVerifyInteractor(
            getAccountDataUseCase = mockGetAccountDataUseCase,
            challengeProvider = mockChallengeProvider,
            getSessionUseCase = mockGetSessionUseCase,
            signInUseCase = mockSignInUseCase,
            challengeDecryptor = mockChallengeDecryptor,
            challengeVerifier = mockChallengeVerifier
        )
    }
    factory {
        BiometryInteractor(
            checkIfPassphraseFileExistsUseCase = mockCheckIfPassphraseExistsUseCase,
            removeBiometricKeyUseCase = mockRemoveBiometricKeyUseCase,
            removeAllAccountsPassphrasesUseCase = mockRemoveAllAccountsPassphraseUseCase,
            fingerprintInfoProvider = mockFingerprintInformationProvider
        )
    }
    single { RuntimeAuthenticatedFlag() }
    single { SignInIdlingResource() }
    factory<AuthContract.Presenter> { (authConfig: ActivityIntents.AuthConfig) ->
        when (authConfig) {
            is ActivityIntents.AuthConfig.Startup -> signInPresenter()
            is ActivityIntents.AuthConfig.Setup -> signInPresenter()
            is ActivityIntents.AuthConfig.ManageAccount -> signInPresenter()
            is ActivityIntents.AuthConfig.SignIn -> signInPresenter()
            is ActivityIntents.AuthConfig.RefreshPassphrase -> passphrasePresenter()
            is ActivityIntents.AuthConfig.Mfa -> passphrasePresenter()
            is ActivityIntents.AuthConfig.RefreshSession -> refreshSessionPresenter()
        }
    }
    factoryOf(::TestCoroutineLaunchContext) bind CoroutineLaunchContext::class
}

private fun Scope.signInPresenter() = SignInPresenter(
    saveSessionUseCase = mock(),
    saveSelectedAccountUseCase = mock(),
    passphraseMemoryCache = mockPassphraseMemoryCache,
    signOutUseCase = mockSignOutUseCase,
    saveServerFingerprintUseCase = mockSaveServerFingerprintUseCase,
    mfaStatusProvider = mockMfaStatusProvider,
    featureFlagsInteractor = mockFeatureFlagsInteractor,
    getAndVerifyServerKeysInteractor = get(),
    signInVerifyInteractor = get(),
    userProfileInteractor = mockProfileInteractor,
    inAppReviewInteractor = mockInAppReviewInteractor,
    biometryInteractor = get(),
    getAccountDataUseCase = mockGetAccountDataUseCase,
    biometricCipher = mockBiometricCipher,
    getPassphraseUseCase = mockGetPassphraseUseCase,
    getPrivateKeyUseCase = mockPrivateKeyUseCase,
    verifyPassphraseUseCase = mockVerifyPassphraseUseCase,
    coroutineLaunchContext = get(),
    authReasonMapper = authReasonMapper,
    rootDetector = mockRootDetector,
    runtimeAuthenticatedFlag = get(),
    signInIdlingResource = get(),
    getGlobalPreferencesUseCase = mockGetGlobalPreferencesUseCase
)

private fun Scope.passphrasePresenter() = PassphrasePresenter(
    passphraseMemoryCache = mockPassphraseMemoryCache,
    getPrivateKeyUseCase = mockPrivateKeyUseCase,
    verifyPassphraseUseCase = mockVerifyPassphraseUseCase,
    getAccountDataUseCase = mockGetAccountDataUseCase,
    coroutineLaunchContext = get(),
    biometricCipher = mockBiometricCipher,
    getPassphraseUseCase = mockGetPassphraseUseCase,
    authReasonMapper = authReasonMapper,
    rootDetector = mockRootDetector,
    biometryInteractor = get(),
    runtimeAuthenticatedFlag = get(),
    getGlobalPreferencesUseCase = mockGetGlobalPreferencesUseCase
)

private fun Scope.refreshSessionPresenter() = RefreshSessionPresenter(
    refreshSessionUseCase = mockRefreshSessionUseCase,
    signInIdlingResource = get(),
    passphraseMemoryCache = mockPassphraseMemoryCache,
    saveSessionUseCase = mock(),
    saveSelectedAccountUseCase = mock(),
    getAccountDataUseCase = mockGetAccountDataUseCase,
    featureFlagsInteractor = mockFeatureFlagsInteractor,
    signOutUseCase = mockSignOutUseCase,
    saveServerFingerprintUseCase = mockSaveServerFingerprintUseCase,
    mfaStatusProvider = mockMfaStatusProvider,
    biometricCipher = mockBiometricCipher,
    getPassphraseUseCase = mockGetPassphraseUseCase,
    getPrivateKeyUseCase = mockPrivateKeyUseCase,
    verifyPassphraseUseCase = mockVerifyPassphraseUseCase,
    coroutineLaunchContext = get(),
    authReasonMapper = authReasonMapper,
    rootDetector = mockRootDetector,
    getAndVerifyServerKeysInteractor = get(),
    signInVerifyInteractor = get(),
    biometryInteractor = get(),
    userProfileInteractor = mockProfileInteractor,
    runtimeAuthenticatedFlag = get(),
    inAppReviewInteractor = mockInAppReviewInteractor,
    getGlobalPreferencesUseCase = mockGetGlobalPreferencesUseCase
)
