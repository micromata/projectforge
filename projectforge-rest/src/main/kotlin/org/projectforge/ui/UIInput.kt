/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.ui

import org.projectforge.framework.i18n.InternalErrorException
import org.projectforge.rest.core.AbstractBaseRest
import org.springframework.util.ClassUtils

data class UIInput(val id: String,
                   @Transient
                   override val layoutContext: LayoutContext? = null,
                   var maxLength: Int? = null,
                   var required: Boolean? = null,
                   var focus: Boolean? = null,
                   var dataType: UIDataType = UIDataType.STRING,
                   override var label: String? = null,
                   override var additionalLabel: String? = null,
                   override var tooltip: String? = null)
    : UIElement(UIElementType.INPUT), UILabelledElement {
    var autoCompletionUrl: String? = null

    /**
     * Please note: Only enabled properties in [BaseDao] are available due to security reasons.
     * @return this for chaining.
     * @see BaseDao.isAutocompletionPropertyEnabled
     */
    fun enableAutoCompletion(services: AbstractBaseRest<*, *, *, *>):UIInput {
        if (!services.isAutocompletionPropertyEnabled(id)) {
            throw InternalErrorException("Development error: You must enable autocompletion properties explicit in '${ClassUtils.getUserClass(services.baseDao).simpleName}.isAutocompletionPropertyEnabled(String)' for property '$id' for security resasons first.")
        }
        autoCompletionUrl = "${services.getRestPath()}/ac?property=${id}&search="
        return this
    }
}
