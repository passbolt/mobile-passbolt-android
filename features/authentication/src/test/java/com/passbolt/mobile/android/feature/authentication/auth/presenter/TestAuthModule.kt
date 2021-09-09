package com.passbolt.mobile.android.feature.authentication.auth.presenter

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.passbolt.mobile.android.common.FingerprintInformationProvider
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.navigation.AuthenticationType
import com.passbolt.mobile.android.feature.authentication.auth.AuthContract
import com.passbolt.mobile.android.feature.authentication.auth.challenge.ChallengeDecryptor
import com.passbolt.mobile.android.feature.authentication.auth.challenge.ChallengeProvider
import com.passbolt.mobile.android.feature.authentication.auth.challenge.ChallengeVerifier
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetServerPublicPgpKeyUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetServerPublicRsaKeyUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SiginInUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignOutUseCase
import com.passbolt.mobile.android.feature.setup.enterpassphrase.VerifyPassphraseUseCase
import com.passbolt.mobile.android.featureflags.usecase.FeatureFlagsInteractor
import com.passbolt.mobile.android.storage.base.TestCoroutineLaunchContext
import com.passbolt.mobile.android.storage.cache.passphrase.PassphraseMemoryCache
import com.passbolt.mobile.android.storage.encrypted.biometric.BiometricCipher
import com.passbolt.mobile.android.storage.usecase.accountdata.GetAccountDataUseCase
import com.passbolt.mobile.android.storage.usecase.biometrickey.RemoveBiometricKeyUseCase
import com.passbolt.mobile.android.storage.usecase.input.UserIdInput
import com.passbolt.mobile.android.storage.usecase.passphrase.CheckIfPassphraseFileExistsUseCase
import com.passbolt.mobile.android.storage.usecase.passphrase.GetPassphraseUseCase
import com.passbolt.mobile.android.storage.usecase.passphrase.RemoveSelectedAccountPassphraseUseCase
import com.passbolt.mobile.android.storage.usecase.privatekey.GetPrivateKeyUseCase
import org.koin.core.qualifier.named
import org.koin.dsl.module
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

internal val mockGetAccountDataUseCase = mock<GetAccountDataUseCase> {
    on { execute(UserIdInput(ACCOUNT)) }.doReturn(
        GetAccountDataUseCase.Output(
            MOCK_ACCOUNT_DATA_FIRST_NAME,
            MOCK_ACCOUNT_DATA_LAST_NAME,
            MOCK_ACCOUNT_DATA_EMAIL,
            MOCK_ACCOUNT_DATA_AVATAR_URL,
            MOCK_ACCOUNT_DATA_URL,
            MOCK_ACCOUNT_DATA_SERVER_ID
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
internal val mockRemoveSelectedAccountPassphraseUseCase = mock<RemoveSelectedAccountPassphraseUseCase>()
internal val mockGetServerPublicRsaKeyUseCase = mock<GetServerPublicRsaKeyUseCase>()
internal val mockChallengeProvider = mock<ChallengeProvider>()
internal val mockSignInUseCase = mock<SiginInUseCase>()
internal val mockChallengeDecryptor = mock<ChallengeDecryptor>()
internal val mockChallengeVerifier = mock<ChallengeVerifier>()
internal val mockFingerprintInformationProvider = mock<FingerprintInformationProvider>()
internal val mockFeatureFlagsInteractor = mock<FeatureFlagsInteractor>()
internal val mockSignOutUseCase = mock<SignOutUseCase>()
internal val mockCipher = mock<Cipher> {
    on { iv }.doReturn(ByteArray(0))
}
internal val mockBiometricCipher = mock<BiometricCipher> {
    on { getBiometricDecryptCipher(any()) }.doReturn(mockCipher)
}
internal val mockGetPassphraseUseCase = mock<GetPassphraseUseCase>()
internal val mockRemoveBiometricKeyUseCase = mock<RemoveBiometricKeyUseCase>()

val testAuthModule = module {
    factory<AuthContract.Presenter>(named(AuthenticationType.Passphrase.javaClass.simpleName)) {
        PassphrasePresenter(
            passphraseMemoryCache = mockPassphraseMemoryCache,
            getPrivateKeyUseCase = mockPrivateKeyUseCase,
            verifyPassphraseUseCase = mockVerifyPassphraseUseCase,
            fingerprintInfoProvider = mockFingerprintInformationProvider,
            removeSelectedAccountPassphraseUseCase = mockRemoveSelectedAccountPassphraseUseCase,
            checkIfPassphraseFileExistsUseCase = mockCheckIfPassphraseExistsUseCase,
            getAccountDataUseCase = mockGetAccountDataUseCase,
            coroutineLaunchContext = get(),
            biometricCipher = mockBiometricCipher,
            getPassphraseUseCase = mockGetPassphraseUseCase,
            removeBiometricKeyUseCase = mockRemoveBiometricKeyUseCase
        )
    }
    factory<AuthContract.Presenter>(named(AuthenticationType.SignIn.javaClass.simpleName)) {
        SignInPresenter(
            getServerPublicPgpKeyUseCase = mockGetServerPublicPgpKeyUseCase,
            getServerPublicRsaKeyUseCase = mockGetServerPublicRsaKeyUseCase,
            signInUseCase = mockSignInUseCase,
            coroutineLaunchContext = get(),
            challengeProvider = mockChallengeProvider,
            challengeDecryptor = mockChallengeDecryptor,
            challengeVerifier = mockChallengeVerifier,
            getAccountDataUseCase = mockGetAccountDataUseCase,
            saveSessionUseCase = mock(),
            saveSelectedAccountUseCase = mock(),
            checkIfPassphraseFileExistsUseCase = mockCheckIfPassphraseExistsUseCase,
            removeSelectedAccountPassphraseUseCase = mockRemoveSelectedAccountPassphraseUseCase,
            fingerprintInfoProvider = mockFingerprintInformationProvider,
            passphraseMemoryCache = mockPassphraseMemoryCache,
            featureFlagsInteractor = mockFeatureFlagsInteractor,
            signOutUseCase = mockSignOutUseCase,
            getPrivateKeyUseCase = mockPrivateKeyUseCase,
            verifyPassphraseUseCase = mockVerifyPassphraseUseCase,
            biometricCipher = mockBiometricCipher,
            getPassphraseUseCase = mockGetPassphraseUseCase,
            removeBiometricKeyUseCase = mockRemoveBiometricKeyUseCase
        )
    }
    factory<CoroutineLaunchContext> { TestCoroutineLaunchContext() }
}
