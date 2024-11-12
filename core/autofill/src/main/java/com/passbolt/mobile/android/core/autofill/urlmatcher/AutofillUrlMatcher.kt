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

package com.passbolt.mobile.android.core.autofill.urlmatcher

import com.passbolt.mobile.android.ui.ResourceModel
import timber.log.Timber
import java.net.URL

class AutofillUrlMatcher {

    fun isMatching(autofillUrl: String?, resource: ResourceModel): Boolean {
        val resourceUris = resource.uris.orEmpty() + resource.uri.orEmpty()
        return resourceUris.any { isMatching(autofillUrl, it) }
    }

    fun isMatching(autofillUrl: String?, resourceUrl: String?): Boolean {
        // not matching if any is null or empty
        if (autofillUrl.isNullOrEmpty() || resourceUrl.isNullOrEmpty()) return false

        // not matching if the schemes are not allowed
        if (allowedSchemes.none { autofillUrl.startsWith(it) } || allowedSchemes.none { resourceUrl.startsWith(it) }) {
            return false
        }

        return try {
            val autofillURL = URL(autofillUrl)
            val resourceURL = URL(resourceUrl)
            val schemesMatch = autofillURL.protocol == resourceURL.protocol
            val hostsMatch = autofillURL.host == resourceURL.host || autofillURL.host == "www.${resourceURL.host}"
            schemesMatch && hostsMatch
        } catch (exception: Exception) {
            Timber.e(exception, "Error during URL parsing")
            false
        }
    }

    companion object {
        private val allowedSchemes = listOf("http", "https")
    }
}
