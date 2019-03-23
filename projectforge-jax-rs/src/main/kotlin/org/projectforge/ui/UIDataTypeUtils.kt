package org.projectforge.ui

import java.util.*

class UIDataTypeUtils {
    companion object {
        internal fun getDataType(elementInfo: UIElementsRegistry.ElementInfo?): UIDataType {
            if (elementInfo == null)
                return UIDataType.STRING
            return when (elementInfo.propertyType) {
                Date::class.java -> UIDataType.DATE
                else -> UIDataType.STRING
            }
        }
    }
}
