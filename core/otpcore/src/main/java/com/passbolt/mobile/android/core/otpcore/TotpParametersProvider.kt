package com.passbolt.mobile.android.core.otpcore

import dev.turingcomplete.kotlinonetimepassword.TimeBasedOneTimePasswordConfig
import dev.turingcomplete.kotlinonetimepassword.TimeBasedOneTimePasswordGenerator
import org.apache.commons.codec.binary.Base32
import timber.log.Timber
import java.util.concurrent.TimeUnit

class TotpParametersProvider(
    private val otpMapper: OtpMapper,
) {
    fun provideOtpParameters(
        secretKey: String,
        digits: Int,
        period: Long,
        algorithm: String,
    ): OtpParametersResult {
        val generatorConfig =
            TimeBasedOneTimePasswordConfig(
                codeDigits = digits,
                hmacAlgorithm = otpMapper.mapAlgorithmToLibraryAlgorithm(algorithm),
                timeStep = period,
                timeStepUnit = TimeUnit.SECONDS,
            )

        val decodedSecret = Base32().decode(secretKey)
        val totpGenerator = TimeBasedOneTimePasswordGenerator(decodedSecret, generatorConfig)
        val counter = totpGenerator.counter()
        // get the beginning of the next timeslot -1 millisecond
        val currentTimeslotEndMillis = totpGenerator.timeslotStart(counter + 1) - 1

        @Suppress("MagicNumber")
        val secondsValid = (currentTimeslotEndMillis - System.currentTimeMillis()) / 1_000

        return try {
            OtpParametersResult.OtpParameters(otpValue = totpGenerator.generate(), secondsValid = secondsValid)
        } catch (e: Exception) {
            Timber.e(e, "Error generating TOTP")
            OtpParametersResult.InvalidTotpInput
        }
    }

    sealed class OtpParametersResult {
        data class OtpParameters(
            val otpValue: String,
            val secondsValid: Long,
        ) : OtpParametersResult()

        data object InvalidTotpInput : OtpParametersResult()
    }
}
