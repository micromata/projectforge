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

package org.projectforge.business.fibu.kost

import org.apache.poi.ss.usermodel.WorkbookFactory
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.fibu.RechnungDO
import org.projectforge.business.fibu.RechnungDao
import org.projectforge.business.fibu.RechnungStatus
import org.projectforge.business.fibu.RechnungsPositionDO
import org.projectforge.business.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month

class KostZuweisungExportTest : AbstractTestBase() {
    @Autowired
    private lateinit var kost1Dao: Kost1Dao

    @Autowired
    private lateinit var kost2ArtDao: Kost2ArtDao

    @Autowired
    private lateinit var kost2Dao: Kost2Dao

    @Autowired
    private lateinit var projektDao: ProjektDao

    @Autowired
    private lateinit var rechnungDao: RechnungDao

    @Autowired
    private lateinit var kostZuweisungExport: KostZuweisungExport

    /**
     * The cost assignments of a deleted position are no part of the export.
     *
     * They used to be a row of it like any other: a deleted position keeps its cost assignments (its collection
     * carries no `@SoftDeleteCollection`, so they travel back on every save), and `RechnungService.fetchPositionen`
     * rebuilt the positions from a projection that did not read `deleted` at all — so the export could not have
     * told the deleted position from a live one, and counted it into the cost list an accountant works with.
     */
    @Test
    fun `deleted positions are excluded from the export`() {
        logon(TEST_FINANCE_USER)
        val project = ProjektDO().also { project ->
            project.name = "Test Project for KostZuweisungExportTest"
            project.internKost2_4 = 100
            project.nummer = 78
            projektDao.insert(project, checkAccess = false)
        }
        val kost1 = createKost1()
        val kost2 = createKost2(project, createKost2Art(79))
        var invoice = RechnungDO().also { invoice ->
            invoice.nummer = rechnungDao.nextNumber
            invoice.kundeText = "Customer"
            // Issued, not planned: `exportRechnungen` exports the valid invoices only (RechnungDO.isValid).
            invoice.status = RechnungStatus.GESTELLT
            invoice.datum = LocalDate.of(2024, Month.DECEMBER, 25)
            invoice.faelligkeit = LocalDate.of(2025, Month.JANUARY, 31)
            invoice.addPosition(RechnungsPositionDO().also { pos ->
                pos.text = "Kept position"
                pos.menge = BigDecimal.ONE
                pos.einzelNetto = BigDecimal("100")
                pos.addKostZuweisung(KostZuweisungDO().also { assignment ->
                    assignment.kost1 = kost1
                    assignment.kost2 = kost2
                    assignment.netto = BigDecimal("100")
                })
            })
            invoice.addPosition(RechnungsPositionDO().also { pos ->
                pos.text = "Position to delete"
                pos.menge = BigDecimal.ONE
                pos.einzelNetto = BigDecimal("50")
                pos.addKostZuweisung(KostZuweisungDO().also { assignment ->
                    assignment.kost1 = kost1
                    assignment.kost2 = kost2
                    assignment.netto = BigDecimal("50")
                })
            })
        }
        rechnungDao.insert(invoice, checkAccess = false)

        invoice = rechnungDao.find(invoice.id, attached = true, checkAccess = false)!!
        invoice.positionen!![1].deleted = true
        rechnungDao.update(invoice, checkAccess = false)

        invoice = rechnungDao.find(invoice.id, checkAccess = false)!!
        Assertions.assertTrue(invoice.positionen!![1].deleted, "The position is stored as deleted.")
        val xls = kostZuweisungExport.exportRechnungen(listOf(invoice), "test")
        Assertions.assertNotNull(xls, "The export is created, deleted position or not.")

        // One heading row plus the row of the kept position, and nothing of the deleted one.
        WorkbookFactory.create(ByteArrayInputStream(xls!!)).use { workbook ->
            val sheet = workbook.getSheetAt(0)
            Assertions.assertEquals(1, sheet.lastRowNum, "Heading row and one position row expected.")
            Assertions.assertEquals(
                100.0,
                sheet.getRow(1).getCell(0).numericCellValue,
                0.001,
                "The gross amount of the kept position, the deleted one contributing nothing.",
            )
        }
    }

    private fun createKost1(): Kost1DO {
        return Kost1DO().also { kost ->
            kost.nummernkreis = 4
            kost.bereich = 100
            kost.teilbereich = 1
            kost.endziffer = 3
            kost1Dao.insert(kost, checkAccess = false)
        }
    }

    private fun createKost2(project: ProjektDO, kost2Art: Kost2ArtDO): Kost2DO {
        return Kost2DO().also { kost ->
            kost.projekt = project
            kost.nummernkreis = project.nummernkreis
            kost.bereich = project.bereich!!
            kost.teilbereich = project.nummer
            kost.kost2Art = kost2Art
            kost2Dao.insert(kost, checkAccess = false)
        }
    }

    private fun createKost2Art(nummer: Long): Kost2ArtDO {
        return Kost2ArtDO().also { art ->
            art.id = nummer
            art.name = "Test Kost2Art $nummer"
            kost2ArtDao.insert(art, checkAccess = false)
        }
    }
}
