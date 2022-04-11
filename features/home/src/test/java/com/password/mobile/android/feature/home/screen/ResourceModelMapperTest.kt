package com.password.mobile.android.feature.home.screen

import com.passbolt.mobile.android.common.InitialsProvider
import com.passbolt.mobile.android.dto.response.PermissionDto
import com.passbolt.mobile.android.dto.response.ResourceResponseDto
import com.passbolt.mobile.android.mappers.ResourceModelMapper
import com.passbolt.mobile.android.ui.ResourcePermission
import org.junit.Before
import org.junit.Test
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
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
            resourceTypeId = "resTypeId",
            description = "description",
            name = "firstname",
            uri = "uri",
            username = "username",
            permission = PermissionDto("abc", 1, null, null, null, null, null, null),
            favorite = null,
            modified = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            resourceFolderId = null,
            tags = emptyList(),
            permissions = emptyList()
        )
        val result = mapper.map(responseDto)
        assertEquals("f", result.initials)
        assertEquals("uri", result.url)
        assertEquals("username", result.username)
        assertEquals("firstname", result.name)
        assertEquals(ResourcePermission.READ, result.permission)
        assertEquals(false, result.isFavourite)
    }
}
