package com.passbolt.mobile.android.feature.authentication.auth.challenge

import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import com.passbolt.mobile.android.feature.authentication.challengeTestModule
import com.passbolt.mobile.android.feature.authentication.getPrivateKeyUseCase
import com.passbolt.mobile.android.feature.authentication.openPgp
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpException
import com.passbolt.mobile.android.storage.usecase.privatekey.GetPrivateKeyUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Rule
import org.junit.Test
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class ChallengeDecryptorTest : KoinTest {

    private val challengeDecryptor: ChallengeDecryptor by inject()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        modules(challengeTestModule)
    }

    @Test
    fun `challenge properly decrypted`() = runBlockingTest {
        val privateKey = "private_key"
        val publicKey = "public_key"
        val challenge = "{version: \"1.0\", domain: \"domain\", verify_token: \"verify_token\"," +
                " access_token: \"access_token\", refresh_token: \"refresh_token\"}"
        whenever(getPrivateKeyUseCase.execute(any())).thenReturn(GetPrivateKeyUseCase.Output(privateKey))
        whenever(openPgp.decryptVerifyMessageArmored(eq(publicKey), eq(privateKey), any(), any())).thenReturn(
            challenge.toByteArray()
        )


        val result = challengeDecryptor.decrypt(
            publicKey, "pass".toByteArray(), "userId", "challenge"
        )
        assertThat(result).isInstanceOf(ChallengeDecryptor.Output.DecryptedChallenge::class.java)
        val decryptedChallenge = (result as ChallengeDecryptor.Output.DecryptedChallenge).challenge
        assertEquals("access_token", decryptedChallenge.accessToken)
        assertEquals("verify_token", decryptedChallenge.verifyToken)
        assertEquals("refresh_token", decryptedChallenge.refreshToken)
        assertEquals("domain", decryptedChallenge.domain)
        assertEquals("1.0", decryptedChallenge.version)
    }

    @Test
    fun `challenge value is correct when decryption failure`() = runBlockingTest {
        val privateKey = "private_key"
        val publicKey = "public_key"
        val exceptionMessage = "message"
        whenever(getPrivateKeyUseCase.execute(any())).thenReturn(GetPrivateKeyUseCase.Output(privateKey))
        whenever(openPgp.decryptVerifyMessageArmored(any(), any(), any(), any()))
            .thenThrow(OpenPgpException(exceptionMessage))



        val result = challengeDecryptor.decrypt(
            publicKey, "pass".toByteArray(), "userId", "challenge"
        )
        assertThat(result).isInstanceOf(ChallengeDecryptor.Output.DecryptionError::class.java)
        val decryptionError = (result as ChallengeDecryptor.Output.DecryptionError)
        assertThat(decryptionError.message).isEqualTo(exceptionMessage)
    }

}
