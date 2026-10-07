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

import de.micromata.merlin.excel.ExcelWorkbook
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.fibu.ProjektStatus
import org.projectforge.business.fibu.kost.Kost2ArtDO
import org.projectforge.business.fibu.kost.Kost2ArtDao
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.Kost2Dao
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.rest.multiselect.MassUpdateContext
import org.projectforge.rest.multiselect.MassUpdateParameter
import org.projectforge.rest.multiselect.MultiSelectionExcelExport
import org.springframework.beans.factory.annotation.Autowired
import java.io.ByteArrayInputStream

/**
 * A project mass update of the cost 2 types: the types aren't a property of the project, so their old and
 * new state has to be recorded explicitly, or the Excel protocol lacks them.
 */
class ProjectKost2ArtsMassUpdateTest : AbstractTestBase() {
    @Autowired
    private lateinit var projectMultiSelectedPageRest: ProjectMultiSelectedPageRest

    @Autowired
    private lateinit var projektDao: ProjektDao

    @Autowired
    private lateinit var kost2ArtDao: Kost2ArtDao

    @Autowired
    private lateinit var kost2Dao: Kost2Dao

    @Test
    fun `activated cost 2 types are part of the Excel protocol`() {
        logon(TEST_FINANCE_USER)
        val arts = listOf(91L, 92L).map { id ->
            Kost2ArtDO().also {
                it.id = id
                it.name = "Kost2Art $id of ProjectKost2ArtsMassUpdateTest"
                kost2ArtDao.insert(it, checkAccess = false)
            }
        }
        val project = ProjektDO().also {
            it.name = "Project of ProjectKost2ArtsMassUpdateTest"
            it.internKost2_4 = 103
            it.nummer = 91
            projektDao.insert(it, checkAccess = false)
        }
        Kost2DO().also {
            it.projekt = project
            it.nummernkreis = project.nummernkreis
            it.bereich = project.bereich!!
            it.teilbereich = project.nummer
            it.kost2Art = arts[0]
            kost2Dao.insert(it, checkAccess = false)
        }
        val params = mutableMapOf(
            "kost2Arts" to MassUpdateParameter("kost2Arts").also {
                it.textValue = "92"
                it.append = true
            },
        )
        val context = object : MassUpdateContext<ProjektDO>(params) {
            override fun getId(obj: ProjektDO): Long = obj.id!!
        }
        val request = Mockito.mock(HttpServletRequest::class.java)
        assertNull(projectMultiSelectedPageRest.massUpdate(request, listOf(project.id!!), context))
        assertEquals(1, context.modifiedCounter, "errors: ${context.errorMessages.map { it.message }}")

        val excel = MultiSelectionExcelExport.export(context, projectMultiSelectedPageRest)
        ExcelWorkbook(ByteArrayInputStream(excel), "protocol.xlsx").use { workbook ->
            val sheet = workbook.getSheet(0)!!.poiSheet
            val row = sheet.getRow(2)
            assertEquals("91", row.getCell(1)?.stringCellValue, "old value")
            assertEquals("91, 92", row.getCell(2)?.stringCellValue, "new value")
        }
    }

    @Test
    fun `an ended project is an error, not silently unchanged`() {
        logon(TEST_FINANCE_USER)
        Kost2ArtDO().also {
            it.id = 93
            it.name = "Kost2Art 93 of ProjectKost2ArtsMassUpdateTest"
            kost2ArtDao.insert(it, checkAccess = false)
        }
        val project = ProjektDO().also {
            it.name = "Ended project of ProjectKost2ArtsMassUpdateTest"
            it.internKost2_4 = 103
            it.nummer = 92
            it.status = ProjektStatus.ENDED
            projektDao.insert(it, checkAccess = false)
        }
        val params = mutableMapOf(
            "kost2Arts" to MassUpdateParameter("kost2Arts").also {
                it.textValue = "93"
                it.append = true
            },
        )
        val context = object : MassUpdateContext<ProjektDO>(params) {
            override fun getId(obj: ProjektDO): Long = obj.id!!
        }
        val request = Mockito.mock(HttpServletRequest::class.java)
        assertNull(projectMultiSelectedPageRest.massUpdate(request, listOf(project.id!!), context))
        assertEquals(0, context.modifiedCounter)
        assertEquals(1, context.errorCounter)
        assertEquals(project.displayName, context.errorMessages.first().identifier)
    }
}
