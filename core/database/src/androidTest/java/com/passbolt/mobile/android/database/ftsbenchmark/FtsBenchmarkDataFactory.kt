package com.passbolt.mobile.android.database.ftsbenchmark

import com.passbolt.mobile.android.entity.resource.Permission
import com.passbolt.mobile.android.entity.resource.Resource
import com.passbolt.mobile.android.entity.resource.ResourceAndTagsCrossRef
import com.passbolt.mobile.android.entity.resource.ResourceMetadata
import com.passbolt.mobile.android.entity.resource.ResourceType
import com.passbolt.mobile.android.entity.resource.ResourceUpdateState
import com.passbolt.mobile.android.entity.resource.ResourceUri
import com.passbolt.mobile.android.entity.resource.Tag
import java.time.ZonedDateTime
import java.util.UUID

/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2026 Passbolt SA
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * The name "Passbolt" is a registered trademark of Passbolt SA, and Passbolt SA hereby declines to grant a trademark
 * license to "Passbolt" pursuant to the GNU Affero General Public License version 3 Section 7(e), without a separate
 * agreement with Passbolt SA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Passbolt SA (https://www.passbolt.com)
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://www.passbolt.com Passbolt (tm)
 * @since v1.0
 */

object FtsBenchmarkDataFactory {
    const val RESOURCE_TYPE_ID = "benchmark-resource-type"
    const val SLUG = "password-and-description"

    private const val FAVOURITE_EVERY_NTH = 7
    private const val CUSTOM_FIELDS_EVERY_NTH = 5
    private const val TAG_EVERY_NTH = 3

    private val NAMES =
        listOf(
            "Gmail",
            "GitHub",
            "Slack",
            "Jira",
            "Confluence",
            "AWS Console",
            "Azure Portal",
            "Docker Hub",
            "Bitbucket",
            "GitLab",
            "Figma",
            "Notion",
            "Linear",
            "Vercel",
            "Cloudflare",
            "DigitalOcean",
            "Heroku",
            "Firebase",
            "MongoDB Atlas",
            "Redis Cloud",
            "Stripe Dashboard",
            "PayPal Business",
            "Twilio",
            "SendGrid",
            "Datadog",
            "New Relic",
            "PagerDuty",
            "Sentry",
            "Grafana",
            "Kibana",
            "Netflix",
            "Spotify",
            "Twitter",
            "LinkedIn",
            "Facebook",
            "Instagram",
            "Pinterest",
            "Reddit",
            "Medium",
            "WordPress",
        )

    private val DOMAINS =
        listOf(
            "google.com",
            "github.com",
            "slack.com",
            "atlassian.net",
            "aws.amazon.com",
            "portal.azure.com",
            "hub.docker.com",
            "bitbucket.org",
            "gitlab.com",
            "figma.com",
            "notion.so",
            "linear.app",
            "vercel.com",
            "cloudflare.com",
            "digitalocean.com",
            "heroku.com",
            "firebase.google.com",
            "mongodb.com",
            "redis.com",
            "stripe.com",
        )

    private val USERNAMES =
        listOf(
            "admin",
            "john.doe",
            "jane.smith",
            "devops",
            "deploy-bot",
            "ci-runner",
            "sysadmin",
            "root",
            "service-account",
            "team-lead",
        )

    private val TAG_NAMES =
        listOf(
            "production",
            "staging",
            "development",
            "personal",
            "shared",
            "critical",
            "deprecated",
            "temporary",
            "api-key",
            "database",
        )

    fun createResourceType() =
        ResourceType(
            resourceTypeId = RESOURCE_TYPE_ID,
            name = "Password with description",
            slug = SLUG,
            deleted = null,
        )

    fun createTags(): List<Tag> =
        TAG_NAMES.mapIndexed { index, name ->
            Tag(
                id = "tag-$index",
                slug = name,
                isShared = index % 2 == 0,
            )
        }

    fun createDataSet(size: Int): DataSet {
        val now = ZonedDateTime.now()
        val tags = createTags()
        val resourceIds = List(size) { UUID.randomUUID().toString() }

        val resources =
            resourceIds.mapIndexed { index, resourceId ->
                createResource(resourceId, index, now)
            }

        val metadata =
            resourceIds.mapIndexed { index, resourceId ->
                val baseName = NAMES[index % NAMES.size]
                val nameCycle = index / NAMES.size
                val name = "$baseName $nameCycle"
                val username = USERNAMES[index % USERNAMES.size]
                val domain = DOMAINS[index % DOMAINS.size]
                createMetadata(resourceId, name, username, domain, index)
            }

        val uris =
            resourceIds.mapIndexed { index, resourceId ->
                val domain = DOMAINS[index % DOMAINS.size]
                ResourceUri(resourceId = resourceId, uri = "https://$domain/login")
            }

        val tagCrossRefs =
            resourceIds.indices
                .filter { it % TAG_EVERY_NTH == 0 }
                .map { index ->
                    ResourceAndTagsCrossRef(
                        tagId = tags[index % tags.size].id,
                        resourceId = resourceIds[index],
                    )
                }

        return DataSet(resources, metadata, uris, tags, tagCrossRefs)
    }

    private fun createResource(
        resourceId: String,
        index: Int,
        now: ZonedDateTime,
    ) = Resource(
        resourceId = resourceId,
        folderId = null,
        resourcePermission = Permission.entries[index % Permission.entries.size],
        resourceTypeId = RESOURCE_TYPE_ID,
        favouriteId = if (index % FAVOURITE_EVERY_NTH == 0) UUID.randomUUID().toString() else null,
        modified = now.minusMinutes(index.toLong()),
        expiry = null,
        metadataKeyId = null,
        metadataKeyType = null,
        updateState = ResourceUpdateState.UPDATED,
    )

    private fun createMetadata(
        resourceId: String,
        name: String,
        username: String,
        domain: String,
        index: Int,
    ) = ResourceMetadata(
        resourceId = resourceId,
        metadataJson = """{"name":"$name","username":"$username"}""",
        name = name,
        username = "$username@$domain",
        description = "Credentials for $name on $domain",
        customFieldsKeys = if (index % CUSTOM_FIELDS_EVERY_NTH == 0) "apiKey,token" else null,
    )

    data class DataSet(
        val resources: List<Resource>,
        val metadata: List<ResourceMetadata>,
        val uris: List<ResourceUri>,
        val tags: List<Tag>,
        val tagCrossRefs: List<ResourceAndTagsCrossRef>,
    )
}
