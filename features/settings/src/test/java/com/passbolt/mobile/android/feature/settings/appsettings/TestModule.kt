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

package com.passbolt.mobile.android.feature.settings.appsettings

import com.passbolt.mobile.android.common.FingerprintInformationProvider
import com.passbolt.mobile.android.feature.authentication.auth.usecase.BiometryInteractor
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsContract
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsPresenter
import com.passbolt.mobile.android.storage.cache.passphrase.PassphraseMemoryCache
import com.passbolt.mobile.android.storage.encrypted.biometric.BiometricCipher
import com.passbolt.mobile.android.storage.usecase.biometrickey.SaveBiometricKeyIvUseCase
import com.passbolt.mobile.android.storage.usecase.passphrase.CheckIfPassphraseFileExistsUseCase
import com.passbolt.mobile.android.storage.usecase.passphrase.RemovePassphraseUseCase
import com.passbolt.mobile.android.storage.usecase.passphrase.SavePassphraseUseCase
import com.passbolt.mobile.android.storage.usecase.preferences.GetHomeDisplayViewPrefsUseCase
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
import org.koin.dsl.module
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import javax.crypto.Cipher

internal val checkIfPassphraseFileExistsUseCase = mock<CheckIfPassphraseFileExistsUseCase>()
internal val removePassphraseUseCase = mock<RemovePassphraseUseCase>()
internal val getSelectedAccountUseCase = mock<GetSelectedAccountUseCase>()
internal val savePassphraseUseCase = mock<SavePassphraseUseCase>()
internal val passphraseMemoryCache = mock<PassphraseMemoryCache>()
internal val mockCipher = mock<Cipher> {
    on { iv }.doReturn(ByteArray(0))
}
internal val biometricCipher = mock<BiometricCipher> {
    on { getBiometricEncryptCipher() }.doReturn(mockCipher)
}
internal val saveBiometricKayIvUseCase = mock<SaveBiometricKeyIvUseCase>()
internal val fingerprintInformationProvider = mock<FingerprintInformationProvider>()
internal val mockBiometryInteractor = mock<BiometryInteractor>()
internal val mockGetHomeDisplayPrefsUseCase = mock<GetHomeDisplayViewPrefsUseCase>()

val testAppSettingsModule = module {
    factory<AppSettingsContract.Presenter> {
        AppSettingsPresenter(
            getHomeDisplayViewPrefsUseCase = mockGetHomeDisplayPrefsUseCase,
            fingerprintInformationProvider = fingerprintInformationProvider,
            passphraseMemoryCache = passphraseMemoryCache,
            biometricCipher = biometricCipher,
            biometryInteractor = mockBiometryInteractor,
            savePassphraseUseCase = savePassphraseUseCase,
            saveBiometricKeyIvUseCase = saveBiometricKayIvUseCase,
            checkIfPassphraseExistsUseCase = checkIfPassphraseFileExistsUseCase,
            removePassphraseUseCase = removePassphraseUseCase,
            getSelectedAccountUseCase = getSelectedAccountUseCase
        )
    }
}
