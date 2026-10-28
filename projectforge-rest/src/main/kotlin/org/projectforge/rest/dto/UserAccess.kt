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

package org.projectforge.rest.dto

/**
 * What the logged-in user may do with the entries of an entity, served with the list metadata
 * ([org.projectforge.rest.core.ListMetaData.userAccess]).
 *
 * A hint for the client which buttons and fields to offer, not authorization: every endpoint checks the
 * access itself (see [read] for the one flag a page acts on).
 */
class UserAccess(
    /**
     * The user has access to the object's history, if given.
     */
    var history: Boolean? = null,
    /**
     * The user has access to insert new objects.
     */
    var insert: Boolean? = null,
    var update: Boolean? = null,
    var delete: Boolean? = null,
    /**
     * Cancel button is visible for all users at default.
     */
    var cancel: Boolean? = true,
    /**
     * Is the edit user-comment button visible?
     * @see [org.projectforge.framework.persistence.api.BaseDao.supportsHistoryUserComments]
     */
    var editHistoryComments: Boolean? = null,
    /**
     * The user may see the entity's entries at all.
     *
     * Unlike the other flags this one is not merely a hint for the UI: false means the DAO will refuse
     * every read (`BaseDao.hasUserSelectAccess`), so there is nothing a page could show. projectforge-next
     * uses it to keep the whole page from rendering (see `useReadAccessGuard`); the authorization itself
     * stays where it was, in the DAO.
     *
     * Last in the parameter list because the leading ones are passed positionally in places
     * (`UserAccess(true, true, true, true)`).
     */
    var read: Boolean? = null,
) {
    fun copyFrom(userAccess: UserAccess?) {
        this.history = userAccess?.history
        this.insert = userAccess?.insert
        this.update = userAccess?.update
        this.delete = userAccess?.delete
        this.cancel = userAccess?.cancel ?: true
        this.editHistoryComments = userAccess?.editHistoryComments
        this.read = userAccess?.read
    }

    fun onlySelectAccess(): Boolean {
        return (insert != true && update != true && delete != true)
    }
}
