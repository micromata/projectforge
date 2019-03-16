package org.projectforge.rest.ui

import org.projectforge.common.props.PropUtils
import org.projectforge.framework.persistence.api.HibernateUtils
import org.projectforge.ui.*

class LayoutUtils {

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(LayoutUtils::class.java)

        /**
         * Sets all length of input fields and text areas with maxLength 0 to the Hibernate JPA definition (@Column).
         * @see HibernateUtils.getPropertyLength
         */
        fun processAllElements(elements: List<Any>, clazz: Class<*>) {
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
                        if (it.value == ".") {
                            val translation = getI18nKey(clazz, it.value, getProperty(it.reference), it)
                            if (translation != null) it.value = translation
                        }
                    }
                }
            }
        }

        private fun getMaxLength(clazz: Class<*>, current: Int?, property: String, element: UIElement): Int? {
            if (current == null || current != 0) return null;
            val maxLength = HibernateUtils.getPropertyLength(clazz, property)
            if (maxLength != null) {
                return maxLength
            } else {
                log.error("Length not found in Entity '${clazz}' for UI element '${element}'.")
                return null;
            }
        }

        private fun getI18nKey(clazz: Class<*>, current: String?, property: String?, element: UIElement): String? {
            if (current != ".") return null;
            val propInfo = PropUtils.get(clazz, property)
            if (propInfo == null || propInfo.i18nKey == null) {
                log.error("PropertyInfo not found in Entity '${clazz}' for UI element '${element}'.")
                return null
            }
            return propInfo.i18nKey
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