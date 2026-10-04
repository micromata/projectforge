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

package org.projectforge.rest.orga

import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.projectforge.business.fibu.KontoDO
import org.projectforge.business.fibu.KontoDao
import org.projectforge.business.fibu.kost.BuchungssatzDO
import org.projectforge.business.fibu.kost.BuchungssatzDao
import org.projectforge.business.fibu.kost.Kost1DO
import org.projectforge.business.fibu.kost.Kost1Dao
import org.projectforge.business.fibu.kost.Kost2ArtDO
import org.projectforge.business.fibu.kost.Kost2ArtDao
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.Kost2Dao
import org.projectforge.business.fibu.kost.SHType
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.user.GroupDao
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.business.user.UserRightDao
import org.projectforge.business.user.UserRightId
import org.projectforge.business.user.UserRightValue
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.UserRightDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.LocalDate

/**
 * The Excel export of the accounting-record list ([AccountingRecordEntityRest.exportAsExcel]), ported from
 * Wicket's `AccountingRecordListPage`: the record's fields, one row per record of the filtered list, gated on
 * the DATEV-import right like the list itself.
 */
class AccountingRecordExcelExportTest : AbstractTestBase() {
    @Autowired
    private lateinit var accountingRecordEntityRest: AccountingRecordEntityRest

    @Autowired
    private lateinit var buchungssatzDao: BuchungssatzDao

    @Autowired
    private lateinit var kontoDao: KontoDao

    @Autowired
    private lateinit var kost1Dao: Kost1Dao

    @Autowired
    private lateinit var kost2Dao: Kost2Dao

    @Autowired
    private lateinit var kost2ArtDao: Kost2ArtDao

    @Autowired
    private lateinit var groupDao: GroupDao

    @Autowired
    private lateinit var userRightDao: UserRightDao

    @Test
    fun `the export has the record's fields and the amount as a General number`() {
        logon(TEST_ADMIN_USER)
        insertRecord()
        val user = financeUser("AccountingRecordExportDatev", UserRightValue.TRUE)
        logon(user)

        val response = accountingRecordEntityRest.exportAsExcel(MagicFilter())

        assertEquals(HttpStatus.OK, response.statusCode)
        val disposition = response.headers[HttpHeaders.CONTENT_DISPOSITION]?.first()
        assertTrue(disposition!!.contains("AccountingRecords") && disposition.endsWith(".xlsx"), disposition)
        val bytes = (response.body as Resource).inputStream.readBytes()
        val sheet = WorkbookFactory.create(bytes.inputStream()).getSheetAt(0)
        assertEquals(translate("fibu.buchungssaetze"), sheet.sheetName)
        assertEquals(
            listOf(
                "fibu.buchungssatz.satznr", "date", "fibu.common.betrag", "finance.accountingRecord.dc",
                "fibu.buchungssatz.konto", "fibu.buchungssatz.gegenKonto", "fibu.kost1", "fibu.kost2",
                "fibu.buchungssatz.beleg", "fibu.buchungssatz.text", "fibu.buchungssatz.menge", "comment",
            ).map { translate(it) },
            sheet.getRow(0).map { it.stringCellValue },
        )
        val row = rowOf(sheet, "2099-07-00042")
        assertEquals(CellType.NUMERIC, row.getCell(2).cellType)
        assertEquals(35.0, row.getCell(2).numericCellValue)
        assertEquals("General", row.getCell(2).cellStyle.dataFormatString)
        assertEquals(translate(SHType.HABEN.i18nKey), row.getCell(3).stringCellValue)
        assertEquals("87341 - Export account", row.getCell(4).stringCellValue)
        assertEquals("87342 - Export counter account", row.getCell(5).stringCellValue)
        assertEquals("4.733.02.05", row.getCell(6).stringCellValue)
        assertEquals("6.734.03.88", row.getCell(7).stringCellValue)
        assertEquals("B-87", row.getCell(8).stringCellValue)
        assertEquals("Export record", row.getCell(9).stringCellValue)
        assertEquals("A comment", row.getCell(11).stringCellValue)
    }

    @Test
    fun `the export is refused without the DATEV-import right`() {
        val user = financeUser("AccountingRecordExportNoDatev", UserRightValue.FALSE)
        logon(user)
        assertThrows<AccessException> { accountingRecordEntityRest.exportAsExcel(MagicFilter()) }
    }

    /** A member of the finance group (the DAO's select access) with the given DATEV-import right. */
    private fun financeUser(username: String, datevRight: UserRightValue): PFUserDO {
        logon(TEST_ADMIN_USER)
        val user = PFUserDO().also { it.username = username }
        val id = userService.insert(user, false)
        userRightDao.insert(UserRightDO(UserRightId.FIBU_DATEV_IMPORT, datevRight).also { it.user = user })
        val group = getGroup(ProjectForgeGroup.FINANCE_GROUP.toString())
        group.addUser(user)
        groupDao.update(group)
        return userService.find(id, false)!!
    }

    private fun insertRecord() {
        val konto = konto(87341, "Export account")
        val gegenKonto = konto(87342, "Export counter account")
        val kost1 = Kost1DO().also {
            it.nummernkreis = 4
            it.bereich = 733
            it.teilbereich = 2
            it.endziffer = 5
            kost1Dao.insert(it, checkAccess = false)
        }
        val art = Kost2ArtDO().also {
            it.id = 88
            it.name = "Export type"
            kost2ArtDao.insert(it, checkAccess = false)
        }
        val kost2 = Kost2DO().also {
            it.nummernkreis = 6 // 4 and 5 need a project.
            it.bereich = 734
            it.teilbereich = 3
            it.kost2Art = art
            kost2Dao.insert(it, checkAccess = false)
        }
        BuchungssatzDO().also {
            it.year = 2099
            it.month = 7
            it.satznr = 42
            it.betrag = BigDecimal("35.00")
            it.sh = SHType.HABEN
            it.konto = konto
            it.gegenKonto = gegenKonto
            it.kost1 = kost1
            it.kost2 = kost2
            it.datum = LocalDate.of(2099, 7, 15)
            it.beleg = "B-87"
            it.text = "Export record"
            it.comment = "A comment"
            buchungssatzDao.insert(it, checkAccess = false)
        }
    }

    private fun konto(nummer: Int, bezeichnung: String): KontoDO = KontoDO().also {
        it.nummer = nummer
        it.bezeichnung = bezeichnung
        kontoDao.insert(it, checkAccess = false)
    }

    /** The row whose first column is [satznr]. */
    private fun rowOf(sheet: Sheet, satznr: String) = (1..sheet.lastRowNum).mapNotNull { sheet.getRow(it) }
        .firstOrNull { it.getCell(0)?.stringCellValue == satznr }
        .also { assertNotNull(it, "No row for $satznr in the export.") }!!
}
