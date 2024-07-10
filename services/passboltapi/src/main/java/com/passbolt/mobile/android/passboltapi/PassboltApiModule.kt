package com.passbolt.mobile.android.passboltapi

import com.passbolt.mobile.android.passboltapi.auth.authApiModule
import com.passbolt.mobile.android.passboltapi.expiry.passwordExpiryApiModule
import com.passbolt.mobile.android.passboltapi.favourites.favouritesApiModule
import com.passbolt.mobile.android.passboltapi.folders.foldersApiModule
import com.passbolt.mobile.android.passboltapi.groups.groupsApiModule
import com.passbolt.mobile.android.passboltapi.mfa.mfaApiModule
import com.passbolt.mobile.android.passboltapi.passwordpolicies.passwordPoliciesApiModule
import com.passbolt.mobile.android.passboltapi.rbac.rbacApiModule
import com.passbolt.mobile.android.passboltapi.registration.mobileTransferApiModule
import com.passbolt.mobile.android.passboltapi.resource.resourceApiModule
import com.passbolt.mobile.android.passboltapi.resourcetypes.resourceTypesApiModule
import com.passbolt.mobile.android.passboltapi.secrets.secretsApiModule
import com.passbolt.mobile.android.passboltapi.settings.settingsApiModule
import com.passbolt.mobile.android.passboltapi.share.shareApiModule
import com.passbolt.mobile.android.passboltapi.users.usersApiModule
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
val passboltApiModule = module {
    mobileTransferApiModule()
    authApiModule()
    mfaApiModule()
    secretsApiModule()
    resourceApiModule()
    resourceTypesApiModule()
    foldersApiModule()
    groupsApiModule()
    usersApiModule()
    settingsApiModule()
    shareApiModule()
    favouritesApiModule()
    rbacApiModule()
    passwordExpiryApiModule()
    passwordPoliciesApiModule()
}
