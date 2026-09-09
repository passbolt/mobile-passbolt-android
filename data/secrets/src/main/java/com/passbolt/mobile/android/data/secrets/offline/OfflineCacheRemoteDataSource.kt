package com.passbolt.mobile.android.data.secrets.offline

import com.passbolt.mobile.android.core.architecture.result.DomainResult
import com.passbolt.mobile.android.core.architecture.result.map
import com.passbolt.mobile.android.core.networking.ResponseHandler
import com.passbolt.mobile.android.core.networking.callWithHandler
import com.passbolt.mobile.android.core.networking.toDomainResult
import com.passbolt.mobile.android.data.secrets.datasource.remote.api.OfflineSecretsApi
import com.passbolt.mobile.android.domain.secrets.offline.RemoteResourceSecret
import java.time.ZonedDateTime

internal class OfflineCacheRemoteDataSource(
    private val offlineSecretsApi: OfflineSecretsApi,
    private val responseHandler: ResponseHandler,
) {
    suspend fun fetchSecretsForResources(resourceIds: List<String>): DomainResult<List<RemoteResourceSecret>> =
        callWithHandler(responseHandler) {
            offlineSecretsApi.getResourcesWithSecrets(resourceIds = resourceIds, limit = resourceIds.size)
        }.toDomainResult()
            .map { response ->
                response.body.mapNotNull { resource ->
                    // the index returns only the current user's secret; guard anyway
                    resource.secrets?.firstOrNull()?.let { secret ->
                        RemoteResourceSecret(
                            resourceId = resource.id.toString(),
                            resourceModified = ZonedDateTime.parse(resource.modified),
                            encryptedSecret = secret.data,
                        )
                    }
                }
            }
}
