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

package org.projectforge.idp

import org.projectforge.idp.model.IdpGroup
import org.projectforge.idp.model.IdpUser

/**
 * Provider-neutral interface for identity provider admin operations.
 * Implementations exist for Keycloak and Authentik.
 *
 * The handlers ([org.projectforge.idp.handler.IdpLoginHandler],
 * [org.projectforge.idp.handler.IdpMasterLoginHandler]) call only
 * these methods, making them completely provider-agnostic.
 */
interface IdpAdminClient {

    /** Returns all users from the IdP, fetching all pages automatically. */
    fun getAllUsers(): List<IdpUser>

    /** Returns all top-level groups from the IdP, fetching all pages automatically. */
    fun getAllGroups(): List<IdpGroup>

    /** Returns the groups a specific user belongs to. */
    fun getUserGroups(userId: String): List<IdpGroup>

    /** Returns all members of a group. */
    fun getGroupMembers(groupId: String): List<IdpUser>

    /** Returns a single user by their IdP ID, or null if not found. */
    fun getUserById(userId: String): IdpUser?

    /** Finds a user by username. Returns null if not found. */
    fun findUserByUsername(username: String): IdpUser?

    /** Creates a user in the IdP and returns the newly assigned external ID. */
    fun createUser(user: IdpUser): String

    /** Updates an existing user in the IdP. */
    fun updateUser(userId: String, user: IdpUser)

    /** Creates a group in the IdP and returns the newly assigned external ID. */
    fun createGroup(group: IdpGroup): String

    /** Updates an existing group in the IdP. */
    fun updateGroup(groupId: String, group: IdpGroup)

    /** Returns a single group by ID including its attributes. */
    fun getGroup(groupId: String): IdpGroup

    /** Adds a user to a group. */
    fun addUserToGroup(userId: String, groupId: String)

    /** Removes a user from a group. */
    fun removeUserFromGroup(userId: String, groupId: String)

    /** Sets (resets) the password for a user. */
    fun resetPassword(userId: String, password: CharArray)

    /** Returns true if the IdP is fully configured and ready for use. */
    fun isConfigured(): Boolean

    /** Returns the display name of the provider (e.g. "Keycloak", "Authentik") for logging. */
    fun providerName(): String
}
