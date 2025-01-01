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

package org.projectforge.plugins.marketing

import de.micromata.merlin.excel.ExcelRow
import de.micromata.merlin.excel.ExcelSheet
import mu.KotlinLogging
import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressExport
import org.projectforge.business.address.PersonalAddressDO
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * For excel export. export must be called with two params, the first is the AddressCampaignValue map and the second the
 * title of the address campaign.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service("addressCampaignValueExport")
open class AddressCampaignValueExport : AddressExport() {
  override val addressCampaignSupported = true

  override fun export(
    origList: List<AddressDO>,
    personalAddressMap: Map<Long, PersonalAddressDO>,
    vararg params: Any
  ): ByteArray? {
    log.info { "Exporting campaign '${params[1]}'..." }
    return super.export(origList, personalAddressMap, *params)
  }

  override fun registerAdditionalCols(sheet: ExcelSheet) {
    register(sheet, "value", "campaignValue")
    register(sheet, "comment", "campaignComment")
  }

  override fun handleAddressCampaign(row: ExcelRow, address: AddressDO, vararg params: Any) {
    @Suppress("UNCHECKED_CAST")
    val addressCampaignValue = (params[0] as Map<Long, AddressCampaignValueDO>).get(address.id)
    row.getCell("campaignValue")?.setCellValue(addressCampaignValue?.value)
    row.getCell("campaignComment")?.setCellValue(addressCampaignValue?.comment)
  }

  override val sheetTitle = "plugins.marketing.addressCampaign"

  override fun configureSheet(sheet: ExcelSheet, vararg params: Any) {
    sheet.setMergedRegion(
      0, 0,
      sheet.getColNumber("campaignValue")!!,
      sheet.getColNumber("campaignComment")!!,
      params[1]
    )
  }
}
