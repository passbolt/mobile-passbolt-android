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

package com.passbolt.mobile.android.core.accounts

import android.content.Context
import com.passbolt.mobile.android.core.accounts.usecase.BiometricCipherImpl
import com.passbolt.mobile.android.core.accounts.usecase.account.accountModule
import com.passbolt.mobile.android.core.accounts.usecase.accountdata.accountDataModule
import com.passbolt.mobile.android.core.accounts.usecase.accounts.accountsModule
import com.passbolt.mobile.android.core.accounts.usecase.biometrickey.biometricKeyIvModule
import com.passbolt.mobile.android.core.accounts.usecase.privatekey.privateKeyModule
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.selectedAccountModule
import com.passbolt.mobile.android.encryptedstorage.biometric.BiometricCipher
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val accountsCoreModule = module {
    accountModule()
    accountsModule()
    accountDataModule()
    privateKeyModule()
    biometricKeyIvModule()
    selectedAccountModule()

    singleOf(::AccountsInteractor)
    singleOf(::AccountKitParser)
    factoryOf(::BiometricCipherImpl) bind BiometricCipher::class

    single {
        androidApplication().getSharedPreferences("user-accounts", Context.MODE_PRIVATE)
    }
}
