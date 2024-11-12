package com.passbolt.mobile.android.serializers.gson

import com.passbolt.mobile.android.core.accounts.usecase.privatekey.GetSelectedUserPrivateKeyUseCase
import com.passbolt.mobile.android.gopenpgp.OpenPgp
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpResult
import com.passbolt.mobile.android.ui.MetadataKeyTypeModel
import com.passbolt.mobile.android.ui.ParsedMetadataKeyModel
import timber.log.Timber

class MetadataEncryptor(
    private val getSelectedUserPrivateKeyUseCase: GetSelectedUserPrivateKeyUseCase,
    private val metadataKeys: List<ParsedMetadataKeyModel>,
    private val openPgp: OpenPgp
) {

    suspend fun encryptMetadata(
        metadataKeyTypeModel: MetadataKeyTypeModel,
        metadataJsonString: String,
        usersPrivateKeyPassphrase: ByteArray
    ): Output {
        return try {
            val (key, passphrase) = when (metadataKeyTypeModel) {
                MetadataKeyTypeModel.PERSONAL -> {
                    val privateKey = getSelectedUserPrivateKeyUseCase.execute(Unit).privateKey
                    require(privateKey != null) { "Selected user private key not found" }
                    privateKey to usersPrivateKeyPassphrase
                }
                MetadataKeyTypeModel.SHARED -> {
                    val metadataPrivateKey = metadataKeys
                        .firstOrNull()
                        ?.metadataPrivateKeys
                        ?.firstOrNull()

                    require(metadataPrivateKey != null) { "Metadata private key not found" }

                    metadataPrivateKey.armoredKey to metadataPrivateKey.passphrase.toByteArray()
                }
            }
            val encryptedMeta = openPgp.encryptSignMessageArmored(
                key,
                passphrase,
                metadataJsonString
            )

            when (encryptedMeta) {
                is OpenPgpResult.Error -> Output.Failure(RuntimeException(encryptedMeta.error.message))
                is OpenPgpResult.Result -> Output.Success(encryptedMeta.result)
            }
        } catch (exception: Exception) {
            Timber.e(exception, "Exception during metadata encryption")
            Output.Failure(exception)
        }
    }

    sealed class Output {
        data class Success(val encryptedMetadata: String) : Output()

        data class Failure(val error: Throwable?) : Output()
    }
}
