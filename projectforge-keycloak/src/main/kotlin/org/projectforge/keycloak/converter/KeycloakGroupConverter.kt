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
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.keycloak.config.KeycloakConfig
import org.projectforge.keycloak.model.KeycloakGroup
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * Converts between Keycloak group representations and ProjectForge's [GroupDO].
 */
@Service
open class KeycloakGroupConverter(private val keycloakConfig: KeycloakConfig) {

    /**
     * Converts a Keycloak group to a new [GroupDO].
     */
    fun toGroupDO(kcGroup: KeycloakGroup): GroupDO {
        val group = GroupDO()
        group.name = kcGroup.name
        return group
    }

    /**
     * Copies fields from a Keycloak group into an existing [GroupDO].
     * Applies configured attribute mappings (KC attributes → PF fields) in addition to the name.
     * Returns true if any field was changed.
     */
    fun copyFields(source: KeycloakGroup, target: GroupDO): Boolean {
        var modified = false
        if (target.name != source.name) { target.name = source.name; modified = true }

        // Keycloak attributes → PF fields (Phase 3: KC is master)
        for ((pfField, kcAttr) in keycloakConfig.groupAttributes) {
            val accessor = SUPPORTED_GROUP_FIELDS[pfField] ?: continue
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
     * Converts a [GroupDO] to a Keycloak group representation.
     * Always includes the pfId attribute. Additionally maps any PF fields configured
     * via [KeycloakConfig.groupAttributes] to Keycloak attributes (Phase 1/2: PF is master).
     */
    fun toKeycloakGroup(groupDO: GroupDO): KeycloakGroup {
        val attrs = mutableMapOf<String, List<String>>()
        groupDO.id?.let { attrs["pfId"] = listOf(it.toString()) }

        for ((pfField, kcAttr) in keycloakConfig.groupAttributes) {
            val accessor = SUPPORTED_GROUP_FIELDS[pfField]
            if (accessor == null) {
                log.warn("Unknown PF group field '$pfField' in groupAttributes mapping, skipping.")
                continue
            }
            accessor.get(groupDO)?.takeIf { it.isNotBlank() }
                ?.let { attrs[kcAttr] = listOf(it) }
        }

        return KeycloakGroup(
            name       = groupDO.name,
            attributes = attrs.ifEmpty { null }
        )
    }

    /** Bidirectional accessor for a single PF group field. */
    data class GroupFieldAccessor(
        val get: (GroupDO) -> String?,
        val set: (GroupDO, String?) -> Unit
    )

    companion object {
        /**
         * Supported PF group fields that can be mapped to Keycloak attributes via configuration.
         * Each entry provides a getter (PF → KC) and a setter (KC → PF) for bidirectional sync.
         */
        val SUPPORTED_GROUP_FIELDS: Map<String, GroupFieldAccessor> = mapOf(
            "description"  to GroupFieldAccessor({ it.description },  { g, v -> g.description = v }),
            "organization" to GroupFieldAccessor({ it.organization }, { g, v -> g.organization = v }),
        )
    }
}
