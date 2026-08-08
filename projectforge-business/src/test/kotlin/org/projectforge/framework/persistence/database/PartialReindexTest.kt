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

package org.projectforge.framework.persistence.database

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.book.BookDO
import org.projectforge.business.fibu.AuftragDO
import org.projectforge.business.fibu.AuftragsPositionDO
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.persistence.api.ReindexSettings
import org.projectforge.framework.persistence.history.HistoryEntryDO
import org.springframework.beans.factory.annotation.Autowired
import java.util.Date

/**
 * The partial re-index restricts the change history by an HQL `entityName in :entityNames`, which Hibernate Search
 * only parses when the indexer actually runs — a wrong parameter binding wouldn't show up at compile time. So the
 * run is done here for real, once with several names and once with a single one.
 */
class PartialReindexTest : AbstractTestBase() {
    @Autowired
    private lateinit var databaseDao: DatabaseDao

    @Test
    fun `partial re-index of the history with several entity names`() {
        val fromDate = Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L)
        // The names don't have to match any row: this asserts the condition is valid HQL and binds, not what it finds.
        reindexHistory(fromDate, listOf(AuftragDO::class.java.name, AuftragsPositionDO::class.java.name))
        reindexHistory(fromDate, listOf(BookDO::class.java.name))
        // Null means no restriction at all (a system wide run, e. g. CronNightlyJob).
        reindexHistory(fromDate, null)
    }

    private fun reindexHistory(fromDate: Date, entityNames: Collection<String>?) {
        val result = databaseDao.rebuildDatabaseSearchIndices(
            HistoryEntryDO::class.java,
            ReindexSettings(fromDate, 1000, entityNames),
        )
        // rebuildDatabaseSearchIndices answers with the class names it processed, or a refusal if a run is going on.
        Assertions.assertTrue(
            result.contains("HistoryEntryDO"),
            "Re-index of entityNames=$entityNames didn't run: $result",
        )
    }
}
