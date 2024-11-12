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

package com.passbolt.mobile.android.supportedresourceTypes

private const val PASSWORD_STRING_SLUG = "password-string"
private const val PASSWORD_AND_DESCRIPTION_SLUG = "password-and-description"
private const val TOTP_SLUG = "totp"
private const val PASSWORD_DESCRIPTION_TOTP_SLUG = "password-description-totp"
private const val V5_TOTP_STANDALONE_SLUG = "v5-totp-standalone"
private const val V5_DEFAULT_SLUG = "v5-default"
private const val V5_DEFAULT_WITH_TOTP = "v5-default-with-totp"
private const val V5_PASSWORD_STRING_SLUG = "v5-password-string"

sealed class ContentType(val slug: String) {
    data object PasswordString : ContentType(PASSWORD_STRING_SLUG)
    data object PasswordAndDescription : ContentType(PASSWORD_AND_DESCRIPTION_SLUG)
    data object Totp : ContentType(TOTP_SLUG)
    data object PasswordDescriptionTotp : ContentType(PASSWORD_DESCRIPTION_TOTP_SLUG)
    data object V5TotpStandalone : ContentType(V5_TOTP_STANDALONE_SLUG)
    data object V5Default : ContentType(V5_DEFAULT_SLUG)
    data object V5DefaultWithTotp : ContentType(V5_DEFAULT_WITH_TOTP)
    data object V5PasswordString : ContentType(V5_PASSWORD_STRING_SLUG)

    fun isSimplePassword() = this == PasswordString || this == V5PasswordString

    fun isV5() = this.slug in SupportedContentTypes.v5Slugs

    companion object {
        fun fromSlug(slug: String): ContentType {
            return when (slug) {
                PASSWORD_STRING_SLUG -> PasswordString
                PASSWORD_AND_DESCRIPTION_SLUG -> PasswordAndDescription
                TOTP_SLUG -> Totp
                PASSWORD_DESCRIPTION_TOTP_SLUG -> PasswordDescriptionTotp
                V5_TOTP_STANDALONE_SLUG -> V5TotpStandalone
                V5_DEFAULT_SLUG -> V5Default
                V5_DEFAULT_WITH_TOTP -> V5DefaultWithTotp
                V5_PASSWORD_STRING_SLUG -> V5PasswordString
                else -> throw IllegalArgumentException("Unsupported content type slug: $slug")
            }
        }
    }
}

object SupportedContentTypes {

    val homeSlugs = setOf(
        ContentType.PasswordString,
        ContentType.PasswordAndDescription,
        ContentType.PasswordDescriptionTotp,
        ContentType.V5Default,
        ContentType.V5PasswordString
    ).map { it.slug }.toSet()

    val totpSlugs = setOf(
        ContentType.Totp,
        ContentType.PasswordDescriptionTotp,
        ContentType.V5TotpStandalone,
        ContentType.V5DefaultWithTotp
    ).map { it.slug }.toSet()

    val allSlugs = homeSlugs + totpSlugs

    val v4Slugs = setOf(
        ContentType.PasswordString,
        ContentType.PasswordAndDescription,
        ContentType.PasswordDescriptionTotp,
        ContentType.Totp
    ).map { it.slug }.toSet()

    val v5Slugs = setOf(
        ContentType.V5Default,
        ContentType.V5PasswordString,
        ContentType.V5DefaultWithTotp,
        ContentType.V5TotpStandalone
    ).map { it.slug }.toSet()
}
