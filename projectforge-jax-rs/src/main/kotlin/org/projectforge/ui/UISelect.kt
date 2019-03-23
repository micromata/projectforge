package org.projectforge.ui

import com.google.gson.annotations.SerializedName
import org.projectforge.common.i18n.I18nEnum

data class UISelect(val id: String,
                    @Transient
                    override val layoutSettings: LayoutSettings? = null,
                    val values: MutableList<UISelectValue> = mutableListOf(),
                    val required: Boolean? = null,
                    override var label: String? = null,
                    @SerializedName("additional-label")
                    override var additionalLabel: String? = null,
                    override var tooltip: String? = null)
    : UIElement(UIElementType.SELECT), UILabelledElement {
    @Transient
    private val log = org.slf4j.LoggerFactory.getLogger(LayoutUtils::class.java)


    fun buildValues(i18nEnum: Class<out Enum<*>>) : UISelect {
        getEnumValues(i18nEnum).forEach { value ->
            if (value is I18nEnum) {
                val translation = translate(value.i18nKey)
                add(UISelectValue(value.name, translation))
            } else {
                log.error("UISelect supports only enums of type I18nEnum, not '${value}': '${this}'")
            }
        }
        return this
    }

    fun add(selectValue: UISelectValue): UISelect {
        values.add(selectValue)
        return this
    }

    // fun getEnumValues(enumClass: KClass<out Enum<*>>): Array<out Enum<*>> = enumClass.java.enumConstants
    private fun getEnumValues(enumClass: Class<out Enum<*>>): Array<out Enum<*>> = enumClass.enumConstants
}
