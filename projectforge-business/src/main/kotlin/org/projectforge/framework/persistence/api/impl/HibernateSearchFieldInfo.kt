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

package org.projectforge.framework.persistence.api.impl

internal class HibernateSearchFieldInfo(val field: String, val type: Class<*>) {
    private var annotations: MutableList<Annotation>? = null

    fun add(annotation: Annotation?) {
        if (annotation == null) {
            return
        }
        if (annotations == null) {
            annotations = mutableListOf()
        }
        annotations!!.add(annotation)
    }

    fun hasAnnotations(): Boolean {
        return !annotations.isNullOrEmpty()
    }

    fun isStringSearchSupported(): Boolean {
        return String::class.java.isAssignableFrom(type)
    }

    fun isNumericSearchSupported(): Boolean {
        return Integer::class.java.isAssignableFrom(type)
                || Int::class.java.isAssignableFrom(type)
    }
}
