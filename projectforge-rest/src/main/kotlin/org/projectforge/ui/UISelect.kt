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

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.projectforge.common.i18n.I18nEnum
import org.projectforge.framework.i18n.translate
import org.projectforge.rest.json.UISelectTypeSerializer

@JsonSerialize(using = UISelectTypeSerializer::class)
class UISelect<T>(val id: String,
                  @Transient
                  override val layoutContext: LayoutContext? = null,
                  var values: List<UISelectValue<T>>? = null,
                  val required: Boolean? = null,
                  /**
                   * Multiple values supported?
                   */
                  val multi: Boolean? = null,
                  override var label: String? = null,
                  override var additionalLabel: String? = null,
                  override var tooltip: String? = null,
                  /**
                   * Optional property of value, needed by the client for mapping the data to the value. Default is "value".
                   */
                  var valueProperty: String = "value",
                  /**
                   * Optional property of label, needed by the client for mapping the data to the label. Default is "label".
                   */
                  var labelProperty: String = "label",
                  /**
                   * The recent or favorite entries, if given, will be shown as favorites for quick select
                   * (in rest client as star beside the select input).
                   */
                  var favorites: List<Favorite<T>>? = null,
                  var autoCompletion: AutoCompletion<*>? = null,
                  key: String? = null,
                  cssClass: String? = null)
    : UIElement(UIElementType.SELECT, key = key, cssClass = cssClass), UILabelledElement {

    class Favorite<T>(val id: T, val name: String)

    @Transient
    private val log = org.slf4j.LoggerFactory.getLogger(LayoutUtils::class.java)


    fun buildValues(i18nEnum: Class<out Enum<*>>): UISelect<T> {
        val newvalues = mutableListOf<UISelectValue<T>>()
        getEnumValues(i18nEnum).forEach { value ->
            if (value is I18nEnum) {
                val translation = translate(value.i18nKey)
                @Suppress("UNCHECKED_CAST")
                newvalues.add(UISelectValue(value.name as T, translation))
            } else {
                log.error("UISelect supports only enums of type I18nEnum, not '${value}': '${this}'")
            }
        }
        values = newvalues
        return this
    }

    // fun getEnumValues(enumClass: KClass<out Enum<*>>): Array<out Enum<*>> = enumClass.java.enumConstants
    private fun getEnumValues(enumClass: Class<out Enum<*>>): Array<out Enum<*>> = enumClass.enumConstants
}
