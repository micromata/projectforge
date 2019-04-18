package org.projectforge.ui

import org.projectforge.common.i18n.I18nEnum
import org.projectforge.framework.i18n.translate

data class UISelect<T>(val id: String,
                    @Transient
                    override val layoutSettings: LayoutContext? = null,
                    val values: MutableList<UISelectValue<T>> = mutableListOf(),
                    val required: Boolean? = null,
                    override var label: String? = null,
                    override var additionalLabel: String? = null,
                    override var tooltip: String? = null)
    : UIElement(UIElementType.SELECT), UILabelledElement {
    @Transient
    private val log = org.slf4j.LoggerFactory.getLogger(LayoutUtils::class.java)


    fun buildValues(i18nEnum: Class<out Enum<*>>) : UISelect<T> {
        getEnumValues(i18nEnum).forEach { value ->
            if (value is I18nEnum) {
                val translation = translate(value.i18nKey)
                add(UISelectValue<T>(value.name as T, translation))
            } else {
                log.error("UISelect supports only enums of type I18nEnum, not '${value}': '${this}'")
            }
        }
        return this
    }

    fun add(selectValue: UISelectValue<T>): UISelect<T> {
        values.add(selectValue)
        return this
    }

    // fun getEnumValues(enumClass: KClass<out Enum<*>>): Array<out Enum<*>> = enumClass.java.enumConstants
    private fun getEnumValues(enumClass: Class<out Enum<*>>): Array<out Enum<*>> = enumClass.enumConstants
}
