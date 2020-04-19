/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.KundeDao
import org.projectforge.framework.i18n.translate
import org.projectforge.rest.config.JacksonConfiguration
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.dto.Customer
import org.projectforge.rest.dto.Konto
import org.projectforge.ui.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.annotation.PostConstruct
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/customer")
class CustomerPagesRest
    : AbstractDTOPagesRest<KundeDO, Customer, KundeDao>(
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
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val layout = super.createListLayout()
                .add(UITable.createUIResultSetTable()
                        .add(UITableColumn("kost", title = "fibu.kunde.nummer"))
                        .add(lc, "identifier", "name", "division", "konto", "statusAsString", "description"))
        layout.getTableColumnById("konto").formatter = Formatter.KONTO
        return LayoutUtils.processListPage(layout, this)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: Customer, userAccess: UILayout.UserAccess): UILayout {
        val nameField = UIInput("name", lc, focus = true)
        val kontoField = UISelect<Konto>("konto", lc,
                autoCompletion = AutoCompletion<Konto>(url = "account/acDebitors?search=:search"))
        val numberField: UIElement = if (dto.nummer != null) {
            UIReadOnlyField("nummer", lc)
        } else {
            UIInput("nummer", lc)
        }

        val layout = super.createEditLayout(dto, userAccess)
                .add(UIRow()
                        .add(UICol()
                                .add(UILabel("'ToDo: Kontoselektion, Errors as return value of watchfields *************** (Status unfinished) ***************"))
                                .add(numberField)
                                .add(nameField)
                                .add(kontoField)
                                .add(lc, "identifier", "division", "description", "status")))

        if (dto.nummer == null) {
            layout.watchFields.addAll(arrayOf("nummer"))
        }

        return LayoutUtils.processEditPage(layout, dto, this)
    }

    override fun onWatchFieldsUpdate(request: HttpServletRequest, dto: Customer, watchFieldsTriggered: Array<String>?): ResponseEntity<ResponseAction> {
        if (watchFieldsTriggered?.contains("nummer") == true) {
            dto.nummer?.let {
                if (baseDao.doesNumberAlreadyExist(transformForDB(dto))) {
                    val error = ValidationError(translate("fibu.kunde.validation.existingCustomerNr"))
                    return ResponseEntity(ResponseAction(validationErrors = listOf(error)), HttpStatus.NOT_ACCEPTABLE)
                }
            }
        }
        return super.onWatchFieldsUpdate(request, dto, watchFieldsTriggered)
    }

    override val autoCompleteSearchFields = arrayOf("name", "identifier")
}
