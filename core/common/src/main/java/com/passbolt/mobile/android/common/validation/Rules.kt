package com.passbolt.mobile.android.common.validation

import android.webkit.URLUtil
import java.net.MalformedURLException
import java.net.URL

open class Rule<T>(val condition: (T) -> Boolean)

object StringNotBlank :
    Rule<String>({ it.isNotBlank() })

class StringMaxLength(length: Int) :
    Rule<String>({ it.length <= length })

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

private const val UUID_PATTERN = "^[{]?[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}[}]?$"
