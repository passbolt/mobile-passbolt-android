package com.passbolt.mobile.android.feature.autofill

import android.app.assist.AssistStructure
import android.view.View
import com.passbolt.mobile.android.feature.autofill.service.ParsedStructure

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
class StructureParser {
    fun parse(assistStructure: AssistStructure): Set<ParsedStructure> {
        val result = mutableSetOf<ParsedStructure>()
        for (i in 0 until assistStructure.windowNodeCount) {
            val viewNode = assistStructure.getWindowNodeAt(i).rootViewNode
            traverseRoot(viewNode, result)
        }
        return result
    }

    fun extractHint(autofillHintType: String, parsedSet: Set<ParsedStructure>) =
        parsedSet.firstOrNull { parsedStructure ->
            parsedStructure.hints.any { hint ->
                hint.contains(autofillHintType, ignoreCase = true)
            }
        }

    private fun traverseRoot(
        viewNode: AssistStructure.ViewNode,
        result: MutableSet<ParsedStructure>
    ) {
        parseNode(viewNode)?.let {
            result.add(it)
        }
        val childrenSize = viewNode.childCount
        for (i in 0 until childrenSize) {
            traverseRoot(viewNode.getChildAt(i), result)
        }
    }

    private fun parseNode(viewNode: AssistStructure.ViewNode): ParsedStructure? {
        val id = viewNode.autofillId ?: return null
        val contentDescription = viewNode.contentDescription
        val autoFillHints = viewNode.autofillHints.orEmpty().toMutableList()

        if (verifyHint(contentDescription, autoFillHints, View.AUTOFILL_HINT_PASSWORD)) {
            autoFillHints.add(View.AUTOFILL_HINT_PASSWORD)
        }
        if (verifyHint(contentDescription, autoFillHints, View.AUTOFILL_HINT_USERNAME)) {
            autoFillHints.add(View.AUTOFILL_HINT_USERNAME)
        }

        return ParsedStructure(id, autoFillHints)
    }

    private fun verifyHint(
        contentDescription: CharSequence?,
        autoFillHints: MutableList<String>?,
        autofillHintType: String
    ) =
        autoFillHints?.any { it.contains(autofillHintType, ignoreCase = true) } == true ||
                contentDescription?.contains(autofillHintType, ignoreCase = true) == true
}
