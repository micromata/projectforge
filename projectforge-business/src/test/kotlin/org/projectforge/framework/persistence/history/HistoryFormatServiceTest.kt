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

package org.projectforge.framework.persistence.history

import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.RechnungDO
import org.projectforge.business.fibu.RechnungDao
import org.projectforge.business.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired

class HistoryFormatServiceTest : AbstractTestBase() {
    //@Autowired
    //private lateinit var historyFormatService: HistoryFormatService
    @Autowired
    private lateinit var rechnungDao: RechnungDao

    @Test
    fun testOldInvoiceHistory() {
        HistoryServiceOldFormatTest.ensureSetup(this, persistenceService, historyService)
        val invoice = RechnungDO()
        invoice.id = 351958
        historyService.loadHistory(invoice, rechnungDao)
        // historyFormatService.loadHistory(invoice)
    }
}
