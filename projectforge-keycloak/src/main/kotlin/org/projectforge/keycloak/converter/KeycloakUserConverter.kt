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

import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.keycloak.model.KeycloakUser
import org.springframework.stereotype.Service

/**
 * Converts between Keycloak user representations and ProjectForge's [PFUserDO].
 */
@Service
open class KeycloakUserConverter {

    /**
     * Converts a Keycloak user to a [PFUserDO].
     * Never sets password fields — passwords are handled separately.
     */
    fun toPFUser(kcUser: KeycloakUser): PFUserDO {
        val user = PFUserDO()
        user.username = kcUser.username
        user.firstname = kcUser.firstName
        user.lastname = kcUser.lastName
        user.email = kcUser.email
        user.deactivated = !kcUser.enabled
        return user
    }

    /**
     * Copies fields from a Keycloak user into an existing [PFUserDO].
     * Returns true if any field was changed.
     */
    fun copyFields(source: KeycloakUser, target: PFUserDO): Boolean {
        var modified = false
        if (target.firstname != source.firstName) { target.firstname = source.firstName; modified = true }
        if (target.lastname != source.lastName) { target.lastname = source.lastName; modified = true }
        if (target.email != source.email) { target.email = source.email; modified = true }
        val shouldBeDeactivated = !source.enabled
        if (target.deactivated != shouldBeDeactivated) { target.deactivated = shouldBeDeactivated; modified = true }
        return modified
    }

    /**
     * Converts a [PFUserDO] to a Keycloak user representation for the initial population.
     * Stores the PF user's ID in the 'pfId' attribute for cross-reference.
     */
    fun toKeycloakUser(pfUser: PFUserDO): KeycloakUser {
        val attrs = pfUser.id?.let { mapOf("pfId" to listOf(it.toString())) }
        return KeycloakUser(
            username = pfUser.username,
            firstName = pfUser.firstname,
            lastName = pfUser.lastname,
            email = pfUser.email,
            enabled = pfUser.hasSystemAccess(),
            attributes = attrs
        )
    }
}
