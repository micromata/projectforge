/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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
package org.projectforge.framework.persistence.utils

import de.micromata.genome.util.strings.ShortDisplayable
import de.micromata.merlin.excel.importer.ImportedElement
import org.projectforge.framework.persistence.api.ShortDisplayNameCapable

open class MyImportedElement<T>(index: Int,
                           clazz: Class<T>,
                           vararg diffProperties: String)
    : ImportedElement<T>(index, clazz, *diffProperties) {

    override fun valueAsString(value: Any?): String? {
        if (value == null)
            return null
        return when (value) {
            is ShortDisplayable ->  value.toShortString()
            is ShortDisplayNameCapable -> value.shortDisplayName
            else -> value.toString()
        }
    }
}
