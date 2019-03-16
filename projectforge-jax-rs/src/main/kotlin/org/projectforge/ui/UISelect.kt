package org.projectforge.ui

import org.projectforge.common.i18n.I18nEnum

data class UISelect(val id: String,
                    val values: MutableList<UISelectValue> = mutableListOf(),
                    val required: Boolean? = null,
                    @Transient
                    val i18nEnum: Class<out Enum<*>>? = null)
    : UIElement(UIElementType.SELECT) {

    fun add(selectValue: UISelectValue): UISelect {
        values.add(selectValue)
        return this
    }
}
