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

package org.projectforge.business.fibu.datev

import de.micromata.merlin.excel.ExcelWorkbook
import de.micromata.merlin.excel.importer.ImportLogger
import de.micromata.merlin.excel.importer.ImportStatus
import de.micromata.merlin.excel.importer.ImportStorage
import de.micromata.merlin.excel.importer.ImportedSheet
import org.projectforge.business.fibu.KontoDO
import org.projectforge.business.fibu.KontoDao
import org.projectforge.business.fibu.kost.BuchungssatzDO
import org.projectforge.business.fibu.kost.BuchungssatzDao
import org.projectforge.business.fibu.kost.Kost1Dao
import org.projectforge.business.fibu.kost.Kost2Dao
import org.projectforge.business.user.UserRightId
import org.projectforge.business.user.UserRightValue
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.persistence.jpa.PfPersistenceContext
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.locale
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.InputStream

@Service
class DatevImportDao {
    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var kontoDao: KontoDao

    @Autowired
    private lateinit var kost1Dao: Kost1Dao

    @Autowired
    private lateinit var kost2Dao: Kost2Dao

    @Autowired
    private lateinit var buchungssatzDao: BuchungssatzDao

    /**
     * Liest den Kontenplan aus dem InputStream (Exceltabelle) und schreibt die gelesenen Werte des Kontenplans in
     * ImportStorge. Der User muss der FINANCE_GROUP angehören, um diese Funktionalität ausführen zu können.
     *
     * @param inputStream
     * @param filename
     * @return ImportStorage mit den gelesenen Daten.
     * @throws Exception
     */
    @Throws(Exception::class)
    fun importKontenplan(inputStream: InputStream, filename: String): ImportStorage<KontoDO> {
        checkLoggeinUserRight(accessChecker)
        log.info("importKontenplan called")
        ExcelWorkbook(inputStream, filename, locale!!).use { workbook ->
            val storage = ImportStorage<KontoDO>(
                Type.KONTENPLAN, workbook, ImportLogger.Level.INFO,
                "'$filename':", log
            )
            val imp = KontenplanExcelImporter()
            imp.doImport(storage, workbook)
            return storage
        }
    }

    /**
     * Liest die Buchungsdaten aus dem InputStream (Exceltabelle) und schreibt die gelesenen Werte in ImportStorge. Der
     * User muss der FINANCE_GROUP angehören, um diese Funktionalität ausführen zu können.
     *
     * @param is
     * @param filename
     * @return ImportStorage mit den gelesenen Daten.
     * @throws Exception
     */
    @Throws(Exception::class)
    fun importBuchungsdaten(`is`: InputStream, filename: String): ImportStorage<BuchungssatzDO> {
        checkLoggeinUserRight(accessChecker)
        log.info("importBuchungsdaten called.")
        ExcelWorkbook(`is`, filename, locale!!).use { workbook ->
            val storage = ImportStorage<BuchungssatzDO>(
                Type.BUCHUNGSSAETZE, workbook, ImportLogger.Level.INFO,
                "'$filename':", log
            )
            val imp = BuchungssatzExcelImporter(storage, kontoDao, kost1Dao, kost2Dao, persistenceService)
            imp.doImport(workbook)
            return storage
        }
    }

    /**
     * Der ImportStorage wird verprobt, dass heißt ein Schreiben der importierten Werte in die Datenbank wird getestet.
     * Ergebnis sind mögliche Fehler und Statistiken, welche Werte neu geschrieben und welche geändert werden. Der User
     * muss der FINANCE_GROUP angehören, um diese Funktionalität ausführen zu können.
     *
     * @param storage
     * @param sheetName of sheet to reconcile.
     */
    fun reconcile(storage: ImportStorage<*>, sheetName: String) {
        checkLoggeinUserRight(accessChecker)
        requireNotNull(storage.getSheets())
        val sheet = storage.getNamedSheet(sheetName)
        requireNotNull(sheet)
        persistenceService.runReadOnly { context ->
            if (storage.id === Type.KONTENPLAN) {
                reconcileKontenplan(sheet as ImportedSheet<KontoDO>, context)
            } else {
                reconcileBuchungsdaten(sheet as ImportedSheet<BuchungssatzDO>, context)
            }
        }
        sheet.numberOfCommittedElements = -1
    }

    fun commit(storage: ImportStorage<*>, sheetName: String) {
        checkLoggeinUserRight(accessChecker)
        requireNotNull(storage.getSheets())
        val sheet = storage.getNamedSheet(sheetName)
        requireNotNull(sheet)
        if (sheet.getStatus() != ImportStatus.RECONCILED) {
            throw UserException("common.import.action.commit.error.notReconciled")
        }
        var no = 0
        no = if (storage.id === Type.KONTENPLAN) {
            commitKontenplan(sheet as ImportedSheet<KontoDO?>)
        } else {
            commitBuchungsdaten(sheet as ImportedSheet<BuchungssatzDO?>)
        }
        sheet.numberOfCommittedElements = no
        sheet.setStatus(ImportStatus.IMPORTED)
    }

    private fun reconcileKontenplan(sheet: ImportedSheet<KontoDO>, context: PfPersistenceContext) {
        log.info("Reconcile Kontenplan called")
        sheet.getElements()?.forEach { el ->
            val konto = el.value
            val dbKonto = kontoDao.getKonto(konto!!.nummer, context)
            if (dbKonto != null) {
                el.oldValue = dbKonto
            }
        }
        sheet.setStatus(ImportStatus.RECONCILED)
        sheet.calculateStatistics()
    }

    private fun reconcileBuchungsdaten(sheet: ImportedSheet<BuchungssatzDO>, context: PfPersistenceContext) {
        log.info("Reconcile Buchungsdaten called")
        sheet.getElements()?.forEach { el ->
            val satz = el.value
            if (satz!!.year != null && satz.month != null && satz.satznr != null) {
                val dbSatz = buchungssatzDao.getBuchungssatz(
                    satz.year!!, satz.month!!,
                    satz.satznr!!,
                    context,
                )
                if (dbSatz != null) {
                    el.oldValue = dbSatz
                }
            }
        }
        sheet.setStatus(ImportStatus.RECONCILED)
        sheet.calculateStatistics()
    }

    private fun commitKontenplan(sheet: ImportedSheet<KontoDO?>): Int {
        log.info("Commit Kontenplan called")
        val col = mutableListOf<KontoDO>()
        sheet.getElements()?.filter { it.selected }?.forEach { el ->
            el.value?.let { konto ->
                if (el.oldValue != null) {
                    konto.id = el.oldValue!!.id
                }
                col.add(konto)
            }
        }
        kontoDao.internalSaveOrUpdateInTrans(col, KONTO_INSERT_BLOCK_SIZE)
        return col.size
    }

    private fun commitBuchungsdaten(sheet: ImportedSheet<BuchungssatzDO?>): Int {
        log.info("Commit Buchungsdaten called")
        val col = mutableListOf<BuchungssatzDO>()
        sheet.getElements()?.filter { it.selected }?.forEach { el ->
            el.value?.let { satz ->
                if (el.oldValue != null) {
                    satz.id = el.oldValue!!.id
                }
                col.add(satz)
            }
        }
        buchungssatzDao.internalSaveOrUpdateInTrans(col, BUCHUNGSSATZ_INSERT_BLOCK_SIZE)
        return col.size
    }

    enum class Type {
        KONTENPLAN, BUCHUNGSSAETZE
    }

    companion object {
        val USER_RIGHT_ID: UserRightId = UserRightId.FIBU_DATEV_IMPORT
        val KONTO_DIFF_PROPERTIES: Array<String> = arrayOf("nummer", "bezeichnung")
        val BUCHUNGSSATZ_DIFF_PROPERTIES: Array<String> = arrayOf(
            "satznr", "betrag", "sh", "konto", "kost2", "menge", "beleg",
            "datum",
            "gegenKonto", "text", "kost1", "comment"
        )

        /**
         * Size of bulk inserts. If this value is too large, exceptions are expected and as more small the value is so as more
         * slowly is the insert process.
         */
        private const val BUCHUNGSSATZ_INSERT_BLOCK_SIZE = 50

        /**
         * Size of bulk inserts. If this value is too large, exceptions are expected and as more small the value is so as more
         * slowly is the insert process.
         */
        private const val KONTO_INSERT_BLOCK_SIZE = 50

        private val log = LoggerFactory.getLogger(DatevImportDao::class.java)

        /**
         * Has the user the right FIBU_DATEV_IMPORT (value true)?
         *
         * @param accessChecker
         * @see UserRightId.FIBU_DATEV_IMPORT
         */
        fun hasRight(accessChecker: AccessChecker): Boolean {
            return hasRight(accessChecker, false)
        }

        /**
         * Has the user the right FIBU_DATEV_IMPORT (value true)?
         *
         * @param accessChecker
         * @throws AccessException
         * @see UserRightId.FIBU_DATEV_IMPORT
         */
        fun checkLoggeinUserRight(accessChecker: AccessChecker): Boolean {
            return hasRight(accessChecker, true)
        }

        private fun hasRight(accessChecker: AccessChecker, throwException: Boolean): Boolean {
            return accessChecker.hasLoggedInUserRight(USER_RIGHT_ID, throwException, UserRightValue.TRUE)
        }
    }
}
