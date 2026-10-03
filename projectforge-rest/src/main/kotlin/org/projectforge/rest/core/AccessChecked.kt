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

package org.projectforge.rest.core

/**
 * Declares how a REST endpoint checks the access of the logged-in user, e.g. `"FIBU_ORGA_GROUPS"`,
 * `"own user only"`, `"DAO: select access"` or `"PUBLIC: token required"`.
 *
 * Purely documentary at runtime: nothing reads it, the check itself stays in the endpoint (or in the DAO it
 * calls). What it enforces is that the question has been asked: `RestEndpointAccessCheckTest`
 * (projectforge-application) fails for every endpoint mapping of a `@RestController` without it, apart from
 * a frozen baseline of legacy endpoints. Projectforge-next builds its own menu, so a hidden menu entry or a
 * Wicket page check no longer keeps anybody out of an endpoint.
 *
 * On a method it covers that method. On a class it covers only the endpoint methods *declared in that class*
 * (and their overrides) - not the methods a subclass adds, so a new endpoint of e.g. an [AbstractEntityRest]
 * subclass needs its own annotation.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class AccessChecked(val value: String)
