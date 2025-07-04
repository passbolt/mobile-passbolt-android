package com.passbolt.mobile.android.metadata.usecase

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
object MetadataTypesStorageConstants {
    internal const val DEFAULT_METADATA_TYPE = "DEFAULT_METADATA_TYPE"
    internal const val DEFAULT_FOLDER_TYPE = "DEFAULT_FOLDER_TYPE"
    internal const val DEFAULT_TAG_TYPE = "DEFAULT_TAG_TYPE"
    internal const val ALLOW_CREATION_OF_V5_RESOURCES = "ALLOW_CREATION_OF_V5_RESOURCES"
    internal const val ALLOW_CREATION_OF_V5_FOLDERS = "ALLOW_CREATION_OF_V5_FOLDERS"
    internal const val ALLOW_CREATION_OF_V5_TAGS = "ALLOW_CREATION_OF_V5_TAGS"
    internal const val ALLOW_CREATION_OF_V4_RESOURCES = "ALLOW_CREATION_OF_V4_RESOURCES"
    internal const val ALLOW_CREATION_OF_V4_FOLDERS = "ALLOW_CREATION_OF_V4_FOLDERS"
    internal const val ALLOW_CREATION_OF_V4_TAGS = "ALLOW_CREATION_OF_V4_TAGS"
    internal const val ALLOW_V4_V5_UPGRADE = "ALLOW_V4_V5_UPGRADE"
    internal const val ALLOW_V5_V4_DOWNGRADE = "ALLOW_V5_V4_DOWNGRADE"
    internal const val ALLOW_USAGE_OF_PERSONAL_KEYS = "ALLOW_USAGE_OF_PERSONAL_KEYS"
    internal const val ZERO_KNOWLEDGE_KEY_SHARE = "ZERO_KNOWLEDGE_KEY_SHARE"
    internal const val TRUSTED_MD_KEY_USER_ID = "TRUSTED_MD_KEY_USER_ID"
    internal const val TRUSTED_MD_KEY_KEY_DATA = "TRUSTED_MD_KEY_KEY_DATA"
    internal const val TRUSTED_MD_KEY_PASSPHRASE = "TRUSTED_MD_KEY_PASSPHRASE"
    internal const val TRUSTED_MD_KEY_CREATED = "TRUSTED_MD_KEY_CREATED"
    internal const val TRUSTED_MD_KEY_CREATED_BY = "TRUSTED_MD_KEY_CREATED_BY"
    internal const val TRUSTED_MD_KEY_MODIFIED = "TRUSTED_MD_KEY_MODIFIED"
    internal const val TRUSTED_MD_KEY_MODIFIED_BY = "TRUSTED_MD_KEY_MODIFIED_BY"
    internal const val TRUSTED_MD_KEY_KEY_PGP_MESSAGE = "TRUSTED_MD_KEY_KEY_PGP_MESSAGE"
    internal const val TRUSTED_MD_KEY_SIGNING_KEY_FINGERPRINT = "TRUSTED_MD_KEY_SIGNING_KEY_FINGERPRINT"
    internal const val TRUSTED_MD_KEY_SIGNED_USERNAME = "TRUSTED_MD_KEY_SIGNED_USERNAME"
    internal const val TRUSTED_MD_KEY_SIGNED_NAME = "TRUSTED_MD_KEY_SIGNED_NAME"
    internal const val TRUSTED_MD_SIGNATURE_CREATION_TIMESTAMP = "TRUSTED_MD_SIGNATURE_CREATION_TIMESTAMP"
    internal const val TRUSTED_MD_KEY_ID = "TRUSTED_MD_KEY_KEY_ID"
}
