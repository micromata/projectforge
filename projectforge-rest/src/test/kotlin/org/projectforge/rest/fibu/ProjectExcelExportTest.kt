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


package org.projectforge.rest.fibu

import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.KontoDO
import org.projectforge.business.fibu.KontoDao
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.fibu.ProjektStatus
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.MagicFilterEntry
import org.projectforge.ui.filter.Kost2FilterUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus

/**
 * The Excel export of the project list ([ProjectEntityRest.exportAsExcel]): all fields of a project, one row
 * per project the filter matches.
 */
class ProjectExcelExportTest : AbstractTestBase() {
    @Autowired
    private lateinit var projectEntityRest: ProjectEntityRest

    @Autowired
    private lateinit var projektDao: ProjektDao

    @Autowired
    private lateinit var kontoDao: KontoDao

    @Test
    fun `the export has all fields of the filtered projects`() {
        logon(TEST_FINANCE_USER)
        val contact = getUser(TEST_USER)
        val konto = KontoDO().also { konto ->
            konto.nummer = 12733
            konto.bezeichnung = "Project Excel export account"
            kontoDao.insert(konto, checkAccess = false)
        }
        val inserted = ProjektDO().also { project ->
            project.name = "Project Excel export"
            project.identifier = "PEE"
            project.internKost2_4 = 733
            project.nummer = 12
            project.status = ProjektStatus.BUILD
            project.description = "A description"
            project.contactPerson = contact
            project.konto = konto
            projektDao.insert(project, checkAccess = false)
        }

        val response = projectEntityRest.exportAsExcel(MagicFilter())

        assertEquals(HttpStatus.OK, response.statusCode)
        val disposition = response.headers[HttpHeaders.CONTENT_DISPOSITION]?.first()
        assertNotNull(disposition)
        assertTrue(disposition!!.contains("Projects") && disposition.endsWith(".xlsx"), disposition)
        val bytes = (response.body as Resource).inputStream.readBytes()
        val sheet = WorkbookFactory.create(bytes.inputStream()).getSheetAt(0)
        val headers = sheet.getRow(0).map { it.stringCellValue }
        assertEquals(22, headers.size)
        assertEquals(translate("fibu.projekt.name"), headers[COL_NAME])
        val row = rowOf(sheet, "Project Excel export")
        assertEquals("PEE", row.getCell(3).stringCellValue)
        // Numeric cells: the project number as assigned on insert (a free one without number ranges).
        val projektnummer = projektDao.find(inserted.id, checkAccess = false)!!.projektnummer!!
        assertEquals(projektnummer.toDouble(), row.getCell(1).numericCellValue)
        assertEquals(translate(ProjektStatus.BUILD.i18nKey), row.getCell(headers.indexOf(translate("status"))).stringCellValue)
        assertEquals(
            contact.getFullname(),
            row.getCell(headers.indexOf(translate("contactPerson"))).stringCellValue,
        )
        // Account number and designation in two columns.
        val kontoColumn = headers.indexOf(translate("fibu.konto.nummer"))
        assertEquals(12733.0, row.getCell(kontoColumn).numericCellValue)
        assertEquals(translate("fibu.konto"), headers[kontoColumn + 1])
        assertEquals("Project Excel export account", row.getCell(kontoColumn + 1).stringCellValue)
        assertEquals("A description", row.getCell(headers.indexOf(translate("description"))).stringCellValue)
    }

    @Test
    fun `an empty result answers 404`() {
        logon(TEST_FINANCE_USER)
        val filter = MagicFilter()
        // An active cost 2 type no project has: matches nothing (ProjectEntityRest.addKost2ArtCriterion).
        filter.entries.add(MagicFilterEntry(Kost2FilterUtils.kost2ArtFieldId()).also { it.value.values = arrayOf("98") })

        assertEquals(HttpStatus.NOT_FOUND, projectEntityRest.exportAsExcel(filter).statusCode)
    }

    private fun rowOf(sheet: Sheet, name: String) =
        (1..sheet.lastRowNum).mapNotNull { sheet.getRow(it) }
            .firstOrNull { it.getCell(COL_NAME)?.stringCellValue == name }
            .also { assertNotNull(it, "No row for $name in the export.") }!!

    companion object {
        private const val COL_NAME = 4
    }
}
