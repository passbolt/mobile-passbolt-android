package com.passbolt.mobile.android.common.validation

open class Rule<T>(val condition: (T) -> Boolean)

object StringNotBlank :
    Rule<String>({ it.isNotBlank() })

class StringMaxLength(length: Int) :
    Rule<String>({ it.length <= length })
