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

package org.projectforge.rest

import de.micromata.merlin.utils.ReplaceUtils
import org.apache.commons.io.IOUtils
import org.projectforge.business.address.AddressDao
import org.projectforge.business.address.AddressExport
import org.projectforge.business.address.PersonalAddressDao
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.time.DateHelper
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.LanguageService
import org.projectforge.rest.core.ResultSet
import org.projectforge.ui.UIColor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*
import jakarta.servlet.http.HttpServletRequest

/**
 * For uploading address immages.
 */
@RestController
@RequestMapping("${Rest.URL}/address")
class AddressServicesRest {

  companion object {
    internal const val SESSION_IMAGE_ATTR = "uploadedAddressImage"
    private const val APPLE_SCRIPT_DIR = "misc/"
    private const val APPLE_SCRIPT_FOR_ADDRESS_BOOK = "AddressBookRemoveNotesOfClassWork.scpt"
  }

  private val log = org.slf4j.LoggerFactory.getLogger(AddressServicesRest::class.java)

  @Autowired
  private lateinit var addressDao: AddressDao

  @Autowired
  private lateinit var addressRest: AddressPagesRest

  @Autowired
  private lateinit var addressExport: AddressExport

  @Autowired
  private lateinit var personalAddressDao: PersonalAddressDao

  @Autowired
  private lateinit var languageService: LanguageService

  @GetMapping("acLang")
  fun getLanguages(@RequestParam("search") search: String?): List<DisplayLanguage> {
    return languageService.getLanguages(search).map { DisplayLanguage(it.value, it.label) }
  }

  @GetMapping("usedLanguages")
  fun getUsedLanguages(): List<LanguageService.Language> {
    return languageService.getLanguages(addressDao.usedCommunicationLanguages.asIterable())
  }

  @GetMapping("exportFavoritesVCards")
  fun exportFavoritesVCards(): ResponseEntity<*> {
    log.info("Exporting personal address book as vcards.")
    val list = addressDao.favoriteVCards
    if (list.isNullOrEmpty()) {
      return ResponseEntity(
        ResponseData(
          "address.book.hasNoVCards",
          messageType = MessageType.TOAST,
          color = UIColor.WARNING
        ), HttpStatus.NOT_FOUND
      )
    }
    val filename = ("ProjectForge-PersonalAddressBook_" + DateHelper.getDateAsFilenameSuffix(Date())
        + ".vcf")
    val writer = StringWriter()
    addressDao.exportFavoriteVCards(writer, list)

    return RestUtils.downloadFile(filename, writer.toString())
  }

  /**
   * Exports favorites addresses.
   */
  @GetMapping("exportFavoritesExcel")
  fun exportFavoritesExcel(request: HttpServletRequest): ResponseEntity<*> {
    log.info("Exporting personal address book as Excel file.")
    val list = addressDao.favoriteVCards.map { it.address!! }
    val magicFilter = MagicFilter(maxRows = QueryFilter.QUERY_FILTER_MAX_ROWS)
    val resultSet = ResultSet(list, null, list.size, magicFilter = magicFilter)
    addressRest.postProcessResultSet(resultSet, request, magicFilter)

    val personalAddressMap = personalAddressDao.personalAddressByAddressId

    val xls = addressExport.export(list, personalAddressMap)
    if (xls == null || xls.isEmpty()) {
      return ResponseEntity(
        ResponseData(
          "address.book.hasNoVCards",
          messageType = MessageType.TOAST,
          color = UIColor.WARNING
        ), HttpStatus.NOT_FOUND
      )
    }
    val filename = ("ProjectForge-AddressExport_" + DateHelper.getDateAsFilenameSuffix(Date())
        + ".xlsx")

    val resource = ByteArrayResource(xls)
    return RestUtils.downloadFile(filename, resource)
  }

  @GetMapping("downloadAppleScript")
  fun downloadAppleScript(): ResponseEntity<*> {
    log.info("Downloading AppleScript.")
    val content: ByteArray?
    val file = APPLE_SCRIPT_DIR + APPLE_SCRIPT_FOR_ADDRESS_BOOK
    try {
      val cLoader = this.javaClass.classLoader
      val inputStream = cLoader.getResourceAsStream(file)
      if (inputStream == null) {
        log.error("Could not find script in resource path: '$file'.")
      }
      content = IOUtils.toByteArray(inputStream!!)
    } catch (ex: IOException) {
      log.error("Could not load script '" + file + "'." + ex.message, ex)
      throw RuntimeException(ex)
    }
    val filename = (APPLE_SCRIPT_FOR_ADDRESS_BOOK)
    val resource = ByteArrayResource(content!!)
    return RestUtils.downloadFile(filename, resource)
  }

  @GetMapping("exportVCard/{id}")
  fun exportVCard(@PathVariable("id") id: Long?): ResponseEntity<*> {
    val address = addressDao.find(id) ?: return ResponseEntity<Any>(HttpStatus.NOT_FOUND)
    val filename = ("ProjectForge-" + ReplaceUtils.encodeFilename(address.fullName, true) + "_"
        + DateHelper.getDateAsFilenameSuffix(Date()) + ".vcf")
    val writer = StringWriter()
    addressDao.exportVCard(PrintWriter(writer), address)
    return RestUtils.downloadFile(filename, writer.toString())
  }

  class DisplayLanguage(val id: String, val displayName: String)
}
