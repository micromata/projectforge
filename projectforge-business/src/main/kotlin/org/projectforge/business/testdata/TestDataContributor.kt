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


package org.projectforge.business.testdata

/**
 * Fills one area of a fresh test system (setup option "with test data"), see [TestDataSeeder].
 *
 * Contributors run in ascending [order], each in a transaction of its own: a failing area is rolled back
 * and logged, the setup goes on. A contributor of a plugin names it in [pluginId] and runs only if the
 * plugin is active.
 */
interface TestDataContributor {
    val order: Int

    /** The share of the total run time, for the progress of the setup (most contributors take well below a second). */
    val weight: Int
        get() = 1

    /** The id of the plugin this contributor belongs to, null for the core. */
    val pluginId: String?
        get() = null

    fun seed(ctx: TestDataContext)
}
