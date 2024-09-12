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

import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.api.HibernateUtils
import java.lang.reflect.Field

/**
 * Used for objects of type BaseDO.
 */
class BaseDOHandler : DefaultHandler() {
    override fun accept(field: Field): Boolean {
        return BaseDO::class.java.isAssignableFrom(field.type)
    }

    override fun fieldValuesEqual(srcFieldValue: Any, destFieldValue: Any): Boolean {
        val srcFieldValueId = HibernateUtils.getIdentifier(srcFieldValue)
        val destFieldValueId = HibernateUtils.getIdentifier(destFieldValue)
        return srcFieldValueId == destFieldValueId
    }
}
