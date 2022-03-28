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

package org.projectforge.rest.fibu

import org.projectforge.business.fibu.EingangsrechnungDO
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractPagesRest
import org.projectforge.rest.multiselect.AbstractMultiSelectedPage
import org.projectforge.rest.multiselect.MassUpdateParameter
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/incomingInvoice")
class EingangsrechnungMultiSelectedPageRest : AbstractMultiSelectedPage() {
  override fun getTitleKey(): String {
    return "fibu.eingangsrechnung.multiselected.title"
  }

  override val pagesRestClass: Class<out AbstractPagesRest<*, *, *>>
    get() = EingangsrechnungPagesRest::class.java

  override fun fillForm(request: HttpServletRequest, layout: UILayout, data: MutableMap<String, MassUpdateParameter>) {
    val field = "iban"
    val lc = LayoutContext(EingangsrechnungDO::class.java)
    val el = LayoutUtils.buildLabelInputElement(lc, field)
    if (el is UIIdFieldIface) {
      el.id = "$field.textValue"
    }
    val param = MassUpdateParameter()
    param.checked = false
    param.textValue = "hurzel"
    data[field] = param
    layout.add(
      UIRow()
        .add(UICheckbox("$field.checked"))
        .add(el)
    )
  }
}
