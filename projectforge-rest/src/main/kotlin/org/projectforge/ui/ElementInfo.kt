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

import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType

class ElementInfo(val propertyName: String,
                  propertyField: Field? = null,
                  propertyType: Class<*>? = null,
                  var maxLength: Int? = null,
                  var required: Boolean? = null,
                  /**
                   * Getter methods without fields are normally read-only fields.
                   */
                  var readOnly: Boolean = false,
                  var i18nKey: String? = null,
                  var additionalI18nKey: String? = null,
                  /**
                   * For nested properties, the property where this is nested in.
                   */
                  var parent: ElementInfo? = null) {

    var propertyType: Class<*> = String::class.java

    private var propertyField: Field? = null
        set(value) {
            field = value
            genericType = null
            if (value != null) {
                propertyType = value.type
                val type = value.genericType
                if (type is ParameterizedType) {
                    val typeArg = type.actualTypeArguments[0]
                    if (typeArg is Class<*>) {
                        genericType = typeArg
                    }
                }
            }
        }

    init {
        this.propertyField = propertyField
        if (propertyType != null)
            this.propertyType = propertyType
    }

    /**
     * Property name without parent names if nested, otherwise equals to [propertyName]
     */
    val simplePropertyName
        get() = if (propertyName.contains('.')) propertyName.substring(propertyName.lastIndexOf('.') + 1) else propertyName

    var genericType: Class<*>? = null
        private set
}
