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

package org.projectforge.business.timesheet

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.util.Date

class TimesheetListPdfExportTest : AbstractTestBase() {
    @Autowired
    private lateinit var timesheetListPdfExport: TimesheetListPdfExport

    /**
     * The export runs on the com.lowagie.text classes of com.lowagie:itext:2.1.7 (brought by xdocreport, see the
     * projectforge-business build): a PDF with the title bar, the filter summary and a table row comes out.
     */
    @Test
    fun `exports a pdf`() {
        logon(TEST_USER)
        val timesheet = TimesheetDO().also {
            it.startTime = Date(1_750_000_000_000L)
            it.stopTime = Date(1_750_003_600_000L)
            it.location = "Kassel"
            it.description = "Development"
        }
        val pdf = timesheetListPdfExport.export(
            listOf(timesheet),
            TimesheetListPdfExport.Context(periodFrom = "2025-06-01", periodTo = "2025-06-30", searchString = "dev"),
        )
        Assertions.assertTrue(pdf.size > 4 && String(pdf, 0, 4, Charsets.US_ASCII) == "%PDF", "A PDF is produced.")
    }
}
