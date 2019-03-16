package org.projectforge.rest.ui

import org.apache.xalan.xsltc.runtime.CallFunction.clazz
import org.projectforge.framework.persistence.api.HibernateUtils
import org.projectforge.ui.UIElement
import org.projectforge.ui.UIInput
import org.projectforge.ui.UITextarea

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
                }
            }
        }

        private fun getMaxLength(clazz: Class<*>, current: Int?, property: String, element: UIElement): Int? {
            if (current == null || current != 0) return null;
            val maxLength = HibernateUtils.getPropertyLength(clazz, property)
            if (maxLength != null) {
                return maxLength
            } else {
                log.error("Length not found in Entity '${clazz}' for UIInput '${element}'.")
                return null;
            }
        }
    }
}