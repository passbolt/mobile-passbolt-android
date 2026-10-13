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

package com.passbolt.mobile.android.domain.metadata.usecase

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.domain.metadata.usecase.db.GetLocalMetadataKeysUseCase
import com.passbolt.mobile.android.ui.MetadataTypeModel
import com.passbolt.mobile.android.ui.MetadataTypesSettingsModel
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class CanShareResourceUseCaseTest {
    private val getMetadataTypesSettingsUseCase = mock<GetMetadataTypesSettingsUseCase>()
    private val getLocalMetadataKeysUseCase = mock<GetLocalMetadataKeysUseCase>()

    private val useCase =
        CanShareResourceUseCase(
            getMetadataTypesSettingsUseCase = getMetadataTypesSettingsUseCase,
            getLocalMetadataKeysUseCase = getLocalMetadataKeysUseCase,
        )

    @Test
    fun `no local encryption key denies sharing`() =
        runTest {
            whenever(getMetadataTypesSettingsUseCase.execute(Unit)).thenReturn(
                GetMetadataTypesSettingsUseCase.Output(
                    MetadataTypesSettingsModel(
                        defaultMetadataType = MetadataTypeModel.V5,
                        defaultFolderType = MetadataTypeModel.V5,
                        defaultTagType = MetadataTypeModel.V5,
                        allowCreationOfV5Resources = true,
                        allowCreationOfV5Folders = true,
                        allowCreationOfV5Tags = true,
                        allowCreationOfV4Resources = true,
                        allowCreationOfV4Folders = true,
                        allowCreationOfV4Tags = true,
                        allowV4V5Upgrade = false,
                        allowV5V4Downgrade = false,
                    ),
                ),
            )
            whenever(getLocalMetadataKeysUseCase.execute(any())).thenReturn(emptyList())

            val result = useCase.execute(Unit)

            assertThat(result.canShareResource).isFalse()
        }
}
