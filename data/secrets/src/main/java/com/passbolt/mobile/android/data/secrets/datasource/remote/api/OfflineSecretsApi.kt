package com.passbolt.mobile.android.data.secrets.datasource.remote.api

import com.passbolt.mobile.android.dto.response.BasePaginatedResponse
import com.passbolt.mobile.android.dto.response.ResourceSecretsResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

internal interface OfflineSecretsApi {
    /**
     * Secrets of several resources in one round trip. `contain[secret]` is part of the
     * resources index whitelist; the server logs each secret access as it does for
     * `/secrets/resource/{id}.json`.
     */
    @GET(RESOURCES)
    suspend fun getResourcesWithSecrets(
        @Query(QUERY_FILTER_HAS_ID) resourceIds: List<String>,
        @Query(QUERY_LIMIT) limit: Int,
        @Query(QUERY_CONTAIN_SECRET) containSecret: Int = 1,
        @Query(QUERY_PAGE) page: Int = 1,
    ): BasePaginatedResponse<List<ResourceSecretsResponseDto>>

    private companion object {
        private const val RESOURCES = "resources.json"
        private const val QUERY_CONTAIN_SECRET = "contain[secret]"
        private const val QUERY_FILTER_HAS_ID = "filter[has-id][]"
        private const val QUERY_LIMIT = "limit"
        private const val QUERY_PAGE = "page"
    }
}
