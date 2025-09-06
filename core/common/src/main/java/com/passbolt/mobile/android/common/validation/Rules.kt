package com.passbolt.mobile.android.common.validation

import android.webkit.URLUtil
import java.net.MalformedURLException
import java.net.URI
import java.net.URL

open class Rule<T>(
    val condition: (T) -> Boolean,
)

object StringNotBlank :
    Rule<String>({ it.isNotBlank() })

object StringIsAPositiveIntegerNumber :
    Rule<String>({ (it.toIntOrNull() != null) && (it.toInt() > 0) })

class StringMaxLength(
    length: Int,
) : Rule<String>({ it.length <= length })

object StringIsUuid :
    Rule<String>({ UUID_PATTERN.toRegex().matches(it) })

object StringIsHttpsWebUrl :
    Rule<String>({
        try {
            URLUtil.isHttpsUrl(URL(it).toString())
            true
        } catch (e: MalformedURLException) {
            false
        }
    })

class UriIsOfScheme(
    private val scheme: String,
) : Rule<URI>({ scheme == it.scheme })

class UriIsOfAuthority(
    private val authority: String,
) : Rule<URI>({ authority == it.authority })

object StringIsBase32 :
    Rule<String>({ Regex(BASE_32_PATTERN).matches(it.uppercase()) })

private const val UUID_PATTERN = "^[{]?[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}[}]?$"

private const val BASE_32_PATTERN = "^[A-Z2-7]+=*$"
