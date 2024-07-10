package com.passbolt.mobile.android.storage

import android.content.Context
import com.passbolt.mobile.android.storage.cache.cacheModule
import com.passbolt.mobile.android.storage.encrypted.biometric.cryptoModule
import com.passbolt.mobile.android.storage.encrypted.encryptedStorageModule
import com.passbolt.mobile.android.storage.usecase.account.accountModule
import com.passbolt.mobile.android.storage.usecase.accountdata.accountDataModule
import com.passbolt.mobile.android.storage.usecase.accounts.accountsModule
import com.passbolt.mobile.android.storage.usecase.biometrickey.biometricKeyIvModule
import com.passbolt.mobile.android.storage.usecase.database.databaseModule
import com.passbolt.mobile.android.storage.usecase.featureflags.featureFlagsModule
import com.passbolt.mobile.android.storage.usecase.passphrase.passphraseModule
import com.passbolt.mobile.android.storage.usecase.policies.policiesModule
import com.passbolt.mobile.android.storage.usecase.preferences.preferencesModule
import com.passbolt.mobile.android.storage.usecase.privatekey.privateKeyModule
import com.passbolt.mobile.android.storage.usecase.selectedaccount.selectedAccountModule
import com.passbolt.mobile.android.storage.usecase.session.sessionModule
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
    cacheModule()
    encryptedStorageModule()
    accountModule()
    accountDataModule()
    accountsModule()
    passphraseModule()
    privateKeyModule()
    selectedAccountModule()
    sessionModule()
    cryptoModule()
    biometricKeyIvModule()
    preferencesModule()
    databaseModule()
    featureFlagsModule()
    policiesModule()

    single {
        androidApplication().getSharedPreferences("user-accounts", Context.MODE_PRIVATE)
    }
}
