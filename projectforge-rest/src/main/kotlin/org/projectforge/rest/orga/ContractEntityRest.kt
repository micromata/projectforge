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

import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.business.orga.ContractDO
import org.projectforge.business.orga.ContractDao
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.time.PFDay
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.rest.config.JacksonConfiguration
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.dto.Contract
import org.projectforge.ui.UILabelledElement
import org.projectforge.ui.ValidationError
import org.projectforge.ui.filter.UIFilterElement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest

/**
 * The contracts, for the hand built list and form of projectforge-next (components/features/contract). Serves no
 * layout: the React pages it replaced are gone.
 */
@RestController
@RequestMapping("${Rest.URL}/contract")
class ContractEntityRest
  : AbstractDTOEntityRest<ContractDO, Contract, ContractDao>(
  ContractDao::class.java, "legalAffaires.contract.title",
  cloneSupport = CloneSupport.CLONE
) {

  /** A contract type of the configuration parameter `orga.contractTypes`, as the form offers it. */
  class ContractTypeOption(val value: String, val label: String)

  @Autowired
  private lateinit var configurationService: ConfigurationService

  @PostConstruct
  private fun postConstruct() {
    /**
     * Enable attachments for this entity.
     */
    enableAttachments()
    JacksonConfiguration.registerAllowedUnknownProperties(Contract::class.java, "statusAsString")
  }

  /**
   * Initializes new contracts for adding.
   */
  override fun newBaseDO(request: HttpServletRequest?): ContractDO {
    val contract = super.newBaseDO(request)
    contract.date = LocalDate.now()
    return contract
  }

  override fun transformForDB(dto: Contract): ContractDO {
    val contractDO = ContractDO()
    dto.copyTo(contractDO)
    return contractDO
  }

  override fun transformFromDB(obj: ContractDO, editMode: Boolean): Contract {
    val contract = Contract()
    contract.copyFrom(obj)
    return contract
  }

  override fun prepareClone(dto: Contract): Contract {
    dto.number = null
    return super.prepareClone(dto)
  }

  override fun validate(validationErrors: MutableList<ValidationError>, dto: Contract) {
    val date = PFDay.fromOrNull(dto.date)
    if (date != null && PFDay.now().isBefore(date)) { // No dates in the future accepted.
      validationErrors.add(ValidationError(translate("error.dateInFuture"), fieldId = "date"))
    }
  }

  /**
   * The year filter of the former React list, still offered (but not pinned): a stored filter of a user may carry it,
   * and next restores and sends every stored entry, so without the element the list would be narrowed to a year
   * nobody can see or remove. Contracts are looked for by the period filter of the date column instead.
   */
  override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
    elements.add(UIFilterElement("year", label = translate("calendar.year")))
  }

  /**
   * The year filter of the former React list. The next list filters the date column by a period instead, but a
   * `year` entry may still be stored in the filter of a user, so it is still understood (see [addMagicFilterElements]).
   */
  override fun preProcessMagicFilter(target: QueryFilter, source: MagicFilter): List<CustomResultFilter<ContractDO>>? {
    source.entries.find { it.field == "year" }?.let { entry ->
      entry.synthetic = true
      NumberHelper.parseInteger(entry.plainValue)?.let { year ->
        target.setYearAndMonth("date", year, -1)
      }
    }
    return null
  }

  /**
   * The contract types to choose from (configuration parameter `orga.contractTypes`), empty if none are configured.
   */
  @AccessChecked("DAO: select access (ContractDao.hasLoggedInUserSelectAccess with throwException)")
  @GetMapping("types")
  fun getTypes(): List<ContractTypeOption> {
    baseDao.hasLoggedInUserSelectAccess(throwException = true)
    return configurationService.contractTypes.mapNotNull { type ->
      val value = type.value?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
      ContractTypeOption(value, type.label?.takeIf { it.isNotBlank() } ?: value)
    }
  }
}
