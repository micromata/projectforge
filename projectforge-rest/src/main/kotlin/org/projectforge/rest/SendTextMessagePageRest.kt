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

package org.projectforge.rest

import org.projectforge.business.address.AddressDao
import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.framework.i18n.translate
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.sms.SmsSenderConfig
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Rest.URL}/sendTextMessage")
class SendTextMessagePageRest {
    @Autowired
    private lateinit var configurationService: ConfigurationService

    @Autowired
    private lateinit var smsSenderConfig: SmsSenderConfig

    @Autowired
    private lateinit var addressDao: AddressDao

    class Data(var message: String? = null,
               var phoneNumber: String? = null)

    @GetMapping("dynamic")
    fun getForm(): FormLayoutData {
        val layout = UILayout("address.sendSms.title")
        val lc = LayoutContext(Data::class.java)

        val buttonCol = UICol(length = 6)
        buttonCol.add(UIButton("send", translate("send"),
                UIColor.SUCCESS,
                responseAction = ResponseAction(PagesResolver.getEditPageUrl(VacationPagesRest::class.java))))
        val numberField =  UISelect<String>("cellPhoneNumber", lc,
                label = translate( "address.sendSms.phoneNumber"),
                tooltip = translate("address.sendSms.phoneNumber.info"),
                autoCompletion = AutoCompletion<String>(url = "address/acLang?search=:search"))
        layout.add(UIFieldset(length = 12)
                .add(UIRow()
                        .add(UICol(mdLength = 6, smLength = 12)
                                .add(numberField)
                        .add(UICol(mdLength = 6, smLength = 12)
                                .add(UITextArea("message", lc, label = translate( "address.sendSms.message"))))))
                .add(UIRow().add(buttonCol)))

        return FormLayoutData(null, layout, null)
    }
}
