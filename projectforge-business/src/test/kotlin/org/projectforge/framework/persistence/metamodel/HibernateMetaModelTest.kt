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

package org.projectforge.framework.persistence.metamodel

import jakarta.persistence.Column
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.book.BookDO
import org.projectforge.framework.persistence.history.NoHistory
import org.projectforge.business.test.AbstractTestBase

class HibernateMetaModelTest : AbstractTestBase() {
    @Test
    fun annotationTest() {
        HibernateMetaModel.getEntityInfo(BookDO::class.java).let { entityInfo ->
            requireNotNull(entityInfo)
            entityInfo.getPropertiesWithAnnotation(Column::class).let { list ->
                Assertions.assertEquals(22, list.size)
                list.find {  it.propertyName == "authors" }.let { propInfo ->
                    requireNotNull(propInfo)
                    Assertions.assertNotNull(propInfo.getAnnotation(Column::class))
                    Assertions.assertNull(propInfo.getAnnotation(NoHistory::class))
                }
            }
            entityInfo.getPropertyInfo("attachmentsNames").let { propInfo ->
                requireNotNull(propInfo)
                Assertions.assertNotNull(propInfo.getAnnotation(NoHistory::class))
            }
        }
    }
}
