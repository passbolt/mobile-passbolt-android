package com.passbolt.mobile.android.feature.settings

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.passbolt.mobile.android.common.FingerprintInformationProvider
import com.passbolt.mobile.android.feature.autofill.AutofillInformationProvider
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignOutUseCase
import com.passbolt.mobile.android.feature.settings.screen.SettingsContract
import com.passbolt.mobile.android.feature.settings.screen.SettingsPresenter
import com.passbolt.mobile.android.featureflags.usecase.GetFeatureFlagsUseCase
import com.passbolt.mobile.android.storage.base.TestCoroutineLaunchContext
import com.passbolt.mobile.android.storage.cache.passphrase.PassphraseMemoryCache
import com.passbolt.mobile.android.storage.encrypted.biometric.BiometricCipher
import com.passbolt.mobile.android.storage.usecase.biometrickey.RemoveBiometricKeyUseCase
import com.passbolt.mobile.android.storage.usecase.biometrickey.SaveBiometricKeyIvUseCase
import com.passbolt.mobile.android.storage.usecase.passphrase.CheckIfPassphraseFileExistsUseCase
import com.passbolt.mobile.android.storage.usecase.passphrase.RemovePassphraseUseCase
import com.passbolt.mobile.android.storage.usecase.passphrase.SavePassphraseUseCase
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.dsl.module
import javax.crypto.Cipher

internal val checkIfPassphraseFileExistsUseCase = mock<CheckIfPassphraseFileExistsUseCase>()
internal val autofillInformationProvider = mock<AutofillInformationProvider>()
internal val removePassphraseUseCase = mock<RemovePassphraseUseCase>()
internal val getSelectedAccountUseCase = mock<GetSelectedAccountUseCase>()
internal val signOutUseCase = mock<SignOutUseCase>()
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
internal val removeBiometricKeyUseCase = mock<RemoveBiometricKeyUseCase>()
internal val getFeatureFlagsUseCase = mock<GetFeatureFlagsUseCase>()

@ExperimentalCoroutinesApi
val testModule = module {
    factory { checkIfPassphraseFileExistsUseCase }
    factory { autofillInformationProvider }
    factory { removePassphraseUseCase }
    factory { getSelectedAccountUseCase }
    factory { signOutUseCase }
    factory { savePassphraseUseCase }
    factory { passphraseMemoryCache }
    factory { biometricCipher }
    factory { saveBiometricKayIvUseCase }
    factory { fingerprintInformationProvider }
    factory { removeBiometricKeyUseCase }
    factory { getFeatureFlagsUseCase }
    factory<SettingsContract.Presenter> {
        SettingsPresenter(
            checkIfPassphraseExistsUseCase = get(),
            autofillInfoProvider = get(),
            removePassphraseUseCase = get(),
            getSelectedAccountUseCase = get(),
            savePassphraseUseCase = get(),
            passphraseMemoryCache = get(),
            biometricCipher = get(),
            saveBiometricKeyIvUseCase = get(),
            fingerprintInformationProvider = get(),
            removeBiometricKeyUseCase = get(),
            getFeatureFlagsUseCase = get(),
            coroutineLaunchContext = get(),
            signOutUseCase = get()
        )
    }
    factory<CoroutineLaunchContext> { TestCoroutineLaunchContext() }
}
