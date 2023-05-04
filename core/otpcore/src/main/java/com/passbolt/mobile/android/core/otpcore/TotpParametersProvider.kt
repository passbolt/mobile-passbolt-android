package com.passbolt.mobile.android.core.otpcore

import dev.turingcomplete.kotlinonetimepassword.HmacAlgorithm
import dev.turingcomplete.kotlinonetimepassword.TimeBasedOneTimePasswordConfig
import dev.turingcomplete.kotlinonetimepassword.TimeBasedOneTimePasswordGenerator
import org.apache.commons.codec.binary.Base32
import java.util.concurrent.TimeUnit

class TotpParametersProvider {

    fun provideOtpParameters(secretKey: String, digits: Int, period: Long, algorithm: String): OtpParameters {
        val generatorConfig = TimeBasedOneTimePasswordConfig(
            codeDigits = digits,
            hmacAlgorithm = mapAlgorithm(algorithm),
            timeStep = period,
            timeStepUnit = TimeUnit.SECONDS
        )

        val decodedSecret = Base32().decode(secretKey)
        val totpGenerator = TimeBasedOneTimePasswordGenerator(decodedSecret, generatorConfig)
        val counter = totpGenerator.counter()
        // get the beginning of the next timeslot -1 millisecond
        val currentTimeslotEndMillis = totpGenerator.timeslotStart(counter + 1) - 1

        @Suppress("MagicNumber")
        val secondsValid = (currentTimeslotEndMillis - System.currentTimeMillis()) / 1_000

        return OtpParameters(otpValue = totpGenerator.generate(), secondsValid = secondsValid)
    }

    private fun mapAlgorithm(algorithm: String) =
        when (algorithm) {
            "SHA1" -> HmacAlgorithm.SHA1
            "SHA256" -> HmacAlgorithm.SHA256
            "SHA512" -> HmacAlgorithm.SHA512
            else -> throw IllegalArgumentException("Unsupported TOTP algorithm: $algorithm")
        }

    data class OtpParameters(
        val otpValue: String,
        val secondsValid: Long
    )
}
