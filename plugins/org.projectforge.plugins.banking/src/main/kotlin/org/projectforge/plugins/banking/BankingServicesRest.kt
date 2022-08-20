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
import org.projectforge.rest.importer.CsvImporter
import org.projectforge.rest.importer.MappingInfo
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
    var importStorage: BankingImportStorage? = null
    if (filename.endsWith("xls", ignoreCase = true) || filename.endsWith("xlsx", ignoreCase = true)) {
      throw IllegalArgumentException("Excel not yet supported.")
    } else {
      importStorage = BankingImportStorage()
      // Try to import CSV
      CsvImporter.parse(file.inputStream.reader(), MappingInfo(), importStorage)
    }
    transactionsImporter.import(request, bankAccountDO, importStorage)
    return ResponseEntity.ok("OK")
  }

  companion object {
    const val REST_PATH = "${Rest.URL}/banking"
  }
}
