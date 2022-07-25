package com.passbolt.mobile.android.feature.authentication.auth.challenge

import com.passbolt.mobile.android.dto.response.ChallengeResponseDto
import com.passbolt.mobile.android.feature.authentication.challengeTestModule
import com.passbolt.mobile.android.feature.base.readFromFile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import kotlin.test.assertTrue


@ExperimentalCoroutinesApi
class ChallengeVerifierTest : KoinTest {

    private val challengeVerifier: ChallengeVerifier by inject()
    private val public = readFromFile("/server_rsa_key")
    private val wrongPublic = readFromFile("/wrong_server_rsa_key")
    private val accessToken =
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJodHRwczpcL1wvcGFzc2JvbHQuZGV2XC8iLCJzdWIiOiJlMWViYzU5Mi1iOTBkLTVlMjItOWY0MC01MGU1MjkxMTY3M2IiLCJleHAiOjE2MjQ0NTIxMDd9.IYI-hT-w9oCLfEon_ZBkX91vXTnmGYO1TtIWKHzrcqCg4NTvDblNx-YiTGQGKN3uZVc8OTiaYveholGClfTYKhZPAPxBR6bIV275bpEXQBsjjhu_U_KrUjI84t3tv2feGCAiaicijdjWy8I8mPM5tGKPC7P_DFo8_E_9Oibp-Ex8jvO5MlKJy-Iv9LpbnE0WqviMjQ9GVmRYzjuHuayg_JnP-7vSFZvJMOA_YlbN9L7xwOTVygZqJE3u8vE-WoYa_G3_aYk5gJYw50LAPZ7PXF8kjv7J01UtSVCZTJ7FmE3AvncN8mG6XgEG1501fUGeQpkVnFW7o99XKLRBY9ydBWiBS2GxOXvFd0hLbCgTD0PGKcy7uoicmyE2RaTZvxzlW31zSctub276NweszOjlOBugWiQLLNEoYKzfw0P-udwoH9Gqa4A5Ws4exTUIobu90Pl2Kde96MNSanvBI8f5Qci2FruATuEVg6Vc-674iX1IWitGJwXX7GqjCajPkiE10S6MWe53x6N5Iyy0lseGehOH5okdIaoG7ZgHgF34p8BYSyZB9l4VPWVCw491celE04oDXkfcXD59n95glBNdJth13IbeokcNk6VDXop7bLp3wWxiOEFFjzY8NpPxpxLrGIlkAh73tB1y8jBhhbS4KP3VZL7AE78DNM6B7G6zrnQ"

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        modules(challengeTestModule)
    }

    @Test
    fun `challenge not verified when token expired`() = runTest {
        val challengeResponseDto = ChallengeResponseDto(
            "", "", "", accessToken, "", null
        )
        val result = challengeVerifier.verify(challengeResponseDto, public)
        assertTrue(result is ChallengeVerifier.Output.TokenExpired)
    }

    @Test
    fun `challenge not verified when wrong access token`() = runTest {
        val challengeResponseDto = ChallengeResponseDto(
            "", "", "", "wrong access", "", null
        )
        val result = challengeVerifier.verify(challengeResponseDto, public)
        assertTrue(result is ChallengeVerifier.Output.Failure)
    }

    @Test
    fun `challenge not verified when wrong public key`() = runTest {
        val challengeResponseDto = ChallengeResponseDto(
            "", "", "", accessToken, "", null
        )
        val result = challengeVerifier.verify(challengeResponseDto, wrongPublic)
        assertTrue(result is ChallengeVerifier.Output.InvalidSignature)
    }
}
