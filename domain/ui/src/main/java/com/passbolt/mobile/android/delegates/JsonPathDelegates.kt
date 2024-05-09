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

package com.passbolt.mobile.android.delegates

import com.jayway.jsonpath.JsonPath
import org.koin.core.component.KoinComponent
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class StringDelegate :
    ReadWriteProperty<JsonModel, String>, KoinComponent {

    override fun getValue(thisRef: JsonModel, property: KProperty<*>): String {
        return thisRef.json
    }

    override fun setValue(thisRef: JsonModel, property: KProperty<*>, value: String) {
        thisRef.json = value
    }
}

class JsonPathDelegate<T : Any>(private val jsonPath: String) :
    ReadWriteProperty<JsonModel, T>, KoinComponent {

    override fun getValue(thisRef: JsonModel, property: KProperty<*>): T {
        return JsonPath.read(thisRef.json, jsonPath)
    }

    override fun setValue(thisRef: JsonModel, property: KProperty<*>, value: T) {
        thisRef.json = JsonPath.parse(thisRef.json).set(jsonPath, value).jsonString()
    }
}

class JsonPathNullableDelegate<T>(private val jsonPath: String) :
    ReadWriteProperty<JsonModel, T?>, KoinComponent {

    override fun getValue(thisRef: JsonModel, property: KProperty<*>): T? {
        return readOrNull(thisRef.json, jsonPath)
    }

    override fun setValue(thisRef: JsonModel, property: KProperty<*>, value: T?) {
        thisRef.json = JsonPath.parse(thisRef.json).set(jsonPath, value).jsonString()
    }

    private fun <T> readOrNull(json: String, path: String): T? {
        return try {
            JsonPath.read(json, path)
        } catch (e: Exception) {
            null
        }
    }
}
