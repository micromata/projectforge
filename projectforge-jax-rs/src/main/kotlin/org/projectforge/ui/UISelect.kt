package org.projectforge.ui

import org.projectforge.common.i18n.I18nEnum
import org.projectforge.framework.i18n.translate

class UISelect<T>(val id: String,
                  @Transient
                  override val layoutSettings: LayoutContext? = null,
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
                  var autoCompletion: AutoCompletion<*>? = null,
                  key: String? = null,
                  cssClass: String? = null)
    : UIElement(UIElementType.SELECT, key = key, cssClass = cssClass), UILabelledElement {
    @Transient
    private val log = org.slf4j.LoggerFactory.getLogger(LayoutUtils::class.java)


    fun buildValues(i18nEnum: Class<out Enum<*>>): UISelect<T> {
        val newvalues = mutableListOf<UISelectValue<T>>()
        getEnumValues(i18nEnum).forEach { value ->
            if (value is I18nEnum) {
                val translation = translate(value.i18nKey)
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
