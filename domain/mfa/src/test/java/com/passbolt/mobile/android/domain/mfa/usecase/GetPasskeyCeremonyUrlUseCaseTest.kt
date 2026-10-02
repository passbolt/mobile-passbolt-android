package com.passbolt.mobile.android.domain.mfa.usecase

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.domain.accounts.usecase.GetCurrentApiUrlUseCase
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class GetPasskeyCeremonyUrlUseCaseTest {
    private val getCurrentApiUrlUseCase = mock<GetCurrentApiUrlUseCase>()

    private val useCase = GetPasskeyCeremonyUrlUseCase(getCurrentApiUrlUseCase)

    @Test
    fun `should build delegated ceremony url with state query`() {
        whenever(getCurrentApiUrlUseCase.execute(Unit)) doReturn GetCurrentApiUrlUseCase.Output(BASE_URL)

        val output = useCase.execute(GetPasskeyCeremonyUrlUseCase.Input(STATE))

        assertThat(output.url).isEqualTo("$BASE_URL/mfa/verify/webauthn/delegated?mfa_delegated_state=$STATE")
    }

    @Test
    fun `should not double slash when api url has trailing slash`() {
        whenever(getCurrentApiUrlUseCase.execute(Unit)) doReturn GetCurrentApiUrlUseCase.Output("$BASE_URL/")

        val output = useCase.execute(GetPasskeyCeremonyUrlUseCase.Input(STATE))

        assertThat(output.url).isEqualTo("$BASE_URL/mfa/verify/webauthn/delegated?mfa_delegated_state=$STATE")
    }

    @Test
    fun `should url encode the state`() {
        whenever(getCurrentApiUrlUseCase.execute(Unit)) doReturn GetCurrentApiUrlUseCase.Output(BASE_URL)

        val output = useCase.execute(GetPasskeyCeremonyUrlUseCase.Input("a b&c"))

        assertThat(output.url).isEqualTo("$BASE_URL/mfa/verify/webauthn/delegated?mfa_delegated_state=a+b%26c")
    }

    private companion object {
        const val BASE_URL = "https://passbolt.example.com"
        const val STATE = "delegated-state-123"
    }
}
