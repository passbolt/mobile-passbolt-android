package com.passbolt.mobile.android.domain.mfa.usecase

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.architecture.result.DomainResult
import com.passbolt.mobile.android.domain.mfa.MfaRepository
import com.passbolt.mobile.android.domain.mfa.model.WebauthnVerification
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertIs

class VerifyWebauthnExchangeCodeUseCaseTest {
    private val mfaRepository = mock<MfaRepository>()

    private val useCase = VerifyWebauthnExchangeCodeUseCase(mfaRepository)

    @Test
    fun `should return mfa header and pass remember flag when verification succeeds`() =
        runTest {
            whenever(mfaRepository.verifyWebauthnExchangeCode(JWT, EXCHANGE_CODE, true)) doReturn
                DomainResult.Finished(WebauthnVerification.Succeeded(MFA_HEADER))

            val output = useCase.execute(VerifyWebauthnExchangeCodeUseCase.Input(JWT, EXCHANGE_CODE, remember = true))

            assertIs<VerifyWebauthnExchangeCodeUseCase.Output.Success>(output)
            assertThat(output.mfaHeader).isEqualTo(MFA_HEADER)
            verify(mfaRepository).verifyWebauthnExchangeCode(JWT, EXCHANGE_CODE, true)
        }

    @Test
    fun `should return unauthorized when verification is unauthorized`() =
        runTest {
            whenever(mfaRepository.verifyWebauthnExchangeCode(JWT, EXCHANGE_CODE, false)) doReturn
                DomainResult.Finished(WebauthnVerification.Unauthorized)

            val output = useCase.execute(VerifyWebauthnExchangeCodeUseCase.Input(JWT, EXCHANGE_CODE, remember = false))

            assertIs<VerifyWebauthnExchangeCodeUseCase.Output.Unauthorized>(output)
        }

    @Test
    fun `should return network failure with error code on other failure`() =
        runTest {
            whenever(mfaRepository.verifyWebauthnExchangeCode(JWT, EXCHANGE_CODE, false)) doReturn
                DomainResult.Finished(WebauthnVerification.OtherFailure(400))

            val output = useCase.execute(VerifyWebauthnExchangeCodeUseCase.Input(JWT, EXCHANGE_CODE, remember = false))

            assertIs<VerifyWebauthnExchangeCodeUseCase.Output.NetworkFailure>(output)
            assertThat(output.errorCode).isEqualTo(400)
        }

    @Test
    fun `should return failure when repository result is incomplete`() =
        runTest {
            whenever(mfaRepository.verifyWebauthnExchangeCode(JWT, EXCHANGE_CODE, false)) doReturn
                DomainResult.Incomplete.Unauthorized

            val output = useCase.execute(VerifyWebauthnExchangeCodeUseCase.Input(JWT, EXCHANGE_CODE, remember = false))

            assertIs<VerifyWebauthnExchangeCodeUseCase.Output.Failure>(output)
            assertThat(output.incomplete).isEqualTo(DomainResult.Incomplete.Unauthorized)
        }

    private companion object {
        const val JWT = "jwt"
        const val EXCHANGE_CODE = "exchange-code"
        const val MFA_HEADER = "passbolt_mfa=jwt"
    }
}
