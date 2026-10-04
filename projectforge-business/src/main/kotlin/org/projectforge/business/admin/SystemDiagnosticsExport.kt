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

package org.projectforge.business.admin

/**
 * Provides the textual system/2FA diagnostics dump exported by the administration page ("Export 2FA configuration").
 *
 * The implementation ([org.projectforge.start.ProjectForgeEndpoints]) lives in `projectforge-application` because it
 * has to enumerate the REST endpoints, which are only known there. This interface is declared in
 * `projectforge-business` so the projectforge-next `SystemRest` can obtain the dump without depending on
 * `projectforge-application`.
 */
interface SystemDiagnosticsExport {
    /** The 2FA configuration, short cuts and endpoint list as plain text, for the "Export 2FA configuration" download. */
    fun getInfo(): String
}
