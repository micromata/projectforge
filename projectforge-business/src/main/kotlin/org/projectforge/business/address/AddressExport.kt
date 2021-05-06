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

import de.micromata.merlin.excel.*
import mu.KotlinLogging
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.projectforge.business.converter.LanguageConverter
import org.projectforge.business.excel.ExcelDateFormats
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

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

  enum class ExcelCol(val header: String, val width: Int) {
    NAME("name", 20),
    FIRST_NAME("firstName", 20),
    FORM("address.form", 8),
    TITLE("address.title", 10),
    CONTACT_STATUS("address.contactStatus", 10),
    ORGANIZATION("organization", LENGTH_STD),
    DIVISION("address.division", LENGTH_STD),
    POSITION("address.positionText", LENGTH_STD),  //
    COMMUNICATION_LANG("address.communication", LENGTH_STD),  //
    ADDRESS_CAMPAIGN_VALUE("value", LENGTH_STD), // Plugin marketing
    ADDRESS_CAMPAIGN_COMMENT("comment", LENGTH_STD), // Plugin marketing
    EMAIL("email", LENGTH_EMAIL),
    WEBSITE("address.website", LENGTH_STD),
    MAILING_ADDRESS("address.heading.addressText", LENGTH_STD),
    MAILING_ADDRESS2("address.heading.addressText2", LENGTH_STD),
    MAILING_ZIPCODE("address.zipCode", LENGTH_ZIPCODE),
    MAILING_CITY("address.city", LENGTH_STD),
    MAILING_COUNTRY("address.country", LENGTH_STD),
    MAILING_STATE("address.state", LENGTH_STD),
    ADDRESS("address.addressText", LENGTH_STD),
    ADDRESS2("address.addressText2", LENGTH_STD),
    ZIPCODE("address.zipCode", LENGTH_ZIPCODE),  //
    CITY("address.city", LENGTH_STD),
    COUNTRY("address.country", LENGTH_STD),  //
    STATE("address.state", LENGTH_STD),
    POSTAL_ADDRESS("address.heading.postalAddressText", LENGTH_STD),
    POSTAL_ADDRESS2("address.heading.postalAddressText2", LENGTH_STD),
    POSTAL_ZIPCODE("address.zipCode", LENGTH_ZIPCODE),
    POSTAL_CITY("address.city", LENGTH_STD),
    POSTAL_COUNTRY("address.country", LENGTH_STD),
    POSTAL_STATE("address.state", LENGTH_STD),
    ADDRESS_STATUS("address.addressStatus", 12),
    BUSINESS_PHONE("address.phoneType.business", LENGTH_PHONENUMBER),
    FAX("address.phoneType.fax", LENGTH_PHONENUMBER),
    MOBILE_PHONE("address.phoneType.mobile", LENGTH_PHONENUMBER),
    PRIVATE_ADDRESS("address.heading.privateAddressText", LENGTH_STD),
    PRIVATE_ADDRESS2("address.heading.privateAddressText2", LENGTH_STD),
    PRIVATE_ZIPCODE("address.zipCode", LENGTH_ZIPCODE),
    PRIVATE_CITY("address.city", LENGTH_STD),
    PRIVATE_COUNTRY("address.country", LENGTH_STD),
    PRIVATE_STATE("address.state", LENGTH_STD),
    PRIVATE_EMAIL("address.privateEmail", LENGTH_EMAIL),
    PRIVATE_PHONE("address.phoneType.private", LENGTH_PHONENUMBER),
    PRIVATE_MOBILE("address.phoneType.privateMobile", LENGTH_PHONENUMBER),
    BIRTHDAY("address.birthday", DATE_LENGTH),  //
    MODIFIED("modified", DATE_LENGTH),  //
    CREATED("created", DATE_LENGTH),  //
    COMMENT("comment", LENGTH_EXTRA_LONG),
    FINGERPRINT("address.fingerprint", LENGTH_STD),
    PUBLIC_KEY("address.publicKey", LENGTH_EXTRA_LONG),  //
    ID("id", DATE_LENGTH)
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
    workbook.standardFormats.dateFormat = ThreadLocalUserContext.getUser().excelDateFormat ?: ExcelDateFormats.EXCEL_DEFAULT_DATE
    workbook.standardFormats.setTimestampPrecision(StandardFormats.TimeStampPrecision.DAY)
    val sheet = workbook.createOrGetSheet(translate(sheetTitle))!!
    sheet.setMergedRegion(0, 0, ExcelCol.MAILING_ADDRESS, ExcelCol.MAILING_STATE, "Mailing")
    sheet.setMergedRegion(0, 0, ExcelCol.ADDRESS, ExcelCol.STATE, translate("address.addressText"))
    sheet.setMergedRegion(0, 0, ExcelCol.POSTAL_ADDRESS, ExcelCol.POSTAL_STATE, translate("address.postalAddressText"))
    sheet.setMergedRegion(
      0,
      0,
      ExcelCol.PRIVATE_ADDRESS,
      ExcelCol.PRIVATE_STATE,
      translate("address.privateAddressText")
    )
    initSheet(sheet, *params)
    sheet.createFreezePane(2, 2)

    val boldFont = workbook.createOrGetFont("bold")!!
    boldFont.bold = true
    val boldStyle = workbook.createOrGetCellStyle("hr")
    boldStyle.setFont(boldFont)
    val headRow = sheet.createRow()
    ExcelCol.values().forEach {
      if (addressCampaignSupported || it != ExcelCol.ADDRESS_CAMPAIGN_VALUE && it != ExcelCol.ADDRESS_CAMPAIGN_COMMENT) {
        headRow.getCell(getCellNo(it))
          .setCellValue(translate(it.header))
          .setCellStyle(boldStyle)
        sheet.setColumnWidth(getCellNo(it), it.width * 256)
      }
    }
    sheet.poiSheet.setAutoFilter(CellRangeAddress(1, 1, 0, sheet.getRow(1).lastCellNum - 1))

    for (address in list) {
      address ?: continue
      val row = sheet.createRow()
      row.getCell(ExcelCol.NAME).setCellValue(address.name)
      row.getCell(ExcelCol.FIRST_NAME).setCellValue(address.firstName)
      row.getCell(ExcelCol.FORM)
        .setCellValue(address.form?.let { translate(address.form?.i18nKey) } ?: "")
      row.getCell(ExcelCol.TITLE).setCellValue(address.title)
      row.getCell(ExcelCol.CONTACT_STATUS).setCellValue(address.contactStatus)
      row.getCell(ExcelCol.ORGANIZATION).setCellValue(address.organization)
      row.getCell(ExcelCol.DIVISION).setCellValue(address.division)
      row.getCell(ExcelCol.POSITION).setCellValue(address.positionText)
      val lang =
        LanguageConverter.getLanguageAsString(address.communicationLanguage, ThreadLocalUserContext.getLocale())
      row.getCell(ExcelCol.COMMUNICATION_LANG).setCellValue(lang)

      handleAddressCampaign(row, address, *params)

      row.getCell(getCellNo(ExcelCol.EMAIL)).setCellValue(address.email)
      row.getCell(getCellNo(ExcelCol.WEBSITE)).setCellValue(address.website)
      row.getCell(getCellNo(ExcelCol.MAILING_ADDRESS)).setCellValue(address.mailingAddressText)
      row.getCell(getCellNo(ExcelCol.MAILING_ADDRESS2)).setCellValue(address.mailingAddressText2)
      row.getCell(getCellNo(ExcelCol.MAILING_ZIPCODE)).setCellValue(address.mailingZipCode)
      row.getCell(getCellNo(ExcelCol.MAILING_CITY)).setCellValue(address.mailingCity)
      row.getCell(getCellNo(ExcelCol.MAILING_COUNTRY)).setCellValue(address.mailingCountry)
      row.getCell(getCellNo(ExcelCol.MAILING_STATE)).setCellValue(address.mailingState)
      row.getCell(getCellNo(ExcelCol.ADDRESS)).setCellValue(address.addressText)
      row.getCell(getCellNo(ExcelCol.ADDRESS2)).setCellValue(address.addressText2)
      row.getCell(getCellNo(ExcelCol.ZIPCODE)).setCellValue(address.zipCode)
      row.getCell(getCellNo(ExcelCol.CITY)).setCellValue(address.city)
      row.getCell(getCellNo(ExcelCol.COUNTRY)).setCellValue(address.country)
      row.getCell(getCellNo(ExcelCol.STATE)).setCellValue(address.state)
      row.getCell(getCellNo(ExcelCol.POSTAL_ADDRESS)).setCellValue(address.postalAddressText)
      row.getCell(getCellNo(ExcelCol.POSTAL_ADDRESS2)).setCellValue(address.postalAddressText2)
      row.getCell(getCellNo(ExcelCol.POSTAL_ZIPCODE)).setCellValue(address.postalZipCode)
      row.getCell(getCellNo(ExcelCol.POSTAL_CITY)).setCellValue(address.postalCity)
      row.getCell(getCellNo(ExcelCol.POSTAL_COUNTRY)).setCellValue(address.postalCountry)
      row.getCell(getCellNo(ExcelCol.POSTAL_STATE)).setCellValue(address.postalState)
      row.getCell(getCellNo(ExcelCol.ADDRESS_STATUS)).setCellValue(address.addressStatus)
      row.getCell(getCellNo(ExcelCol.BUSINESS_PHONE)).setCellValue(address.businessPhone)
      row.getCell(getCellNo(ExcelCol.FAX)).setCellValue(address.fax)
      row.getCell(getCellNo(ExcelCol.MOBILE_PHONE)).setCellValue(address.mobilePhone)
      row.getCell(getCellNo(ExcelCol.PRIVATE_ADDRESS)).setCellValue(address.privateAddressText)
      row.getCell(getCellNo(ExcelCol.PRIVATE_ADDRESS2)).setCellValue(address.privateAddressText2)
      row.getCell(getCellNo(ExcelCol.PRIVATE_ZIPCODE)).setCellValue(address.privateZipCode)
      row.getCell(getCellNo(ExcelCol.PRIVATE_CITY)).setCellValue(address.privateCity)
      row.getCell(getCellNo(ExcelCol.PRIVATE_COUNTRY)).setCellValue(address.privateCountry)
      row.getCell(getCellNo(ExcelCol.PRIVATE_STATE)).setCellValue(address.privateState)
      row.getCell(getCellNo(ExcelCol.PRIVATE_EMAIL)).setCellValue(address.privateEmail)
      row.getCell(getCellNo(ExcelCol.PRIVATE_PHONE)).setCellValue(address.privatePhone)
      row.getCell(getCellNo(ExcelCol.PRIVATE_MOBILE)).setCellValue(address.privateMobilePhone)
      row.getCell(getCellNo(ExcelCol.BIRTHDAY)).setCellValue(workbook, address.birthday)
      row.getCell(getCellNo(ExcelCol.CREATED)).setCellValue(workbook, address.created)
      row.getCell(getCellNo(ExcelCol.MODIFIED)).setCellValue(workbook, address.lastUpdate)
      row.getCell(getCellNo(ExcelCol.COMMENT)).setCellValue(address.comment)
      row.getCell(getCellNo(ExcelCol.FINGERPRINT)).setCellValue(address.fingerprint)
      row.getCell(getCellNo(ExcelCol.PUBLIC_KEY)).setCellValue(address.publicKey)
      row.getCell(getCellNo(ExcelCol.ID)).setCellValue(workbook, address.id)
    }
    return workbook.asByteArrayOutputStream.toByteArray()
  }

  protected open fun initSheet(sheet: ExcelSheet, vararg params: Any) {

  }

  protected open fun handleAddressCampaign(row: ExcelRow, address: AddressDO, vararg params: Any) {
  }

  /**
   * Exclude address campaign cols for normal address export.
   */
  private fun getCellNo(col: ExcelCol): Int {
    if (addressCampaignSupported || col < ExcelCol.ADDRESS_CAMPAIGN_VALUE) {
      return col.ordinal
    }
    return col.ordinal - 2
  }

  protected open val addressCampaignSupported = false

  protected open val sheetTitle = "address.addresses"

  companion object {
    const val LENGTH_PHONENUMBER = 20
    const val LENGTH_EMAIL = 30
    const val LENGTH_ZIPCODE = 7
    const val LENGTH_STD = 30
    const val LENGTH_EXTRA_LONG = 80
    const val DATE_LENGTH = 10
  }
}
