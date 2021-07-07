package com.password.mobile.android.feature.home

import com.passbolt.mobile.android.common.InitialsProvider
import com.passbolt.mobile.android.dto.response.ResourceResponseDto
import com.passbolt.mobile.android.mappers.ResourceModelMapper
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class ResourceModelMapperTest {
    private lateinit var mapper: ResourceModelMapper

    @Before
    fun setUp() {
        mapper = ResourceModelMapper(InitialsProvider())
    }

    @Test
    fun `resource is properly mapped`() {
        val responseDto = ResourceResponseDto(
            id = "id1",
            description = "description",
            name = "firstname",
            uri = "uri",
            username = "username"
        )
        val result = mapper.map(responseDto)
        assertEquals("f", result.initials)
        assertEquals("firstnameusernameuri", result.searchCriteria)
        assertEquals("uri", result.url)
        assertEquals("username", result.subtitle)
        assertEquals("firstname", result.title)
    }
}
