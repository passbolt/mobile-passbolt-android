package com.passbolt.mobile.android.feature.settings

import com.nhaarman.mockitokotlin2.mock
import com.passbolt.mobile.android.common.autofill.AutofillInformationProvider
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignOutUseCase
import com.passbolt.mobile.android.feature.settings.screen.SettingsContract
import com.passbolt.mobile.android.feature.settings.screen.SettingsPresenter
import com.passbolt.mobile.android.storage.base.TestCoroutineLaunchContext
import com.passbolt.mobile.android.storage.usecase.passphrase.CheckIfPassphraseFileExistsUseCase
import com.passbolt.mobile.android.storage.usecase.passphrase.RemovePassphraseUseCase
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.dsl.module

internal val checkIfPassphraseFileExistsUseCase = mock<CheckIfPassphraseFileExistsUseCase>()
internal val autofillInformationProvider = mock<AutofillInformationProvider>()
internal val removePassphraseUseCase = mock<RemovePassphraseUseCase>()
internal val getSelectedAccountUseCase = mock<GetSelectedAccountUseCase>()
internal val signOutUseCase = mock<SignOutUseCase>()

@ExperimentalCoroutinesApi
val testModule = module {
    factory { checkIfPassphraseFileExistsUseCase }
    factory { autofillInformationProvider }
    factory { removePassphraseUseCase }
    factory { getSelectedAccountUseCase }
    factory { signOutUseCase }
    factory<SettingsContract.Presenter> {
        SettingsPresenter(
            checkIfPassphraseExistsUseCase = get(),
            autofillInfoProvider = get(),
            removePassphraseUseCase = get(),
            getSelectedAccountUseCase = get(),
            signOutUseCase = get(),
            coroutineLaunchContext = get()
        )
    }
    factory<CoroutineLaunchContext> { TestCoroutineLaunchContext() }
}
