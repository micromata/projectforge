package org.projectforge.ui.filter

import org.projectforge.common.i18n.I18nEnum
import org.projectforge.framework.i18n.translate
import org.projectforge.rest.core.log
import org.projectforge.ui.UISelectValue

open class UIFilterSelectElement(
        id: String,
        var multi: Boolean = true,
        var values: List<UISelectValue<String>>? = null
) : UIFilterElement(id, FilterType.SELECT) {

    fun buildValues(i18nEnum: Class<out Enum<*>>): UIFilterSelectElement {
        val newValues = mutableListOf<UISelectValue<String>>()
        i18nEnum.enumConstants.forEach { enum ->
            if (enum is I18nEnum) {
                newValues.add(UISelectValue(enum.name, translate(enum.i18nKey)))
            } else {
                log.error("UIFilterSelectElement supports only enums of type I18nEnum, not '${enum}': '${this}'")
            }
        }

        values = newValues

        return this
    }
}
