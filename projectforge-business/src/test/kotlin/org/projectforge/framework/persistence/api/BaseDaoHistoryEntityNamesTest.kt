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

package org.projectforge.framework.persistence.api

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.book.BookDO
import org.projectforge.business.book.BookDao
import org.projectforge.business.fibu.AuftragDO
import org.projectforge.business.fibu.AuftragDao
import org.projectforge.business.fibu.AuftragsPositionDO
import org.projectforge.business.fibu.PaymentScheduleDO

/**
 * [BaseDao.historyEntityNames] is what restricts the change history of a partial re-index to a list page
 * (DatabaseDao.createMassIndexer). A DAO forgetting its children loses their history until the next nightly run,
 * which is invisible at runtime, so it is asserted here. No Spring context needed: the names are static.
 */
class BaseDaoHistoryEntityNamesTest {
    @Test
    fun `entity with children names them all, its own class included`() {
        val names = AuftragDao().historyEntityNames
        Assertions.assertEquals(
            listOf(AuftragDO::class.java.name, AuftragsPositionDO::class.java.name, PaymentScheduleDO::class.java.name),
            names,
        )
    }

    @Test
    fun `entity without children names only itself`() {
        Assertions.assertEquals(listOf(BookDO::class.java.name), BookDao().historyEntityNames)
    }
}
