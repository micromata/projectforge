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

package org.projectforge.ui

data class LayoutContext(
        /**
         * Data class for auto-detecting JPA-property (@Column), PropertyInfo and property type.
         */
        val dataObjectClazz: Class<*>?,
        var idPrefix: String? = null) {
    private val log = org.slf4j.LoggerFactory.getLogger(ElementsRegistry::class.java)

    private val listElements = mutableMapOf<String, ElementInfo>()

    constructor(layoutContext: LayoutContext) : this(layoutContext.dataObjectClazz) {
        idPrefix = layoutContext.idPrefix
    }

    fun registerListElement(varName: String, idPath: String) {
        val elInfo = ElementsRegistry.getElementInfo(this, idPath)
        if (elInfo == null) {
            log.warn("Can't register list element '$idPath'. It won't be available under varname '$varName'")
        } else {
            listElements[varName] = elInfo
        }
    }

    fun getListElementInfo(varName: String): ElementInfo? {
        return listElements[varName]
    }
}
