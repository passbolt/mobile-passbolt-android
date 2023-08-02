package com.passbolt.mobile.android.core.resources.actions

import androidx.annotation.VisibleForTesting
import com.passbolt.mobile.android.core.mvp.authentication.UnauthenticatedReason
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.SecretInteractor
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.parser.DecryptedSecret
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.parser.SecretParser
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import com.passbolt.mobile.android.ui.DecryptedSecretOrError
import com.passbolt.mobile.android.ui.ResourceModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.transform
import timber.log.Timber

class SecretPropertiesActionsInteractor(
    private val needSessionRefreshFlow: MutableStateFlow<UnauthenticatedReason?>,
    private val sessionRefreshedFlow: StateFlow<Unit?>,
    private val resource: ResourceModel,
    private val secretParser: SecretParser,
    private val secretInteractor: SecretInteractor
) {

    suspend fun provideDescription(): Flow<SecretPropertyActionResult<String>> =
        fetchAndDecrypt()
            .mapSuccess {
                when (val description = secretParser.extractDescription(resource.resourceTypeId, it.secret)) {
                    is DecryptedSecretOrError.DecryptedSecret ->
                        SecretPropertyActionResult.Success(
                            DESCRIPTION_LABEL,
                            isSecret = true,
                            description.secret
                        )

                    is DecryptedSecretOrError.Error ->
                        SecretPropertyActionResult.DecryptionFailure()
                }
            }

    suspend fun providePassword(): Flow<SecretPropertyActionResult<String>> =
        fetchAndDecrypt()
            .mapSuccess {
                when (val password = secretParser.extractPassword(resource.resourceTypeId, it.secret)) {
                    is DecryptedSecretOrError.DecryptedSecret ->
                        SecretPropertyActionResult.Success(
                            SECRET_LABEL,
                            isSecret = true,
                            password.secret
                        )
                    is DecryptedSecretOrError.Error ->
                        SecretPropertyActionResult.DecryptionFailure()
                }
            }

    suspend fun provideOtp(): Flow<SecretPropertyActionResult<DecryptedSecret.StandaloneTotp.Totp>> =
        fetchAndDecrypt()
            .mapSuccess {
                when (val totp = secretParser.extractTotpData(resource.resourceTypeId, it.secret)) {
                    is DecryptedSecretOrError.DecryptedSecret ->
                        SecretPropertyActionResult.Success(
                            OTP_LABEL,
                            isSecret = true,
                            totp.secret
                        )
                    is DecryptedSecretOrError.Error ->
                        SecretPropertyActionResult.DecryptionFailure()
                }
            }

    private suspend fun fetchAndDecrypt(): Flow<SecretFetchAndDecryptResult> = flowOf(
        when (val output =
            runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
                secretInteractor.fetchAndDecrypt(resource.resourceId)
            }
        ) {
            is SecretInteractor.Output.DecryptFailure ->
                SecretFetchAndDecryptResult.DecryptFailure
            is SecretInteractor.Output.FetchFailure ->
                SecretFetchAndDecryptResult.FetchFailure
            is SecretInteractor.Output.Success ->
                SecretFetchAndDecryptResult.Success(output.decryptedSecret)
            is SecretInteractor.Output.Unauthorized ->
                SecretFetchAndDecryptResult.Unauthorized
        }
    )

    private inline fun <T> Flow<SecretFetchAndDecryptResult>.mapSuccess(
        crossinline transform: suspend (value: SecretFetchAndDecryptResult.Success) -> SecretPropertyActionResult<T>
    ): Flow<SecretPropertyActionResult<T>> =
        transform {
            emit(
                when (it) {
                    is SecretFetchAndDecryptResult.DecryptFailure ->
                        SecretPropertyActionResult.DecryptionFailure()
                    is SecretFetchAndDecryptResult.FetchFailure ->
                        SecretPropertyActionResult.FetchFailure()
                    is SecretFetchAndDecryptResult.Unauthorized ->
                        SecretPropertyActionResult.Unauthorized()
                    is SecretFetchAndDecryptResult.Success -> {
                        transform(it)
                    }
                }
            )
        }

    private sealed class SecretFetchAndDecryptResult {

        object FetchFailure : SecretFetchAndDecryptResult()

        object DecryptFailure : SecretFetchAndDecryptResult()

        object Unauthorized : SecretFetchAndDecryptResult()

        class Success(val secret: ByteArray) : SecretFetchAndDecryptResult()
    }

    companion object {
        @VisibleForTesting
        const val SECRET_LABEL = "Secret"

        @VisibleForTesting
        const val DESCRIPTION_LABEL = "Description"

        @VisibleForTesting
        const val OTP_LABEL = "TOTP"
    }
}

suspend fun <T> performSecretPropertyAction(
    action: suspend () -> Flow<SecretPropertyActionResult<T>>,
    doOnFetchFailure: () -> Unit,
    doOnDecryptionFailure: () -> Unit,
    doOnSuccess: (SecretPropertyActionResult.Success<T>) -> Unit
) {
    action().single().let {
        when (it) {
            is SecretPropertyActionResult.DecryptionFailure -> {
                doOnDecryptionFailure()
            }
            is SecretPropertyActionResult.FetchFailure -> {
                doOnFetchFailure()
            }
            is SecretPropertyActionResult.Success -> {
                doOnSuccess(it)
            }
            is SecretPropertyActionResult.Unauthorized -> {
                // can be ignored - runAuthenticatedOperation handles it
                Timber.d("Unauthorized during decrypting secret")
            }
        }
    }
}
