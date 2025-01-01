/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.persistence.jpa

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.book.BookDO
import org.projectforge.business.task.TaskDO
import org.projectforge.business.task.TaskDO.Companion.TITLE_LENGTH

class EntityMetaDataTest {
    @Test
    fun entityMetaDataTest() {
        EntityMetaDataRegistry.getColumnMetaData(TaskDO::class.java, "title").let { columnMetaData ->
            Assertions.assertEquals("title", columnMetaData!!.name)
            Assertions.assertEquals(TITLE_LENGTH, columnMetaData.length)
            Assertions.assertFalse(columnMetaData.nullable)
        }
        EntityMetaDataRegistry.getEntityMetaData(BookDO::class.java).let { entityMetaData ->
            entityMetaData!!.getColumnMetaData("attachmentsNames").let { columnMetaData ->
                Assertions.assertEquals("attachments_names", columnMetaData!!.name)
                Assertions.assertEquals(10000, columnMetaData.length)
                Assertions.assertTrue(columnMetaData.nullable)
            }
        }
    }
}
