package com.passbolt.mobile.android.storage.di

import android.content.Context
import android.content.SharedPreferences
import com.passbolt.mobile.android.storage.factory.EncryptedFileFactory
import com.passbolt.mobile.android.storage.factory.EncryptedSharedPreferencesFactory
import com.passbolt.mobile.android.storage.factory.KeySpecProvider
import com.passbolt.mobile.android.storage.usecase.AddAccountUseCase
import com.passbolt.mobile.android.storage.usecase.GetAccountDataUseCase
import com.passbolt.mobile.android.storage.usecase.GetAccountsUseCase
import com.passbolt.mobile.android.storage.usecase.GetPassphraseUseCase
import com.passbolt.mobile.android.storage.usecase.GetPrivateKeyUseCase
import com.passbolt.mobile.android.storage.usecase.GetSelectedAccountUseCase
import com.passbolt.mobile.android.storage.usecase.GetSessionUseCase
import com.passbolt.mobile.android.storage.usecase.SaveAccountDataUseCase
import com.passbolt.mobile.android.storage.usecase.SavePassphraseUseCase
import com.passbolt.mobile.android.storage.usecase.SavePrivateKeyUseCase
import com.passbolt.mobile.android.storage.usecase.SaveSelectedAccountUseCase
import com.passbolt.mobile.android.storage.usecase.SaveSessionUseCase
import com.passbolt.mobile.android.storage.usecase.SaveUserAvatarUseCase
import com.passbolt.mobile.android.storage.usecase.UpdateAccountDataUseCase
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

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

val storageModule = module {
    single { provideSharedPreferences(androidApplication()) }
    single {
        EncryptedFileFactory(
            context = androidApplication(),
            keySpecProvider = get()
        )
    }
    single {
        EncryptedSharedPreferencesFactory(
            context = androidApplication(),
            keySpecProvider = get()
        )
    }
    single { KeySpecProvider() }
    single {
        AddAccountUseCase(
            sharedPreferences = get()
        )
    }
    single {
        GetAccountDataUseCase(
            encryptedSharedPreferencesFactory = get()
        )
    }
    single {
        GetAccountsUseCase(
            sharedPreferences = get()
        )
    }
    single {
        GetPassphraseUseCase(
            encryptedFileFactory = get()
        )
    }
    single {
        GetPrivateKeyUseCase(
            encryptedFileFactory = get()
        )
    }
    single {
        GetSelectedAccountUseCase(
            encryptedSharedPreferencesFactory = get()
        )
    }
    single {
        GetSessionUseCase(
            encryptedSharedPreferencesFactory = get()
        )
    }
    single {
        SaveAccountDataUseCase(
            encryptedSharedPreferencesFactory = get()
        )
    }
    single {
        UpdateAccountDataUseCase(
            getSelectedAccountUseCase = get(),
            encryptedSharedPreferencesFactory = get()
        )
    }
    single {
        SavePassphraseUseCase(
            encryptedFileFactory = get()
        )
    }
    single {
        SavePrivateKeyUseCase(
            encryptedFileFactory = get()
        )
    }
    single {
        SaveUserAvatarUseCase(
            getSelectedAccountUseCase = get(),
            encryptedFileFactory = get()
        )
    }
    single {
        SaveSelectedAccountUseCase(
            encryptedSharedPreferencesFactory = get()
        )
    }
    single {
        SaveSessionUseCase(
            encryptedSharedPreferencesFactory = get()
        )
    }
}

private fun provideSharedPreferences(appContext: Context): SharedPreferences {
    return appContext.getSharedPreferences("user-accounts", Context.MODE_PRIVATE)
}
