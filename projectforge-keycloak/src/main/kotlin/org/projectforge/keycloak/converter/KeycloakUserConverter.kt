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

package org.projectforge.keycloak.converter

import mu.KotlinLogging
import org.projectforge.framework.persistence.user.entities.Gender
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.keycloak.config.KeycloakConfig
import org.projectforge.keycloak.model.KeycloakUser
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * Converts between Keycloak user representations and ProjectForge's [PFUserDO].
 */
@Service
open class KeycloakUserConverter(private val keycloakConfig: KeycloakConfig) {

    /**
     * Converts a Keycloak user to a new [PFUserDO].
     * Never sets password fields — passwords are handled separately.
     */
    fun toPFUser(kcUser: KeycloakUser): PFUserDO {
        val user = PFUserDO()
        user.username = kcUser.username
        user.firstname = kcUser.firstName
        user.lastname = kcUser.lastName
        user.email = kcUser.email
        user.deactivated = !kcUser.enabled
        user.keycloakId = kcUser.id
        return user
    }

    /**
     * Copies fields from a Keycloak user into an existing [PFUserDO].
     * Applies configured attribute mappings (KC attributes → PF fields) in addition to core fields.
     * Returns true if any field was changed.
     */
    fun copyFields(source: KeycloakUser, target: PFUserDO): Boolean {
        var modified = false
        if (target.firstname != source.firstName) { target.firstname = source.firstName; modified = true }
        if (target.lastname != source.lastName) { target.lastname = source.lastName; modified = true }
        val sourceEmail = source.email?.takeIf { it.isNotBlank() && it != "null" }
        if (target.email != sourceEmail) { target.email = sourceEmail; modified = true }
        val shouldBeDeactivated = !source.enabled
        if (target.deactivated != shouldBeDeactivated) { target.deactivated = shouldBeDeactivated; modified = true }
        if (source.id != null && target.keycloakId != source.id) {
            log.debug { "User '${target.username}': keycloakId '${target.keycloakId}' → '${source.id}'" }
            target.keycloakId = source.id; modified = true
        }

        // Keycloak attributes → PF fields (Phase 3: KC is master)
        for ((pfField, kcAttr) in keycloakConfig.userAttributes) {
            val accessor = SUPPORTED_USER_FIELDS[pfField] ?: continue
            val kcValue = source.attributes?.get(kcAttr)?.firstOrNull()
            val pfValue = accessor.get(target)
            if (pfValue != kcValue) {
                accessor.set(target, kcValue)
                modified = true
            }
        }
        return modified
    }

    /**
     * Converts a [PFUserDO] to a Keycloak user representation.
     * Always includes the pfId attribute. Additionally maps any PF fields configured
     * via [KeycloakConfig.userAttributes] to Keycloak attributes (Phase 1/2: PF is master).
     */
    fun toKeycloakUser(pfUser: PFUserDO): KeycloakUser {
        val attrs = mutableMapOf<String, List<String>>()
        pfUser.id?.let { attrs["pfId"] = listOf(it.toString()) }

        for ((pfField, kcAttr) in keycloakConfig.userAttributes) {
            val accessor = SUPPORTED_USER_FIELDS[pfField]
            if (accessor == null) {
                log.warn("Unknown PF user field '$pfField' in userAttributes mapping, skipping.")
                continue
            }
            accessor.get(pfUser)?.takeIf { it.isNotBlank() }
                ?.let { attrs[kcAttr] = listOf(it) }
        }

        return KeycloakUser(
            username   = pfUser.username,
            firstName  = sanitizeName(pfUser.firstname),
            lastName   = sanitizeName(pfUser.lastname),
            email      = resolveEmail(pfUser),
            enabled    = pfUser.hasSystemAccess(),
            attributes = attrs.ifEmpty { null }
        )
    }

    /**
     * Returns the effective email for Keycloak.
     * Replaces the generic developer placeholder with a per-user address derived from
     * first/last name (or username as fallback).
     */
    /**
     * Sanitizes a person name for Keycloak: replaces characters not accepted by Keycloak's
     * name validation (e.g. parentheses as in "Reinhard (Admin)") with underscores.
     * Returns null if the input is null.
     */
    private fun sanitizeName(name: String?): String? =
        name?.replace(Regex("[()\\[\\]{}<>]"), "_")

    private fun resolveEmail(pfUser: PFUserDO): String {
        val email = pfUser.email
        // null and the literal string "null" both mean no email — send empty string to clear the field in Keycloak
        if (email == null || email.isBlank() || email == "null") return ""
        if (email != DEVELOPER_PLACEHOLDER_EMAIL) return email
        val first = pfUser.firstname?.trim()?.lowercase()
        val last = pfUser.lastname?.trim()?.lowercase()
        val raw = if (!first.isNullOrBlank() && !last.isNullOrBlank()) "$first.$last" else pfUser.username ?: return ""
        // Remove any character not valid in an email local part (letters, digits, dot, dash, underscore)
        val localPart = raw.replace(Regex("[^a-z0-9._-]"), "").trimEnd('.')
        return if (localPart.isBlank()) "${pfUser.username ?: ""}@localhost" else "$localPart@localhost"
    }

    /** Bidirectional accessor for a single PF user field. */
    data class UserFieldAccessor(
        val get: (PFUserDO) -> String?,
        val set: (PFUserDO, String?) -> Unit
    )

    companion object {
        /** Placeholder email used in development/test setups — replaced per user during KC sync. */
        const val DEVELOPER_PLACEHOLDER_EMAIL = "m.developer@localhost"

        /**
         * Supported PF user fields that can be mapped to Keycloak attributes via configuration.
         * Each entry provides a getter (PF → KC) and a setter (KC → PF) for bidirectional sync.
         */
        val SUPPORTED_USER_FIELDS: Map<String, UserFieldAccessor> = mapOf(
            "jiraUsername"             to UserFieldAccessor({ it.jiraUsername },             { u, v -> u.jiraUsername = v }),
            "mobilePhone"              to UserFieldAccessor({ it.mobilePhone },              { u, v -> u.mobilePhone = v }),
            "organization"             to UserFieldAccessor({ it.organization },             { u, v -> u.organization = v }),
            "description"              to UserFieldAccessor({ it.description },              { u, v -> u.description = v }),
            "gender"                   to UserFieldAccessor({ it.gender?.name },             { u, v -> u.gender = v?.let { runCatching { enumValueOf<Gender>(it) }.getOrNull() } }),
            "nickname"                 to UserFieldAccessor({ it.nickname },                 { u, v -> u.nickname = v }),
            "locale"                   to UserFieldAccessor({ it.locale?.toString() },       { u, v -> u.locale = v?.let { runCatching { java.util.Locale.forLanguageTag(it) }.getOrNull() } }),
            "timeZone"                 to UserFieldAccessor({ it.timeZoneString },           { u, v -> u.timeZoneString = v }),
            "personalPhoneIdentifiers" to UserFieldAccessor({ it.personalPhoneIdentifiers }, { u, v -> u.personalPhoneIdentifiers = v }),
        )
    }
}
