/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.address

import de.micromata.merlin.excel.Configuration
import de.micromata.merlin.excel.ExcelRow
import de.micromata.merlin.excel.ExcelSheet
import de.micromata.merlin.excel.ExcelWorkbook
import mu.KotlinLogging
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.projectforge.business.converter.LanguageConverter
import org.projectforge.business.excel.ExcelDateFormats
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.excel.ExcelUtils
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * For excel export.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service("addressExport")
open class AddressExport {
  @Autowired
  private lateinit var accessChecker: AccessChecker

  protected fun registerCols(sheet: ExcelSheet) {
    register(sheet, "name", 20)
    register(sheet, "firstName", 20)
    register(sheet, "form", 8)
    register(sheet, "title", 10)
    register(sheet, "contactStatus", 10)
    register(sheet, "organization")
    register(sheet, "division")
    register(sheet, "positionText")
    register(sheet, "communicationLanguage")
    registerAdditionalCols(sheet) // Used by AddressCampaignValueExport
    register(sheet, "email", ExcelUtils.Size.EMAIL)
    register(sheet, "website")

    register(sheet, "address.addressText", "mailingAddressText")
    register(sheet, "address.addressText2", "mailingAddressText2")
    register(sheet, "address.zipCode", "mailingZipCode", ExcelUtils.Size.ZIPCODE)
    register(sheet, "address.city", "mailingCity")
    register(sheet, "address.country", "mailingCountry")
    register(sheet, "address.state", "mailingState")

    registerAddress(sheet, "") // register business address: address, city, ...
    registerAddress(sheet, "postal")
    register(sheet, "addressStatus")
    register(sheet, "businessPhone", ExcelUtils.Size.PHONENUMBER)
    register(sheet, "fax", ExcelUtils.Size.PHONENUMBER)
    register(sheet, "mobilePhone", ExcelUtils.Size.PHONENUMBER)

    registerAddress(sheet, "private")
    register(sheet, "address.privateEmail", "privateEmail", ExcelUtils.Size.EMAIL)
    register(sheet, "address.phoneType.private", "privatePhone", ExcelUtils.Size.PHONENUMBER)
    register(sheet, "address.phoneType.privateMobile", "privateMobilePhone", ExcelUtils.Size.PHONENUMBER)
    register(sheet, "birthday", ExcelUtils.Size.DATE)
    register(sheet, "lastUpdate", ExcelUtils.Size.DATE)
    register(sheet, "comment")
    register(sheet, "fingerprint")
    register(sheet, "publicKey", ExcelUtils.Size.EXTRA_LONG)
    register(sheet, "id")
  }

  // Implemented by AddressCampaignValueExport
  protected open fun registerAdditionalCols(sheet: ExcelSheet) {
  }

  protected fun register(sheet: ExcelSheet, i18nKey: String, alias: String, size: Int = ExcelUtils.Size.STANDARD) {
    sheet.registerColumn(translate(i18nKey), alias).withSize(size)
  }

  protected fun register(sheet: ExcelSheet, property: String, size: Int = ExcelUtils.Size.STANDARD) {
    ExcelUtils.registerColumn(sheet, AddressDO::class.java, property, size)
  }

  private fun registerAddress(sheet: ExcelSheet, prefix: String) {
    // decapitalized only needed, if prefix is empty:
    register(sheet, "${prefix}AddressText".decapitalize())
    register(sheet, "${prefix}AddressText2".decapitalize())
    register(sheet, "${prefix}ZipCode".decapitalize(), ExcelUtils.Size.ZIPCODE)
    register(sheet, "${prefix}City".decapitalize())
    register(sheet, "${prefix}Country".decapitalize())
    register(sheet, "${prefix}State".decapitalize())
  }

  /**
   * Exports the filtered list as table with almost all fields. For members of group FINANCE_GROUP (PF_Finance) and
   * MARKETING_GROUP (PF_Marketing) all addresses are exported, for others only those which are marked as personal
   * favorites.
   */
  open fun export(
    origList: List<AddressDO>,
    personalAddressMap: Map<Int?, PersonalAddressDO?>,
    vararg params: Any
  ): ByteArray? {
    log.info("Exporting address list.")

    val list: MutableList<AddressDO?> = ArrayList()
    for (address in origList) {
      if (accessChecker.isLoggedInUserMemberOfGroup(
          ProjectForgeGroup.FINANCE_GROUP,
          ProjectForgeGroup.MARKETING_GROUP
        )
      ) {
        // Add all addresses for users of finance group:
        list.add(address)
      } else if (personalAddressMap.containsKey(address.id)) // For others only those which are personal:
        list.add(address)
    }
    if (list.isEmpty()) {
      return null
    }

    val workbook = ExcelWorkbook(XSSFWorkbook())
    workbook.configuration.setDateFormats(
      ThreadLocalUserContext.getUser().excelDateFormat ?: ExcelDateFormats.EXCEL_DEFAULT_DATE,
      Configuration.TimeStampPrecision.DAY
    )
    val sheet = workbook.createOrGetSheet(translate(sheetTitle))!!
    sheet.enableMultipleColumns = true
    initSheet(sheet, *params)
    sheet.createFreezePane(2, 2)

    val boldFont = workbook.createOrGetFont("bold")!!
    boldFont.bold = true
    val boldStyle = workbook.createOrGetCellStyle("hr")
    boldStyle.setFont(boldFont)
    registerCols(sheet)
    sheet.createRow() // title row
    val headRow = sheet.createRow() // second row as head row.
    sheet.columnDefinitions.forEachIndexed { index, it ->
      headRow.getCell(index).setCellValue(it.columnHeadname).setCellStyle(boldStyle)
    }
    //sheet.poiSheet.setAutoFilter(CellRangeAddress(1, 1, 0, sheet.getRow(1).lastCellNum - 1))
    sheet.setMergedRegion(
      0,
      0,
      sheet.getColNumber("mailingAddressText")!!,
      sheet.getColNumber("mailingState")!!,
      translate("address.mailing")
    ).setCellStyle(boldStyle)
    sheet.setMergedRegion(
      0,
      0,
      sheet.getColNumber("addressText")!!,
      sheet.getColNumber("state")!!,
      translate("address.addressText")
    ).setCellStyle(boldStyle)
    sheet.setMergedRegion(
      0,
      0,
      sheet.getColNumber("postalAddressText")!!,
      sheet.getColNumber("postalState")!!,
      translate("address.postalAddressText")
    ).setCellStyle(boldStyle)
    sheet.setMergedRegion(
      0,
      0,
      sheet.getColNumber("privateAddressText")!!,
      sheet.getColNumber("privateState")!!,
      translate("address.privateAddressText")
    ).setCellStyle(boldStyle)

    for (address in list) {
      address ?: continue
      val row = sheet.createRow()
      row.autoFillFromObject(address, "communicationLanguage")
      val lang =
        LanguageConverter.getLanguageAsString(address.communicationLanguage, ThreadLocalUserContext.getLocale())
      row.getCell("communicationLanguage")!!.setCellValue(lang)
      row.getCell("mailingAddressText")!!.setCellValue(address.mailingAddressText);
      row.getCell("mailingAddressText2")!!.setCellValue(address.mailingAddressText2);
      row.getCell("mailingZipCode")!!.setCellValue(address.mailingZipCode);
      row.getCell("mailingCity")!!.setCellValue(address.mailingCity);
      row.getCell("mailingCountry")!!.setCellValue(address.mailingCountry);
      row.getCell("mailingState")!!.setCellValue(address.mailingState);

      handleAddressCampaign(row, address, *params)
    }
    return workbook.asByteArrayOutputStream.toByteArray()
  }

  protected open fun initSheet(sheet: ExcelSheet, vararg params: Any) {

  }

  protected open fun handleAddressCampaign(row: ExcelRow, address: AddressDO, vararg params: Any) {
  }

  protected open val addressCampaignSupported = false

  protected open val sheetTitle = "address.addresses"
}
