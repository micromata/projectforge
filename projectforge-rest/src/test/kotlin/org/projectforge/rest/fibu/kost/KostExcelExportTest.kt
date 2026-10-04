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

package org.projectforge.rest.fibu.kost

import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.fibu.kost.Kost1DO
import org.projectforge.business.fibu.kost.Kost1Dao
import org.projectforge.business.fibu.kost.Kost2ArtDO
import org.projectforge.business.fibu.kost.Kost2ArtDao
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.Kost2Dao
import org.projectforge.business.fibu.kost.KostentraegerStatus
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.MagicFilter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

/**
 * The Excel exports of the cost 1 and cost 2 lists ([Kost1EntityRest.exportAsExcel],
 * [Kost2EntityRest.exportAsExcel]), ported from Wicket's `Kost1ListPage`/`Kost2ListPage`: same columns in
 * the same order, one row per cost unit of the filtered list.
 */
class KostExcelExportTest : AbstractTestBase() {
    @Autowired
    private lateinit var kost1EntityRest: Kost1EntityRest

    @Autowired
    private lateinit var kost2EntityRest: Kost2EntityRest

    @Autowired
    private lateinit var kost1Dao: Kost1Dao

    @Autowired
    private lateinit var kost2Dao: Kost2Dao

    @Autowired
    private lateinit var kost2ArtDao: Kost2ArtDao

    @Autowired
    private lateinit var projektDao: ProjektDao

    @Test
    fun `the cost 1 export has Wicket's columns and the cost unit's values`() {
        logon(TEST_FINANCE_USER)
        Kost1DO().also { kost ->
            kost.nummernkreis = 4
            kost.bereich = 731
            kost.teilbereich = 2
            kost.endziffer = 5
            kost.description = "Kost1 Excel export"
            kost.kostentraegerStatus = KostentraegerStatus.ACTIVE
            kost1Dao.insert(kost, checkAccess = false)
        }

        val sheet = export(kost1EntityRest.exportAsExcel(MagicFilter()), "Kost1Export")

        assertEquals(
            listOf(translate("fibu.kost1"), translate("description"), translate("status")),
            headers(sheet),
        )
        assertEquals(
            listOf("4.731.02.05", "Kost1 Excel export", translate(KostentraegerStatus.ACTIVE.i18nKey)),
            rowOf(sheet, "4.731.02.05"),
        )
    }

    @Test
    fun `the cost 2 export has Wicket's columns, the type, the invoiced flag and the project`() {
        logon(TEST_FINANCE_USER)
        val project = ProjektDO().also { project ->
            project.name = "Kost2 Excel export project"
            project.internKost2_4 = 732
            project.nummer = 11
            projektDao.insert(project, checkAccess = false)
        }
        val art = Kost2ArtDO().also { art ->
            art.id = 87
            art.name = "Kost2 Excel export type"
            art.fakturiert = true
            kost2ArtDao.insert(art, checkAccess = false)
        }
        Kost2DO().also { kost ->
            kost.projekt = project
            kost.nummernkreis = project.nummernkreis
            kost.bereich = project.bereich!!
            kost.teilbereich = project.nummer
            kost.kost2Art = art
            kost.description = "Kost2 Excel export"
            kost.comment = "A comment"
            kost2Dao.insert(kost, checkAccess = false)
        }

        val sheet = export(kost2EntityRest.exportAsExcel(MagicFilter()), "Kost2Export")

        assertEquals(
            listOf(
                translate("fibu.kost2"), translate("fibu.kost2.art"), translate("fibu.fakturiert"),
                translate("fibu.projekt"), translate("status"), translate("description"), translate("comment"),
            ),
            headers(sheet),
        )
        val number = "4.732.11.87"
        val row = rowOf(sheet, number)
        assertEquals("Kost2 Excel export type", row[1])
        assertEquals("X", row[2])
        assertEquals("4.732.11 - Kost2 Excel export project", row[3], "No customer: number and project name.")
        assertEquals("Kost2 Excel export", row[5])
        assertEquals("A comment", row[6])
    }

    /** Asserts a downloaded .xlsx named after [namePart] and answers its first sheet. */
    private fun export(response: ResponseEntity<*>, namePart: String): Sheet {
        assertEquals(HttpStatus.OK, response.statusCode)
        val disposition = response.headers[HttpHeaders.CONTENT_DISPOSITION]?.first()
        assertNotNull(disposition)
        assertTrue(disposition!!.contains(namePart) && disposition.endsWith(".xlsx"), disposition)
        val bytes = (response.body as Resource).inputStream.readBytes()
        return WorkbookFactory.create(bytes.inputStream()).getSheetAt(0)
    }

    private fun headers(sheet: Sheet): List<String> = sheet.getRow(0).map { it.stringCellValue }

    /** The cells of the row whose first column is [number], as strings (empty for a blank cell). */
    private fun rowOf(sheet: Sheet, number: String): List<String> {
        val columns = sheet.getRow(0).lastCellNum.toInt()
        val row = (1..sheet.lastRowNum).mapNotNull { sheet.getRow(it) }
            .firstOrNull { it.getCell(0)?.stringCellValue == number }
        assertNotNull(row, "No row for $number in the export.")
        return (0 until columns).map { row!!.getCell(it)?.stringCellValue ?: "" }
    }
}
