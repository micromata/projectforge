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
                  @Transient
                  override val ignoreAdditionalLabel: Boolean = false,
                  @Transient
                  override val ignoreTooltip: Boolean = false,
                  /**
                   * Optional property of value, needed by the client for mapping the data to the value. Default is "value".
                   */
                  var valueProperty: String = "id",
                  /**
                   * Optional property of label, needed by the client for mapping the data to the label. Default is "label".
                   */
                  var labelProperty: String = "displayName",
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
    private val log = org.slf4j.LoggerFactory.getLogger(UISelect::class.java)


    fun buildValues(i18nEnum: Class<out Enum<*>>): UISelect<T> {
        val newvalues = mutableListOf<UISelectValue<T>>()
        getEnumValues(i18nEnum).forEach { value ->
            if (value is I18nEnum) {
                val translation = translate(value.i18nKey)
                @Suppress("UNCHECKED_CAST")
                newvalues.add(UISelectValue(value.name as T, translation))
            } else {
                log.error("UISelect supports only enums of type I18nEnum, not '$value': '${this}'")
            }
        }
        values = newvalues
        return this
    }

    // fun getEnumValues(enumClass: KClass<out Enum<*>>): Array<out Enum<*>> = enumClass.java.enumConstants
    private fun getEnumValues(enumClass: Class<out Enum<*>>): Array<out Enum<*>> = enumClass.enumConstants

    companion object {
        fun createUserSelect(lc: LayoutContext? = null, id:String, multi: Boolean = false, label: String? = null, additionalLabel: String? = null, tooltip: String? = null,
                             showOnlyActiveUsers: Boolean = true): UISelect<Int> {
            return UISelect<Int>(id,
                    lc,
                    multi = multi,
                    label = label ?: ElementsRegistry.getElementInfo(lc, id)?.i18nKey,
                    additionalLabel = additionalLabel ?: ElementsRegistry.getElementInfo(lc, id)?.additionalI18nKey,
                    autoCompletion = AutoCompletion.getAutoCompletion4Users(showOnlyActiveUsers),
                    tooltip = tooltip ?: ElementsRegistry.getElementInfo(lc, id)?.tooltipI18nKey)
        }

        fun createGroupSelect(lc: LayoutContext,id:String, multi: Boolean, label: String? = null, additionalLabel: String? = null, tooltip: String? = null): UISelect<Int> {
            return UISelect<Int>(id,
                    lc,
                    multi = multi,
                    label = label ?: ElementsRegistry.getElementInfo(lc, id)?.i18nKey,
                    additionalLabel = additionalLabel ?: ElementsRegistry.getElementInfo(lc, id)?.additionalI18nKey,
                    autoCompletion = AutoCompletion.getAutoCompletion4Groups(),
                    tooltip = tooltip ?: ElementsRegistry.getElementInfo(lc, id)?.tooltipI18nKey)
        }

        fun createCustomerSelect(lc: LayoutContext,id:String, multi: Boolean, label: String? = null, additionalLabel: String? = null, tooltip: String? = null): UISelect<Int> {
            return UISelect<Int>(id,
                    lc,
                    multi = multi,
                    label = label ?: ElementsRegistry.getElementInfo(lc, id)?.i18nKey,
                    additionalLabel = additionalLabel ?: ElementsRegistry.getElementInfo(lc, id)?.additionalI18nKey,
                    autoCompletion = AutoCompletion.getAutoCompletion4Customers(),
                    tooltip = tooltip ?: ElementsRegistry.getElementInfo(lc, id)?.tooltipI18nKey)
        }

        fun createProjectSelect(lc: LayoutContext,id:String, multi: Boolean, label: String? = null, additionalLabel: String? = null, tooltip: String? = null): UISelect<Int> {
            return UISelect<Int>(id,
                    lc,
                    multi = multi,
                    label = label ?: ElementsRegistry.getElementInfo(lc, id)?.i18nKey,
                    additionalLabel = additionalLabel ?: ElementsRegistry.getElementInfo(lc, id)?.additionalI18nKey,
                    autoCompletion = AutoCompletion.getAutoCompletion4Projects(),
                    tooltip = tooltip ?: ElementsRegistry.getElementInfo(lc, id)?.tooltipI18nKey)
        }
    }
}
