package com.passbolt.mobile.android.mappers

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.dto.request.UpdateTransferRequestDto
import com.passbolt.mobile.android.dto.request.StatusRequest
import com.passbolt.mobile.android.ui.Status
import org.junit.Before
import org.junit.Test

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
class TransferMapperTest {
    private lateinit var mapper: TransferMapper

    @Before
    fun setUp() {
        mapper = TransferMapper()
    }

    @Test
    fun `Mapping in progress status should returns proper dto`() =
        verifyStatus(Status.IN_PROGRESS, StatusRequest.IN_PROGRESS)

    @Test
    fun `Mapping error status should returns proper dto`() =
        verifyStatus(Status.ERROR, StatusRequest.ERROR)

    @Test
    fun `Mapping complete status should returns proper dto`() =
        verifyStatus(Status.COMPLETE, StatusRequest.COMPLETE)

    @Test
    fun `Mapping cancel status should returns proper dto`() =
        verifyStatus(Status.CANCEL, StatusRequest.CANCEL)

    private fun verifyStatus(inputStatus: Status, expectedStatusRequest: StatusRequest) {
        val result = mapper.mapRequestToDto(0, inputStatus)
        val expected = UpdateTransferRequestDto(0, expectedStatusRequest)
        assertThat(result).isEqualTo(expected)
    }

}

