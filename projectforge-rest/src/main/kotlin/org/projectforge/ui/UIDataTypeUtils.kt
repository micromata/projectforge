package org.projectforge.ui

import java.util.*

class UIDataTypeUtils {
    companion object {
        internal fun getDataType(elementInfo: ElementsRegistry.ElementInfo?): UIDataType {
            if (elementInfo == null)
                return UIDataType.STRING
            return when (elementInfo.propertyType) {
                Date::class.java -> UIDataType.DATE
                Locale::class.java -> UIDataType.LOCALE
                else -> UIDataType.STRING
            }
        }
    }
}
