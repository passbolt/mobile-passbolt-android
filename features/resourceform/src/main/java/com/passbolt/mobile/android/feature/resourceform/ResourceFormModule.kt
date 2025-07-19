package com.passbolt.mobile.android.feature.resourceform

import com.passbolt.mobile.android.core.resources.usecase.GetDefaultCreateContentTypeUseCase
import com.passbolt.mobile.android.core.resources.usecase.GetEditContentTypeUseCase
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.note.noteFormModule
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.password.passwordFormModule
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.advanced.totpAdvancedSettingsFormModule
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.totpFormModule
import com.passbolt.mobile.android.feature.resourceform.main.resourceFormModule
import com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris.additionalUrisFormModule
import com.passbolt.mobile.android.feature.resourceform.metadata.appearance.appearanceFormModule
import com.passbolt.mobile.android.feature.resourceform.metadata.description.descriptionFormModule
import org.koin.core.module.dsl.factoryOf
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

val resourceFormModule =
    module {
        resourceFormModule()
        descriptionFormModule()
        totpFormModule()
        totpAdvancedSettingsFormModule()
        noteFormModule()
        passwordFormModule()
        additionalUrisFormModule()
        appearanceFormModule()

        factoryOf(::GetDefaultCreateContentTypeUseCase)
        factoryOf(::GetEditContentTypeUseCase)
    }
