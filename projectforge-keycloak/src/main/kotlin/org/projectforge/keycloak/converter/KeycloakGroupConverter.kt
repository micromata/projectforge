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

import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.keycloak.model.KeycloakGroup
import org.springframework.stereotype.Service

/**
 * Converts between Keycloak group representations and ProjectForge's [GroupDO].
 */
@Service
open class KeycloakGroupConverter {

    /**
     * Converts a Keycloak group to a [GroupDO].
     */
    fun toGroupDO(kcGroup: KeycloakGroup): GroupDO {
        val group = GroupDO()
        group.name = kcGroup.name
        return group
    }

    /**
     * Copies fields from a Keycloak group into an existing [GroupDO].
     * Returns true if any field was changed.
     */
    fun copyFields(source: KeycloakGroup, target: GroupDO): Boolean {
        var modified = false
        if (target.name != source.name) { target.name = source.name; modified = true }
        return modified
    }

    /**
     * Converts a [GroupDO] to a Keycloak group representation for the initial population.
     * Stores the PF group's ID in the 'pfId' attribute for cross-reference.
     */
    fun toKeycloakGroup(groupDO: GroupDO): KeycloakGroup {
        val attrs = groupDO.id?.let { mapOf("pfId" to listOf(it.toString())) }
        return KeycloakGroup(
            name = groupDO.name,
            attributes = attrs
        )
    }
}
