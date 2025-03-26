package com.passbolt.mobile.android.feature.setup.fingerprint

import com.passbolt.mobile.android.common.FingerprintInformationProvider
import com.passbolt.mobile.android.core.accounts.usecase.biometrickey.SaveBiometricKeyIvUseCase
import com.passbolt.mobile.android.core.authenticationcore.passphrase.SavePassphraseUseCase
import com.passbolt.mobile.android.core.passphrasememorycache.PassphraseMemoryCache
import com.passbolt.mobile.android.encryptedstorage.biometric.BiometricCipher
import com.passbolt.mobile.android.feature.authentication.auth.usecase.BiometryInteractor
import com.passbolt.mobile.android.feature.autofill.informationprovider.AutofillInformationProvider
import com.passbolt.mobile.android.feature.main.mainscreen.encouragements.EncouragementsInteractor
import org.koin.dsl.module
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

internal val fingerprintInformationProvider = mock<FingerprintInformationProvider>()
internal val autofillInformationProvider = mock<AutofillInformationProvider>()
internal val passphraseMemoryCache = mock<PassphraseMemoryCache>()
internal val savePassphraseUseCase = mock<SavePassphraseUseCase>()
internal val mockCipher = mock<Cipher> {
    on { iv }.doReturn(ByteArray(0))
}
internal val biometricCipher = mock<BiometricCipher> {
    on { getBiometricEncryptCipher() }.doReturn(mockCipher)
}
internal val saveBiometricKayIvUseCase = mock<SaveBiometricKeyIvUseCase>()
internal val mockBiometryInteractor = mock<BiometryInteractor>()
internal val mockEncouragementsInteractor = mock<EncouragementsInteractor>()


val fingerprintModule = module {
    factory<FingerprintContract.Presenter> {
        FingerprintPresenter(
            fingerprintInformationProvider = get(),
            autofillInformationProvider = get(),
            passphraseMemoryCache = get(),
            savePassphraseUseCase = savePassphraseUseCase,
            biometricCipher = biometricCipher,
            saveBiometricKeyIvUseCase = saveBiometricKayIvUseCase,
            biometryInteractor = mockBiometryInteractor,
            encouragementsInteractor = mockEncouragementsInteractor
        )
    }
    factory { fingerprintInformationProvider }
    factory { passphraseMemoryCache }
    factory { autofillInformationProvider }
}
