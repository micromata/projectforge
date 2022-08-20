/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

import mu.KotlinLogging
import org.projectforge.common.FormatterUtils
import org.projectforge.rest.config.Rest
import org.projectforge.rest.dto.BankAccount
import org.projectforge.rest.dto.BankAccountRecordMapping
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping(BankingServicesRest.REST_PATH)
class BankingServicesRest {
  @Autowired
  private lateinit var bankAccountDao: BankAccountDao

  @Autowired
  private lateinit var transactionsImporter: TransactionsImporter

  @PostMapping("import/{id}")
  fun import(
    request: HttpServletRequest,
    @PathVariable("id", required = true) id: Int,
    @RequestParam("file") file: MultipartFile
  ): ResponseEntity<*> {
    val filename = file.originalFilename
    log.info {
      "User tries to upload serial execution file: id='$id', filename='$filename', size=${
        FormatterUtils.formatBytes(
          file.size
        )
      }."
    }
    val bankAccountDO = bankAccountDao.getById(id)
    if (bankAccountDO == null) {
      log.warn("Bank account with id #$id not found.")
      throw IllegalArgumentException()
    }
    val bankAccount = BankAccount()
    bankAccount.copyFrom(bankAccountDO)
    log.info("Importing transactions for bank account #$id, iban=${bankAccount.iban}")
    var context: ImportContext? = null
    val mapping = BankAccountRecordMapping()
    mapping.mappingMap[BankAccountRecord::date.name] = listOf("buchungstag")
    mapping.mappingMap[BankAccountRecord::valueDate.name] = listOf("valuta*")
    mapping.mappingMap[BankAccountRecord::type.name] = listOf("buchungstext*")
    mapping.mappingMap[BankAccountRecord::subject.name] = listOf("verwendung*")
    mapping.mappingMap[BankAccountRecord::debteeId.name] = listOf("gläub*", "glaeu*")
    mapping.mappingMap[BankAccountRecord::mandateReference.name] = listOf("Mandat*")
    mapping.mappingMap[BankAccountRecord::customerReference.name] = listOf("Kundenref*")
    mapping.mappingMap[BankAccountRecord::collectionReference.name] = listOf("sammler*")
    mapping.mappingMap[BankAccountRecord::receiverSender.name] = listOf("*beguen*", "*zahlungspflicht*")
    mapping.mappingMap[BankAccountRecord::iban.name] = listOf("*iban*")
    mapping.mappingMap[BankAccountRecord::bic.name] = listOf("bic*")
    mapping.mappingMap[BankAccountRecord::amount.name] = listOf("betrag")
    mapping.mappingMap[BankAccountRecord::currency.name] = listOf("waehrung")
    mapping.mappingMap[BankAccountRecord::info.name] = listOf("info")
    bankAccount.mappingTable = mapping
    if (filename.endsWith("xls", ignoreCase = true) || filename.endsWith("xlsx", ignoreCase = true)) {
      throw IllegalArgumentException("Excel not yet supported.")
    } else {
      // Try to import CSV
      context = CsvImporter.parse(file.inputStream.reader(), bankAccount = bankAccount)
    }
    transactionsImporter.import(request, bankAccountDO, importContext = context)
    return ResponseEntity.ok("OK")
  }

  companion object {
    const val REST_PATH = "${Rest.URL}/banking"
  }
}
