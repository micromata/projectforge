/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.keycloak.rest

import mu.KotlinLogging
import org.projectforge.business.group.service.GroupService
import org.projectforge.business.user.service.UserService
import org.projectforge.framework.access.AccessChecker
import org.projectforge.keycloak.client.KeycloakAdminClient
import org.projectforge.keycloak.converter.KeycloakGroupConverter
import org.projectforge.keycloak.converter.KeycloakUserConverter
import org.projectforge.rest.config.Rest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

/**
 * Admin-only REST endpoint for one-time migration of ProjectForge users and groups to Keycloak.
 *
 * Endpoint: POST /rs/admin/keycloak/migrateToKeycloak
 *
 * This is intended to be called once during initial setup to populate Keycloak
 * with the existing PF user base. Subsequent syncs are driven by Keycloak → PF.
 *
 * Behavior:
 * - Skips users/groups already present in Keycloak (deduplication by username/name)
 * - Skips localUser / localGroup / deleted entries
 * - Creates group memberships for all synced users
 * - Returns a [MigrationReport] with counts per category
 */
@RestController
@RequestMapping("${Rest.URL}/admin/keycloak")
open class KeycloakMigrationRest {

    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var groupService: GroupService

    @Autowired
    private lateinit var keycloakAdminClient: KeycloakAdminClient

    @Autowired
    private lateinit var keycloakUserConverter: KeycloakUserConverter

    @Autowired
    private lateinit var keycloakGroupConverter: KeycloakGroupConverter

    /**
     * Exports all eligible PF users and groups to Keycloak.
     * Admin access required.
     */
    @PostMapping("migrateToKeycloak")
    fun migrateToKeycloak(): ResponseEntity<MigrationReport> {
        accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
        log.info("Starting initial migration from ProjectForge to Keycloak...")

        val report = MigrationReport()

        // Pre-fetch existing Keycloak data to avoid duplicates
        val existingKcUsers = keycloakAdminClient.getAllUsers()
        val existingKcGroups = keycloakAdminClient.getAllGroups()
        val existingKcUsernames = existingKcUsers.mapNotNull { it.username }.toSet()
        val existingKcGroupNames = existingKcGroups.mapNotNull { it.name }.toSet()

        // Map username → Keycloak ID (for membership creation)
        val kcUserIdByUsername = existingKcUsers
            .filter { it.username != null && it.id != null }
            .associate { it.username!! to it.id!! }
            .toMutableMap()

        // Map group name → Keycloak ID
        val kcGroupIdByName = existingKcGroups
            .filter { it.name != null && it.id != null }
            .associate { it.name!! to it.id!! }
            .toMutableMap()

        // --- Migrate users ---
        val pfUsers = userService.selectAll(false)
        pfUsers.filter { !it.localUser && !it.deleted }.forEach { pfUser ->
            val username = pfUser.username ?: return@forEach
            try {
                if (existingKcUsernames.contains(username)) {
                    log.debug("Keycloak user '$username' already exists, skipping.")
                    report.usersSkipped++
                } else {
                    val kcUser = keycloakUserConverter.toKeycloakUser(pfUser)
                    val newId = keycloakAdminClient.createUser(kcUser)
                    kcUserIdByUsername[username] = newId
                    report.usersCreated++
                    log.debug("Created Keycloak user '$username'")
                }
            } catch (ex: Exception) {
                log.error("Failed to migrate user '$username' to Keycloak: ${ex.message}", ex)
                report.userErrors++
            }
        }

        // --- Migrate groups ---
        val pfGroups = groupService.getAllGroups()
        pfGroups.filter { !it.localGroup && !it.deleted }.forEach { group ->
            val groupName = group.name ?: return@forEach
            try {
                if (existingKcGroupNames.contains(groupName)) {
                    log.debug("Keycloak group '$groupName' already exists, skipping.")
                    report.groupsSkipped++
                } else {
                    val kcGroup = keycloakGroupConverter.toKeycloakGroup(group)
                    val newId = keycloakAdminClient.createGroup(kcGroup)
                    kcGroupIdByName[groupName] = newId
                    report.groupsCreated++
                    log.debug("Created Keycloak group '$groupName'")
                }
            } catch (ex: Exception) {
                log.error("Failed to migrate group '$groupName' to Keycloak: ${ex.message}", ex)
                report.groupErrors++
            }
        }

        // --- Migrate memberships ---
        pfGroups.filter { !it.localGroup && !it.deleted }.forEach { group ->
            val kcGroupId = kcGroupIdByName[group.name] ?: return@forEach
            group.assignedUsers?.forEach { user ->
                if (user.localUser || user.deleted) return@forEach
                val kcUserId = kcUserIdByUsername[user.username] ?: return@forEach
                try {
                    keycloakAdminClient.addUserToGroup(kcUserId, kcGroupId)
                    report.membershipsCreated++
                } catch (ex: Exception) {
                    log.error(
                        "Failed to add user '${user.username}' to group '${group.name}' in Keycloak: ${ex.message}",
                        ex
                    )
                    report.membershipErrors++
                }
            }
        }

        log.info(
            "Migration to Keycloak complete: " +
            "${report.usersCreated} users created, ${report.usersSkipped} skipped, ${report.userErrors} errors | " +
            "${report.groupsCreated} groups created, ${report.groupsSkipped} skipped, ${report.groupErrors} errors | " +
            "${report.membershipsCreated} memberships created, ${report.membershipErrors} errors"
        )
        return ResponseEntity.ok(report)
    }

    data class MigrationReport(
        var usersCreated: Int = 0,
        var usersSkipped: Int = 0,
        var userErrors: Int = 0,
        var groupsCreated: Int = 0,
        var groupsSkipped: Int = 0,
        var groupErrors: Int = 0,
        var membershipsCreated: Int = 0,
        var membershipErrors: Int = 0
    )
}
