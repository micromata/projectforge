/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.sipgate

import de.micromata.merlin.excel.ExcelWorkbook
import org.projectforge.business.sipgate.SipgateAddress
import org.projectforge.business.sipgate.SipgateDevice
import org.projectforge.business.sipgate.SipgateNumber
import org.projectforge.business.sipgate.SipgateUser
import org.projectforge.excel.ExcelUtils
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.DateHelper
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import java.util.*

/**
 * Exports Sipgate data (users, numbers, devices etc.) to Excel.
 * @author K. Reinhard (k.reinhard@micromata.de)
 */
object SipgateExcelExporter {
  private class Context(val workbook: ExcelWorkbook, val storage: SipgateDataStorage) {
    val boldFont = workbook.createOrGetFont("bold", bold = true)
    val boldStyle = workbook.createOrGetCellStyle("hr", font = boldFont)
    val wrapTextStyle = workbook.createOrGetCellStyle("wrap")

    init {
      wrapTextStyle.wrapText = true
    }
  }

  fun export(storage: SipgateDataStorage): ByteArray {
    ExcelWorkbook.createEmptyWorkbook(ThreadLocalUserContext.locale!!).use { workbook ->
      val context = Context(workbook, storage)
      addUsersSheet(context)
      addNumbersSheet(context)
      addDevicesSheet(context)
      addAddressesSheet(context)
      return workbook.asByteArrayOutputStream.toByteArray()
    }
  }

  fun download(storage: SipgateDataStorage): ResponseEntity<ByteArrayResource> {
    val workbook = export(storage)
    val filename = ("Sipgate-${DateHelper.getDateAsFilenameSuffix(Date())}.xlsx")
    val resource = ByteArrayResource(workbook)
    return ResponseEntity.ok()
      .contentType(org.springframework.http.MediaType.parseMediaType("application/octet-stream"))
      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=$filename")
      .body(resource)
  }

  private fun addUsersSheet(context: Context) {
    val sheet = context.workbook.createOrGetSheet("Users")
    val users = context.storage.users ?: return
    ExcelUtils.registerColumn(sheet, SipgateUser::id, 10, false)
    ExcelUtils.registerColumn(sheet, SipgateUser::firstname, logErrorIfPropertyInfoNotFound = false)
    ExcelUtils.registerColumn(sheet, SipgateUser::lastname, logErrorIfPropertyInfoNotFound = false)
    ExcelUtils.registerColumn(sheet, SipgateUser::email, logErrorIfPropertyInfoNotFound = false)
    ExcelUtils.registerColumn(sheet, SipgateUser::admin, 10, logErrorIfPropertyInfoNotFound = false)
    ExcelUtils.registerColumn(sheet, SipgateUser::defaultDevice, 10, logErrorIfPropertyInfoNotFound = false)
    ExcelUtils.registerColumn(sheet, SipgateUser::busyOnBusy, 10, logErrorIfPropertyInfoNotFound = false)
    ExcelUtils.registerColumn(sheet, SipgateUser::timezone, 20, logErrorIfPropertyInfoNotFound = false)
    ExcelUtils.registerColumn(sheet, SipgateUser::directDialIds, logErrorIfPropertyInfoNotFound = false)
    ExcelUtils.registerColumn(sheet, SipgateUser::addressId, 10, logErrorIfPropertyInfoNotFound = false)
    ExcelUtils.addHeadRow(sheet, context.boldStyle)
    users.sortedBy { it.fullname }.forEach { sheet.createRow().autoFillFromObject(it) }
    sheet.setAutoFilter()
  }

  private fun addNumbersSheet(context: Context) {
    val sheet = context.workbook.createOrGetSheet("Numbers")
    val numbers = context.storage.numbers ?: return
    ExcelUtils.registerColumn(sheet, SipgateNumber::id, 10, logErrorIfPropertyInfoNotFound = false)
    ExcelUtils.registerColumn(sheet, SipgateNumber::number, logErrorIfPropertyInfoNotFound = false)
    ExcelUtils.registerColumn(sheet, SipgateNumber::type, 10, logErrorIfPropertyInfoNotFound = false)
    sheet.registerColumn("Users by dial id", "usersByDialId").withSize(40)
    ExcelUtils.registerColumn(sheet, SipgateNumber::endpointId, 10, logErrorIfPropertyInfoNotFound = false)
    sheet.registerColumn("Users by endpointId (routings)", "usersByRouting").withSize(40)
    ExcelUtils.registerColumn(sheet, SipgateNumber::localized, 20, logErrorIfPropertyInfoNotFound = false)
    ExcelUtils.registerColumn(sheet, SipgateNumber::prolongationId, 15, logErrorIfPropertyInfoNotFound = false)
    ExcelUtils.registerColumn(sheet, SipgateNumber::blockId, 10, logErrorIfPropertyInfoNotFound = false)
    ExcelUtils.registerColumn(sheet, SipgateNumber::endpointUrl, 50, logErrorIfPropertyInfoNotFound = false)
    ExcelUtils.addHeadRow(sheet, context.boldStyle)
    numbers.forEach { number ->
      val row = sheet.createRow()
      row.autoFillFromObject(number, "usersByDialId", "usersByRouting")
      val users = context.storage.getUsersByDialId(number.id)
      if (users.isNotEmpty()) {
        row.getCell("usersByDialId")
          ?.let { cell ->
            cell.setCellValue(users.joinToString { it.fullname })
            cell.setCellStyle(context.wrapTextStyle)
          }
      }
      val routings = context.storage.getActivePhoneLines(number)
      if (routings.isNotEmpty()) {
        row.getCell("usersByRouting")
          ?.let { cell ->
            cell.setCellValue(routings.joinToString { it.alias ?: "???" })
            cell.setCellStyle(context.wrapTextStyle)
          }
      }
    }
    sheet.setAutoFilter()
  }

  private fun addDevicesSheet(context: Context) {
    val sheet = context.workbook.createOrGetSheet("Devices")
    val userDevices = context.storage.userDevices ?: return
    sheet.registerColumn("User-Id", "userId").withSize(10)
    sheet.registerColumn("User-Name", "userName").withSize(40)
    ExcelUtils.registerColumn(sheet, SipgateDevice::id, 10, logErrorIfPropertyInfoNotFound = false)
    ExcelUtils.registerColumn(sheet, SipgateDevice::type, 10, logErrorIfPropertyInfoNotFound = false)
    ExcelUtils.registerColumn(sheet, SipgateDevice::alias, 40, logErrorIfPropertyInfoNotFound = false)
    ExcelUtils.registerColumn(sheet, SipgateDevice::dnd, 10, logErrorIfPropertyInfoNotFound = false)
    ExcelUtils.registerColumn(sheet, SipgateDevice::esim, 10, logErrorIfPropertyInfoNotFound = false)
    ExcelUtils.registerColumn(sheet, SipgateDevice::online, 10, logErrorIfPropertyInfoNotFound = false)
    ExcelUtils.registerColumn(sheet, SipgateDevice::simState, 10, logErrorIfPropertyInfoNotFound = false)
    ExcelUtils.registerColumn(sheet, SipgateDevice::activePhonelines, 60, logErrorIfPropertyInfoNotFound = false)
    ExcelUtils.registerColumn(sheet, SipgateDevice::activeGroups, 60, logErrorIfPropertyInfoNotFound = false)
    ExcelUtils.registerColumn(sheet, SipgateDevice::credentials, 35, logErrorIfPropertyInfoNotFound = false)
    ExcelUtils.addHeadRow(sheet, context.boldStyle)
    userDevices.sortedBy { getUser(it.userId, context).fullname.lowercase() }.forEach { el ->
      val user = getUser(el.userId, context)
      el.devices?.forEach { device ->
        val row = sheet.createRow()
        row.getCell("userId")?.setCellValue(user.id)
        row.getCell("userName")?.setCellValue(user.fullname)
        row.autoFillFromObject(
          device,
          SipgateDevice::activeGroups.name,
          SipgateDevice::activePhonelines.name,
        )
        row.getCell(SipgateDevice::activeGroups.name)
          ?.let { cell ->
            cell.setCellValue(device.activeGroups?.joinToString(", ", prefix = "routing=[", postfix = "]"))
            cell.setCellStyle(context.wrapTextStyle)
          }
        row.getCell(SipgateDevice::activePhonelines.name)
          ?.let { cell ->
            cell.setCellValue(device.activePhonelines?.joinToString(", ", prefix = "routing=[", postfix = "]"))
            cell.setCellStyle(context.wrapTextStyle)
          }
      }
    }
    sheet.setAutoFilter()
  }

  private fun getUser(id: String?, context: Context): SipgateUser {
    return context.storage.users?.firstOrNull { it.id == id } ?: SipgateUser(
      id ?: "???",
      firstname = "???",
      lastname = "???",
    )
  }

  private fun addAddressesSheet(context: Context) {
    val sheet = context.workbook.createOrGetSheet("Addresses")
    val addresses = context.storage.addresses ?: return
    ExcelUtils.registerColumn(sheet, SipgateAddress::addressId, logErrorIfPropertyInfoNotFound = false)
    ExcelUtils.registerColumn(sheet, SipgateAddress::street, logErrorIfPropertyInfoNotFound = false)
    ExcelUtils.registerColumn(sheet, SipgateAddress::number, logErrorIfPropertyInfoNotFound = false)
    ExcelUtils.registerColumn(sheet, SipgateAddress::address1, logErrorIfPropertyInfoNotFound = false)
    ExcelUtils.registerColumn(sheet, SipgateAddress::address2, logErrorIfPropertyInfoNotFound = false)
    ExcelUtils.registerColumn(sheet, SipgateAddress::city, logErrorIfPropertyInfoNotFound = false)
    ExcelUtils.registerColumn(sheet, SipgateAddress::postcode, logErrorIfPropertyInfoNotFound = false)
    ExcelUtils.registerColumn(sheet, SipgateAddress::state, logErrorIfPropertyInfoNotFound = false)
    ExcelUtils.registerColumn(sheet, SipgateAddress::countrycode, logErrorIfPropertyInfoNotFound = false)
    ExcelUtils.registerColumn(sheet, SipgateAddress::numbersUrl, logErrorIfPropertyInfoNotFound = false)
    ExcelUtils.registerColumn(sheet, SipgateAddress::type, logErrorIfPropertyInfoNotFound = false)
    ExcelUtils.registerColumn(sheet, SipgateAddress::emergencyState, logErrorIfPropertyInfoNotFound = false)
    ExcelUtils.addHeadRow(sheet, context.boldStyle)
    addresses.forEach { sheet.createRow().autoFillFromObject(it) }
    sheet.setAutoFilter()
  }
}
