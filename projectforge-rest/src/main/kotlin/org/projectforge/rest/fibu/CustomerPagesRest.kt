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

package org.projectforge.rest.fibu

import jakarta.annotation.PostConstruct
import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.KundeDao
import org.projectforge.business.fibu.KundeStatus
import org.projectforge.framework.i18n.translate
import org.projectforge.rest.config.JacksonConfiguration
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.rest.dto.Customer
import org.projectforge.ui.UILabelledElement
import org.projectforge.ui.ValidationError
import org.projectforge.ui.filter.UIFilterListElement
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The customer (Kunde) list and edit page, layout free — its list and form are hand built in
 * projectforge-next (`/next/customer`), so this carries no `createListLayout` or `createEditLayout`
 * any more. The counterpart of [OutgoingInvoiceEntityRest] and [OrderEntityRest]: only the read/write
 * path, the filter and the validation are left for the server to answer. The Wicket customer page is
 * still reachable and writes through the same [KundeDao].
 *
 * Customer favorites (`UserPrefArea.KUNDE_FAVORITE`) are deliberately not carried over — the next
 * list offers the generic saved-filter favorites instead.
 */
@RestController
@RequestMapping("${Rest.URL}/customer")
class CustomerPagesRest
    : AbstractDTOEntityRest<KundeDO, Customer, KundeDao>(
        KundeDao::class.java,
        "fibu.kunde.title") {

    @PostConstruct
    private fun postConstruct() {
        JacksonConfiguration.registerAllowedUnknownProperties(Customer::class.java, "statusAsString")
    }

    override fun transformFromDB(obj: KundeDO, editMode: Boolean): Customer {
        val kunde = Customer()
        kunde.copyFrom(obj)
        return kunde
    }

    override fun transformForDB(dto: Customer): KundeDO {
        val kundeDO = KundeDO()
        dto.copyTo(kundeDO)
        return kundeDO
    }

    /**
     * `status` is a real enum property of KundeDO, so the standard magic filter applies it by field name
     * — no [preProcessMagicFilter] entry is needed (unlike the synthetic filters of the group or user
     * list). `defaultFilter = true` puts its pill on the row without the user adding it first, the same
     * way the vacation list pins its status. See VacationPagesRest for the pattern.
     */
    override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
        elements.add(
            UIFilterListElement("status", label = translate("status"), defaultFilter = true)
                .buildValues(KundeStatus::class.java)
        )
    }

    /**
     * The customer number is the entity's user-assigned id and must be free. This is the check
     * [KundeDao.onInsert] makes (`fibu.kunde.validation.existingCustomerNr`), repeated here as a field
     * error so the hand built form marks the number field instead of only toasting the exception. Used
     * to be the `onWatchFieldsUpdate` of the removed edit layout.
     *
     * Only for a new customer: an existing one's number equals its id, so `doesNumberAlreadyExist` would
     * always find the customer itself.
     */
    override fun validate(validationErrors: MutableList<ValidationError>, dto: Customer) {
        super.validate(validationErrors, dto)
        if (dto.id == null && dto.nummer != null && baseDao.doesNumberAlreadyExist(transformForDB(dto))) {
            validationErrors.add(
                ValidationError(
                    translate("fibu.kunde.validation.existingCustomerNr"),
                    fieldId = KundeDO::nummer.name,
                )
            )
        }
    }

    override val autoCompleteSearchFields = arrayOf("name", "identifier")
}
