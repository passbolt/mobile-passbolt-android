package com.passbolt.mobile.android.feature.setup.scanqr.mapper

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

@Suppress("ktlint:standard:max-line-length", "MaxLineLength")
internal val PASSBOLT_FIRST_MISSING_VALUE_CONFIGURATION_PAGE_SCAN =
    (
        "100{" +
            "\"transfer_id\":\"6a63c0f1-1c87-4402-84eb-b3141e1e6397\"," +
            "\"user_id\":\"f848277c-5398-58f8-a82a-72397af2d450\"," +
            "\"total_pages\":7," +
            "\"hash\":\"3d84155d3ea079c17221587bbd1fce285b8b636014025e484da01867cf28c0bc22079cac9a268e2ca76d075189065e5426044244b6d0e1a440adda4d89e148fb\"," +
            "\"authentication_token\":\"af32cb1f-c1ae-4753-9982-7cc0d2178355\"" +
            "}"
    ).toByteArray()

@Suppress("ktlint:standard:max-line-length", "MaxLineLength")
internal val PASSBOLT_FIRST_PAGE_SCAN =
    (
        "100{" +
            "\"transfer_id\":\"6a63c0f1-1c87-4402-84eb-b3141e1e6397\"," +
            "\"user_id\":\"f848277c-5398-58f8-a82a-72397af2d450\"," +
            "\"domain\":\"https://localhost:8443\"," +
            "\"total_pages\":7," +
            "\"hash\":\"3d84155d3ea079c17221587bbd1fce285b8b636014025e484da01867cf28c0bc22079cac9a268e2ca76d075189065e5426044244b6d0e1a440adda4d89e148fb\"," +
            "\"authentication_token\":\"af32cb1f-c1ae-4753-9982-7cc0d2178355\"" +
            "}"
    ).toByteArray()

internal val PASSBOLT_SUBSEQUENT_PAGE_SCAN =
    (
        "101{" +
            "\"user_id\":\"f848277c-5398-58f8-a82a-72397af2d450\"," +
            "\"fingerprint\":\"03f60e958f4cb29723acdf761353b5b15d9b054f\"," +
            "\"armored_key\":\"-----BEGIN PGP PRIVATE KEY BLOCK-----"
    ).toByteArray()

internal val PASSBOLT_FIRST_PAGE_ACCOUNT_KIT_SCAN =
    (
        "200{" +
            "\"account_kit_url\":\"mock_url\"" +
            "}"
    ).toByteArray()
