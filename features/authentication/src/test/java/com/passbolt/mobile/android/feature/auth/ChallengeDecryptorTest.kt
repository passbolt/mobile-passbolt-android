package com.passbolt.mobile.android.feature.auth

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import com.passbolt.mobile.android.feature.authentication.auth.challenge.ChallengeDecryptor
import com.passbolt.mobile.android.storage.usecase.GetPrivateKeyUseCase
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
        modules(testModule)
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
        assertEquals("access_token", result.accessToken)
        assertEquals("verify_token", result.verifyToken)
        assertEquals("refresh_token", result.refreshToken)
        assertEquals("domain", result.domain)
        assertEquals("1.0", result.version)
    }

}
