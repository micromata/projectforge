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
package org.projectforge.framework.persistence.database

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.task.TaskDO
import org.projectforge.framework.persistence.api.HibernateUtils.enterTestMode
import org.projectforge.framework.persistence.api.HibernateUtils.exitTestMode
import org.projectforge.framework.persistence.api.HibernateUtils.getPropertyLength
import org.projectforge.test.AbstractTestBase

class HibernateUtilsTest : AbstractTestBase() {
    @Test
    fun propertyLengthTest() {
        var length = getPropertyLength(TaskDO::class.java.name, "title")
        Assertions.assertEquals(40, length)
        length = getPropertyLength(TaskDO::class.java.name, "shortDescription")
        Assertions.assertEquals(255, length)
        length = getPropertyLength(TaskDO::class.java, "shortDescription")
        Assertions.assertEquals(255, length)
        enterTestMode()
        length = getPropertyLength(TaskDO::class.java.name, "unknown")
        exitTestMode()
        Assertions.assertNull(length)
        length = getPropertyLength("unknown", "name")
        Assertions.assertNull(length)
    }
}
