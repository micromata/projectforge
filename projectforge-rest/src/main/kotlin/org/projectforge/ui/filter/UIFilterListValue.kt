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

package org.projectforge.ui.filter

/**
 * One value of a [UIFilterListElement] whose values are loaded on demand ([UIFilterListElement.valuesUrl]).
 *
 * @param id The key sent back in `MagicFilterEntry.value.values` when the value is selected.
 * @param freeText True for a value typed as free text rather than picked from an entity (e.g. an order's
 *   `kundeText`), so the frontend can mark it as such.
 * @param group True for a value standing for several others (a customer group, a business unit), marked as such
 *   by the frontend.
 */
class UIFilterListValue(
    val id: String,
    val displayName: String,
    val freeText: Boolean? = null,
    val group: Boolean? = null,
)
