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

package org.projectforge.plugins.banking

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.jobs.JobHandler
import org.projectforge.rest.config.Rest
import org.projectforge.rest.dto.BankAccount
import org.projectforge.rest.importer.AbstractImportRest
import org.projectforge.rest.importer.CsvImporter
import org.projectforge.rest.importer.ImportPairEntry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.InputStream

private val log = KotlinLogging.logger {}

/**
 * The layout-free import of the transactions of a bank account (`/next/bankAccount/import`), the successor of the
 * React pages `BankAccountRecordImportPageRest` and of the upload `BankingServicesRest`. The csv file is parsed with
 * the import settings of the target account, which therefore comes with the upload (field `bankAccountId`) and
 * can't be changed afterwards.
 *
 * The account is always the stored one, read from the database with the access of the logged-in user: only users
 * with write access to it ([BankAccountDao]) may import. The transactions of the file's period missing in the
 * file are offered as deleted (see [BankingImportStorage]).
 */
@RestController
@RequestMapping("${Rest.URL}/bankAccountRecordImport")
class BankAccountRecordImportRest : AbstractImportRest<BankAccountRecord, BankingImportStorage>() {
    @Autowired
    private lateinit var bankAccountDao: BankAccountDao

    @Autowired
    private lateinit var bankAccountRecordDao: BankAccountRecordDao

    @Autowired
    private lateinit var jobHandler: JobHandler

    override val fileExtensions = arrayOf("csv")

    override val maxFileUploadSizeMB = 100L

    /** Bank accounts are for the finance group only (see [BankAccountDao.hasAccess]). */
    override fun checkRight() {
        bankAccountDao.hasLoggedInUserSelectAccess(true)
    }

    override fun proceedUpload(inputStream: InputStream, filename: String): BankingImportStorage {
        throw IllegalArgumentException(translate("plugins.banking.account.record.import.error.noBankAccountGiven"))
    }

    override fun proceedUpload(
        inputStream: InputStream,
        filename: String,
        request: HttpServletRequest,
    ): BankingImportStorage {
        val bankAccountDO = getWritableAccount(request.getParameter("bankAccountId")?.toLongOrNull())
        val bankAccount = BankAccount()
        bankAccount.copyFrom(bankAccountDO)
        log.info { "Importing transactions for bank account #${bankAccountDO.id}, iban=${bankAccount.iban}" }
        val storage = BankingImportStorage(bankAccount.importSettings, bankAccount)
        storage.filename = filename
        CsvImporter.parse(inputStream, storage, storage.importSettings.charSet)
        storage.analyzeReadTransactions()
        storage.reconcileImportStorage()
        // Not needed until the next reconcile, which reads them again.
        storage.databaseTransactions = null
        return storage
    }

    override fun import(storage: BankingImportStorage, selectedEntries: List<ImportPairEntry<BankAccountRecord>>): Int? {
        // The account of the storage is the one of the upload, read again for the current rights.
        val bankAccountDO = getWritableAccount((storage.targetEntity as? BankAccount)?.id)
        return jobHandler.addJob(
            BankingImportJob(
                bankAccountDO,
                bankAccountDao,
                bankAccountRecordDao,
                selectedEntries,
                importStorage = storage,
            )
        ).id
    }

    override fun extraViewMeta(storage: BankingImportStorage): Map<String, Any>? {
        val bankAccount = storage.targetEntity as? BankAccount ?: return null
        val id = bankAccount.id ?: return null
        return mapOf("bankAccountId" to id, "bankAccountName" to (bankAccount.name ?: ""))
    }

    /**
     * The stored account of the given id, if the logged-in user may write into it. Otherwise an exception with a
     * message for the user (answered as `400` by the upload, see [AbstractImportRest.upload]).
     */
    private fun getWritableAccount(id: Long?): BankAccountDO {
        val noAccount = translate("plugins.banking.account.record.import.error.noBankAccountGiven")
        id ?: throw IllegalArgumentException(noAccount)
        val bankAccountDO = bankAccountDao.find(id, checkAccess = false)
        if (bankAccountDO == null || bankAccountDO.deleted
            || !bankAccountDao.hasLoggedInUserUpdateAccess(bankAccountDO, bankAccountDO, false)
        ) {
            log.warn { "Bank account #$id not found or not writable for the logged-in user." }
            throw IllegalArgumentException(noAccount)
        }
        return bankAccountDO
    }
}
