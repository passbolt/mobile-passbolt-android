package com.passbolt.mobile.android.feature.base

private fun Object.readFromFile(filename: String?): String {
    val inputStream = javaClass.getResourceAsStream(filename)
    val stringBuilder = StringBuilder()
    var i: Int
    val b = ByteArray(4096)
    while (inputStream.read(b).also { i = it } != -1) {
        stringBuilder.append(String(b, 0, i))
    }
    return stringBuilder.toString()
}
