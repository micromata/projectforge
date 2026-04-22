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
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.idp.IdpConfig
import org.projectforge.idp.model.IdpGroup
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * Converts between provider-neutral [IdpGroup] and ProjectForge's [GroupDO].
 * Used by both IdpLoginHandler (IdP → PF) and IdpMasterLoginHandler (PF → IdP).
 */
@Service
open class IdpGroupConverter(private val idpConfig: IdpConfig) {

    /**
     * Converts an IdP group to a new [GroupDO].
     */
    fun toGroupDO(idpGroup: IdpGroup): GroupDO {
        val group = GroupDO()
        group.name = idpGroup.name
        return group
    }

    /**
     * Copies fields from an IdP group into an existing [GroupDO].
     * Applies configured attribute mappings (IdP attributes → PF fields) in addition to the name.
     * Returns true if any field was changed.
     */
    fun copyFields(source: IdpGroup, target: GroupDO): Boolean {
        var modified = false
        if (target.name != source.name) { target.name = source.name; modified = true }

        for ((pfField, idpAttr) in idpConfig.groupAttributes) {
            val accessor = SUPPORTED_GROUP_FIELDS[pfField] ?: continue
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
     * Converts a [GroupDO] to a provider-neutral [IdpGroup].
     * Always includes the pfId attribute. Additionally maps any PF fields configured
     * via [IdpConfig.groupAttributes] to IdP attributes (Phase 1/2: PF is master).
     */
    fun toIdpGroup(groupDO: GroupDO): IdpGroup {
        val attrs = mutableMapOf<String, List<String>>()
        groupDO.id?.let { attrs["pfId"] = listOf(it.toString()) }

        for ((pfField, idpAttr) in idpConfig.groupAttributes) {
            val accessor = SUPPORTED_GROUP_FIELDS[pfField]
            if (accessor == null) {
                log.warn("Unknown PF group field '$pfField' in groupAttributes mapping, skipping.")
                continue
            }
            accessor.get(groupDO)?.takeIf { it.isNotBlank() }
                ?.let { attrs[idpAttr] = listOf(it) }
        }

        return IdpGroup(
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
        val SUPPORTED_GROUP_FIELDS: Map<String, GroupFieldAccessor> = mapOf(
            "description"  to GroupFieldAccessor({ it.description },  { g, v -> g.description = v }),
            "organization" to GroupFieldAccessor({ it.organization }, { g, v -> g.organization = v }),
        )
    }
}
