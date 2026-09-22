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
 * A DTO whose hand built next page asks it what may be done with it: whether the form may be saved and
 * whether the entry may be marked as deleted.
 *
 * The same two answers [org.projectforge.ui.UILayout.UserAccess] carries for the layout driven frontends
 * - but that one travels with the *layout*, and a hand built page fetches `GET /rs/{entity}/{id}` and no
 * layout at all. So the flags ride on the entity, filled in one place
 * ([org.projectforge.rest.core.AbstractEntityRest.getById] via `checkUserAccess`) from the very same DAO
 * calls that fill the `UserAccess` - implement this interface and there is nothing to do per rest class.
 *
 * `null` means "not asked" and is read as allowed on the client (`lib/rs/entity-access.ts`), which is
 * what a list row gets: filling the flags costs DAO calls per row and no list offers a save button.
 *
 * A hint for the UI in every case, never an authorization - the DAO stays the authority and answers a
 * write it refuses with HTTP 406 (`AbstractPagesRestUtils.handleException`).
 *
 * Per-*field* rights are not this: they are specific to one entity and stay in its `transformFromDB`
 * (see [Auftrag.vollstaendigFakturiertWriteAccess], [Task.kost2AndBookingStatusWriteAccess]).
 */
interface EntityAccessSupport {
    /** Whether the logged-in user may save this entry, `null` if not asked. */
    var writeAccess: Boolean?

    /** Whether the logged-in user may mark this entry as deleted, `null` if not asked. */
    var deleteAccess: Boolean?
}
