package org.projectforge.rest.ui

import org.projectforge.business.excel.ExportConfig
import org.projectforge.common.i18n.I18nEnum
import org.projectforge.common.props.PropUtils
import org.projectforge.framework.persistence.api.HibernateUtils
import org.projectforge.ui.*

fun translate(i18nKey: String?): String {
    if (i18nKey == null) return "???"
    return ExportConfig.getInstance().getDefaultExportContext().getLocalizedString(i18nKey)
}

class LayoutUtils {

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(LayoutUtils::class.java)

        fun process(layout: UILayout, clazz: Class<*>): UILayout {
            processAllElements(layout.getAllElements(), clazz)
            layout.title = translate(layout.title)
            return layout
        }

        /**
         * Sets all length of input fields and text areas with maxLength 0 to the Hibernate JPA definition (@Column).
         * @see HibernateUtils.getPropertyLength
         */
        private fun processAllElements(elements: List<Any>, clazz: Class<*>) {
            elements.forEach {
                when (it) {
                    is UIInput -> {
                        val maxLength = getMaxLength(clazz, it.maxLength, it.id, it)
                        if (maxLength != null) it.maxLength = maxLength
                    }
                    is UITextarea -> {
                        val maxLength = getMaxLength(clazz, it.maxLength, it.id, it)
                        if (maxLength != null) it.maxLength = maxLength
                    }
                    is UILabel -> {
                        if (it.value == "@") {
                            val translation = getI18nKey(clazz, it.value, getProperty(it.reference), it)
                            if (translation != null) it.value = translation
                        }
                    }
                    is UISelect -> {
                        if (it.i18nEnum != null) {
                            getEnumValues(it.i18nEnum).forEach { value ->
                                if (value is I18nEnum) {
                                    val translation = translate(value.i18nKey)
                                    it.add(UISelectValue(value.name, translation))
                                } else {
                                    log.error("UISelect supports only enums of type I18nEnum, not '${value}': '${it}'")
                                }
                            }
                        }
                    }
                    is UIButton -> {
                        if (it.title == "@") {
                            val i18nKey = when (it.id) {
                                "cancel" -> "cancel"
                                "markAsDeleted" -> "markAsDeleted"
                                "reset" -> "reset"
                                "search" -> "search"
                                "undelete" -> "undelete"
                                "update" -> "save"
                                else -> null
                            }
                            if (it.title != null) it.title = translate(i18nKey!!)
                        }
                    }
                }
            }
        }

        // fun getEnumValues(enumClass: KClass<out Enum<*>>): Array<out Enum<*>> = enumClass.java.enumConstants
        private fun getEnumValues(enumClass: Class<out Enum<*>>): Array<out Enum<*>> = enumClass.enumConstants

        private fun getMaxLength(clazz: Class<*>, current: Int?, property: String, element: UIElement): Int? {
            if (current != 0) return null;
            val maxLength = HibernateUtils.getPropertyLength(clazz, property)
            if (maxLength == null) {
                log.error("Length not found in Entity '${clazz}' for UI element '${element}'.")
            }
            return maxLength
        }

        private fun getI18nKey(clazz: Class<*>, current: String?, property: String?, element: UIElement): String? {
            if (current != "@") return null;
            val propInfo = PropUtils.get(clazz, property)
            if (propInfo == null || propInfo.i18nKey == null) {
                log.error("PropertyInfo not found in Entity '${clazz}' for UI element '${element}'.")
                return null
            }
            return translate(propInfo.i18nKey)
        }

        private fun getProperty(element: UIElement?): String? {
            if (element == null) return null
            return when (element) {
                is UIInput -> element.id
                is UISelect -> element.id
                is UITextarea -> element.id
                else -> null
            }
        }

    }
}