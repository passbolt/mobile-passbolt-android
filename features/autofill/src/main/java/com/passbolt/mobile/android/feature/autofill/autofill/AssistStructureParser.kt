package com.passbolt.mobile.android.feature.autofill.autofill

import android.annotation.SuppressLint
import android.app.assist.AssistStructure
import android.view.ViewStructure
import timber.log.Timber

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
class AssistStructureParser {

    fun parse(assistStructure: AssistStructure): Set<ParsedStructure> {
        val result = mutableSetOf<ParsedStructure>()
        (0 until assistStructure.windowNodeCount)
            .map { assistStructure.getWindowNodeAt(it).rootViewNode }
            .forEach { visitNode(it, result) }
        return result
    }

    private fun visitNode(
        viewNode: AssistStructure.ViewNode,
        result: MutableSet<ParsedStructure>
    ) {
        parseNode(viewNode)?.let { result.add(it) }

        (0 until viewNode.childCount)
            .map { viewNode.getChildAt(it) }
            .forEach { visitNode(it, result) }
    }

    private fun parseNode(viewNode: AssistStructure.ViewNode): ParsedStructure? {
        logNodeVisit(viewNode)

        val autofillId = viewNode.autofillId ?: return null
        val domain = viewNode.webScheme.orEmpty().let { webScheme ->
            if (webScheme.isEmpty()) {
                viewNode.webDomain
            } else {
                "%s://%s".format(webScheme, viewNode.webDomain)
            }
        }
        val autofillHints = viewNode.autofillHints
        val packageName = viewNode.idPackage
        val inputType = viewNode.inputType

        val heuristicAutofillHints = if (!autofillHints.isNullOrEmpty()) {
            autofillHints.toList()
        } else {
            gatherHtmlHints(viewNode) + gatherHintHints(viewNode) + gatherContentDescriptionHints(viewNode)
        }
        return ParsedStructure(autofillId, heuristicAutofillHints, inputType, domain, packageName)
    }

    private fun gatherContentDescriptionHints(viewNode: AssistStructure.ViewNode): List<String> {
        return viewNode.contentDescription.let {
            if (it != null) {
                listOf(it.toString())
            } else {
                emptyList()
            }
        }
    }

    private fun gatherHintHints(viewNode: AssistStructure.ViewNode): List<String> {
        return viewNode.hint.let {
            if (it != null) {
                listOf(it)
            } else {
                emptyList()
            }
        }
    }

    private fun gatherHtmlHints(viewNode: AssistStructure.ViewNode): List<String> {
        return if (viewNode.htmlInfo.hasAttribute(HTML_AUTOCOMPLETE_ATTR)) {
            viewNode.htmlInfo.getAttribute(HTML_AUTOCOMPLETE_ATTR)
                ?.split(HTML_AUTOCOMPLETE_ATTR_VALUES_DELIMITER)
                ?.filter { it.isBlank() } ?: emptyList()
        } else {
            emptyList()
        }
    }

    @SuppressLint("BinaryOperationInTimber")
    private fun logNodeVisit(viewNode: AssistStructure.ViewNode) {
        Timber.d(
            "Visiting view node with id: %d \n" +
                    "scheme + domain: %s://%s \n" +
                    "package: %s \n" +
                    "content description: %s \n" +
                    "autofill hints %s \n" +
                    "hint: %s \n" +
                    "html autocomplete attr: %s \n" +
                    "important for autofill: %d \n" +
                    "input type: %d \n",
            viewNode.id,
            viewNode.webScheme,
            viewNode.webDomain,
            viewNode.idPackage,
            viewNode.contentDescription,
            viewNode.autofillHints?.joinToString(separator = ","),
            viewNode.hint,
            viewNode.htmlInfo.getAttribute("autocomplete"),
            viewNode.importantForAutofill,
            viewNode.inputType
        )
    }

    private fun ViewStructure.HtmlInfo?.getAttribute(name: String) =
        this?.attributes?.firstOrNull { it.first == name }?.second

    private fun ViewStructure.HtmlInfo?.hasAttribute(name: String) =
        this?.attributes?.any { it.first == name } == true

    private companion object {
        private const val HTML_AUTOCOMPLETE_ATTR = "autocomplete"
        private const val HTML_AUTOCOMPLETE_ATTR_VALUES_DELIMITER = " "
    }
}
