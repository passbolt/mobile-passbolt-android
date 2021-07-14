package com.passbolt.mobile.android.storage.encrypted.biometric

import android.security.keystore.UserNotAuthenticatedException
import android.util.Base64
import java.security.Key
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec

class CipherWrapper(
    private val cipher: Cipher
) {

    // B64(iv)]B64(ENCRYPT(data))
    @Throws(UserNotAuthenticatedException::class)
    fun encrypt(data: ByteArray, key: Key?): ByteArray {
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val iv = cipher.iv
        val ivString = Base64.encodeToString(iv, Base64.DEFAULT)
        var result = ivString + IV_SEPARATOR

        val bytes = cipher.doFinal(data)
        result += Base64.encodeToString(bytes, Base64.DEFAULT)

        return result.toByteArray()
    }

    @Throws(UserNotAuthenticatedException::class)
    fun decrypt(data: String, key: Key?): ByteArray {
        val split = data.split(IV_SEPARATOR.toRegex())
        if (split.size != 2)
            throw IllegalArgumentException("Passed data is incorrect. There was no IV specified with it.")

        val ivString = split[0] // B64(iv)
        val encodedEncryptedData = split[1] // B64(ENCRYPT(data))
        val ivSpec = IvParameterSpec(Base64.decode(ivString, Base64.DEFAULT))
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)

        val encryptedData = Base64.decode(encodedEncryptedData, Base64.DEFAULT)
        return cipher.doFinal(encryptedData)
    }

    companion object {
        private const val IV_SEPARATOR = "]"
    }
}
