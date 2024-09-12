/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.persistence.jpa.candh

import org.projectforge.framework.persistence.history.PropertyOpType

internal class HistoryContext {
    private val entries = mutableListOf<Entry>()

    fun add(path: String, type: PropertyOpType, newValue: Any? = null, oldValue: Any? = null) {
        entries.add(Entry(path = path, type = type, newValue = newValue, oldValue = oldValue))
    }

    class Entry(val path: String, val type: PropertyOpType, val oldValue: Any?, val newValue: Any?) {
    }
}
