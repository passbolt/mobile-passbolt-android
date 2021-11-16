package com.passbolt.mobile.android.feature.base

import org.koin.test.KoinTest

fun KoinTest.readFromFile(filename: String): String {
    javaClass.getResourceAsStream(filename)?.use {
        return String(it.readBytes())
    }
    return ""
}
