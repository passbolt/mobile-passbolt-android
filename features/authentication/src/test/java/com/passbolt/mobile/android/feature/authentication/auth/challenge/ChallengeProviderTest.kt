package com.passbolt.mobile.android.feature.authentication.auth.challenge

import com.passbolt.mobile.android.core.accounts.usecase.privatekey.GetPrivateKeyUseCase
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class ChallengeProviderTest : KoinTest {
    private val challengeProvider: ChallengeProvider by inject()

    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            modules(challengeTestModule)
        }

    @Test
    fun `challenge properly provided`() =
        runTest {
            val publicKey = "public_key"
            val privateKey = "private_key"
            val domain = "domain"

            whenever(openPgp.encryptSignMessageArmored(eq(publicKey), eq(privateKey), any(), any()))
                .thenReturn(OpenPgpResult.Result(challenge))
            whenever(timeProvider.getCurrentEpochSeconds()).thenReturn(1624448538)
            whenever(uuidProvider.get()).thenReturn("555a30f6-48f0-42be-beca-d200347f1848")
            whenever(getPrivateKeyUseCase.execute(any())).thenReturn(GetPrivateKeyUseCase.Output(privateKey))

            val result =
                challengeProvider.get(
                    domain,
                    publicKey,
                    "pass".toByteArray(),
                    "userId",
                )
            assertTrue(result is ChallengeProvider.Output.Success)
            assertEquals(challenge, result.challenge)
        }

    private val challenge =
        "-----BEGIN PGP MESSAGE-----\\n" +
            "Version: GopenPGP 2.1.7\\n" +
            "Comment: https://gopenpgp.org\\n" +
            "\\n" +
            "wcBMAxj9pz8No5f+AQf/evIjo9cR6xlklOG2KjGTBA+wko14n56wylcGu7ZMBkF4\\n" +
            "dX23TQDXYCvMxiB4TxdLOyRCSuXJ9cLwfOs//PKKeEvDY2TN2EgL4Vwmq8Nvkq0Y\\n" +
            "ZABt8vx9azVS+/oYUC0qfDdzAUKKhjTmCIQjI5ryvkABPYUejmiQ1XLPyiSstK3h\\n" +
            "KRaORfDYWiCvH2VOlBEN4bR3gmfX/oHTKZxa2o8otnuyo67Rf9s7OUa0oEii4K6r\\n" +
            "xGSBZt565iV0nXd74//lEYr4gv86/6GdTkN5wbslb35MyDf6bAMKKv6NLOFuBmfQ\\n" +
            "k02th8lBBxAmeHiBlp21Mh1I4/G5aADB7HZBwol96dLpAdX221sYcNgffDORtvYw\\n" +
            "Lwpepf+wPnpkasLnGYavn08CrYANKOBsCwNy+NRrrDXWK8s2j+F+bx1boUnRmZ44\\n" +
            "wyApwW6ikQ1FwMK6dTd5e1yxjuOaP5DdlTQvi53DfbHOiCQV44Nr4roR+3CEoQ9k\\n" +
            "KEXREg08E9iYc21S/52h8fJKppnvwMk+KJH3WwAHgeX2B/Z4Zv/aFDLaY/YTYJ6r\\n" +
            "mx/uTCkQ9TjaOrG8pPt23ZayI/pSnfznN+8KwtJqiyud3Y61Iln3SKrOqtYrnj85\\n" +
            "cpW3WPWpTLi0mgK/q2Mz6BF9bMsIM+ZiViYXuEfMp4kzcl8LFVHMaePSIZCR/1mx\\n" +
            "qulQTfeTOw0U7i6quK2WEpjskhcgI+/lV7EXm1GzUi3Tq4S02Kzxtv/Ig51EiJTY\\n" +
            "CJMfouIfBKqGL3z0aZjipH/v57EgxVKhFcrDlAR2F9bNVP5fuB217o//9TxDzCvC\\n" +
            "3ts1EiN9h58KE0EvE5ahqQcAZSu2r2gW//s04tZHhHyj3RwI995kBe9O/RnP0UfM\\n" +
            "wYhrseuRDCJhjcMzZuoF0M8ejjAXnigjVaZNqj0UcMYXtzvH7oAaSIPM42c2S6we\\n" +
            "toRUyysFkqbOPGwIOBP2d14atnuCX1eUr6rdnMPuuxYZTYv2yfTSJ5gr959mAsoX\\n" +
            "xph7aFzcJrxZOdTSvHONL6PAQKkWGhzVUmtiyo5nmNl9w+PiZ1sdZAoVc8nkAR3+\\n" +
            "Z6D7wTrskjBhGCAc4Fjukot/rwrRddzGOL/lYc49cRFpWYanBozF/8ytYPU483Uv\\n" +
            "TfiFg6eWbP8vJ5uAQsHlQKhnajB3c35b3Ubi0Xy4bgcsgT/2n27tqiaZ3qMKIFVd\\n" +
            "4zqhUhaHLfR3/7krJ5ckEbM9JX4Ik8KhrmCIXWaw5tsK5QyVVR0jAjOWejxzfcwR\\n" +
            "5Go4N2KBEjpA876B1zuh96kAhEldpOOAP5CgBjpKUifzqmzgCJwEJNtHg46DdBzQ\\n" +
            "NF/ProYlPTWwfvl1K5WUIVOthWuKv053lA8mYOomUGu1B8k=\\n" +
            "=HHZy\\n" +
            "-----END PGP MESSAGE-----"
}
