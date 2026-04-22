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

package org.projectforge.idp.converter

import mu.KotlinLogging
import org.projectforge.framework.persistence.user.entities.Gender
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.idp.IdpConfig
import org.projectforge.idp.model.IdpUser
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * Converts between provider-neutral [IdpUser] and ProjectForge's [PFUserDO].
 * Used by both IdpLoginHandler (IdP → PF) and IdpMasterLoginHandler (PF → IdP).
 */
@Service
open class IdpUserConverter(private val idpConfig: IdpConfig) {

    /**
     * Converts an IdP user to a new [PFUserDO].
     * Never sets password fields — passwords are handled separately.
     */
    fun toPFUser(idpUser: IdpUser): PFUserDO {
        val user = PFUserDO()
        user.username = idpUser.username
        user.firstname = idpUser.firstName
        user.lastname = idpUser.lastName
        user.email = idpUser.email
        user.deactivated = !idpUser.enabled
        user.idpExternalId = idpUser.id
        return user
    }

    /**
     * Copies fields from an IdP user into an existing [PFUserDO].
     * Applies configured attribute mappings (IdP attributes → PF fields) in addition to core fields.
     * Returns true if any field was changed.
     */
    fun copyFields(source: IdpUser, target: PFUserDO): Boolean {
        var modified = false
        if (target.firstname != source.firstName) { target.firstname = source.firstName; modified = true }
        if (target.lastname != source.lastName) { target.lastname = source.lastName; modified = true }
        val sourceEmail = source.email?.takeIf { it.isNotBlank() && it != "null" }
        if (target.email != sourceEmail) { target.email = sourceEmail; modified = true }
        val shouldBeDeactivated = !source.enabled
        if (target.deactivated != shouldBeDeactivated) { target.deactivated = shouldBeDeactivated; modified = true }
        if (source.id != null && target.idpExternalId != source.id) {
            log.debug { "User '${target.username}': idpExternalId '${target.idpExternalId}' → '${source.id}'" }
            target.idpExternalId = source.id; modified = true
        }

        // IdP attributes → PF fields (Phase 3: IdP is master)
        for ((pfField, idpAttr) in idpConfig.userAttributes) {
            val accessor = SUPPORTED_USER_FIELDS[pfField] ?: continue
            val idpValue = source.attributes?.get(idpAttr)?.firstOrNull()
            val pfValue = accessor.get(target)
            if (pfValue != idpValue) {
                accessor.set(target, idpValue)
                modified = true
            }
        }
        return modified
    }

    /**
     * Converts a [PFUserDO] to a provider-neutral [IdpUser].
     * Always includes the pfId attribute. Additionally maps any PF fields configured
     * via [IdpConfig.userAttributes] to IdP attributes (Phase 1/2: PF is master).
     */
    fun toIdpUser(pfUser: PFUserDO): IdpUser {
        val attrs = mutableMapOf<String, List<String>>()
        pfUser.id?.let { attrs["pfId"] = listOf(it.toString()) }

        for ((pfField, idpAttr) in idpConfig.userAttributes) {
            val accessor = SUPPORTED_USER_FIELDS[pfField]
            if (accessor == null) {
                log.warn("Unknown PF user field '$pfField' in userAttributes mapping, skipping.")
                continue
            }
            accessor.get(pfUser)?.takeIf { it.isNotBlank() }
                ?.let { attrs[idpAttr] = listOf(it) }
        }

        return IdpUser(
            username   = pfUser.username,
            firstName  = sanitizeName(pfUser.firstname),
            lastName   = sanitizeName(pfUser.lastname),
            email      = resolveEmail(pfUser),
            enabled    = pfUser.hasSystemAccess(),
            attributes = attrs.ifEmpty { null }
        )
    }

    /**
     * Sanitizes a person name: replaces characters not accepted by some IdPs'
     * name validation (e.g. parentheses as in "Reinhard (Admin)") with underscores.
     */
    private fun sanitizeName(name: String?): String? =
        name?.replace(Regex("[()\\[\\]{}<>]"), "_")

    /**
     * Returns the effective email.
     * Replaces the generic developer placeholder with a per-user address derived from
     * first/last name (or username as fallback).
     */
    private fun resolveEmail(pfUser: PFUserDO): String {
        val email = pfUser.email
        if (email == null || email.isBlank() || email == "null") return ""
        if (email != DEVELOPER_PLACEHOLDER_EMAIL) return email
        val first = pfUser.firstname?.trim()?.lowercase()
        val last = pfUser.lastname?.trim()?.lowercase()
        val raw = if (!first.isNullOrBlank() && !last.isNullOrBlank()) "$first.$last" else pfUser.username ?: return ""
        val localPart = raw.replace(Regex("[^a-z0-9._-]"), "").trimEnd('.')
        return if (localPart.isBlank()) "${pfUser.username ?: ""}@localhost" else "$localPart@localhost"
    }

    /** Bidirectional accessor for a single PF user field. */
    data class UserFieldAccessor(
        val get: (PFUserDO) -> String?,
        val set: (PFUserDO, String?) -> Unit
    )

    companion object {
        const val DEVELOPER_PLACEHOLDER_EMAIL = "m.developer@localhost"

        val SUPPORTED_USER_FIELDS: Map<String, UserFieldAccessor> = mapOf(
            "jiraUsername"             to UserFieldAccessor({ it.jiraUsernameOrUsername },   { u, v -> u.jiraUsername = v }),
            "mobilePhone"              to UserFieldAccessor({ it.mobilePhone },              { u, v -> u.mobilePhone = v }),
            "organization"             to UserFieldAccessor({ it.organization },             { u, v -> u.organization = v }),
            "description"              to UserFieldAccessor({ it.description },              { u, v -> u.description = v }),
            "gender"                   to UserFieldAccessor({ it.gender?.name },             { u, v -> u.gender = v?.let { runCatching { enumValueOf<Gender>(it) }.getOrNull() } }),
            "nickname"                 to UserFieldAccessor({ it.nickname },                 { u, v -> u.nickname = v }),
            "locale"                   to UserFieldAccessor({ it.locale?.language },          { u, v -> u.locale = v?.let { runCatching { java.util.Locale.forLanguageTag(it) }.getOrNull() } }),
            "timeZone"                 to UserFieldAccessor({ it.timeZoneString },           { u, v -> u.timeZoneString = v }),
            "personalPhoneIdentifiers" to UserFieldAccessor({ it.personalPhoneIdentifiers }, { u, v -> u.personalPhoneIdentifiers = v }),
        )
    }
}
