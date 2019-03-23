package org.projectforge.ui

import org.apache.xalan.xsltc.runtime.CallFunction.clazz

data class UITable(val id : String, val columns : MutableList<UITableColumn> = mutableListOf()) : UIElement(UIElementType.TABLE) {
    fun add(column: UITableColumn): UITable {
        columns.add(column)
        return this
    }

    /**
     * For adding columns with the given ids
     */
    fun add(layoutSettings: LayoutSettings, vararg columnIds: String): UITable {
        columnIds.forEach {
            val col = UITableColumn(it)
            col.protectTitle = true
            val elementInfo = UIElementsRegistry.getElementInfo(layoutSettings.dataObjectClazz, it)
            if (elementInfo != null) {
                val translation = LayoutUtils.getLabelTransformation(elementInfo?.i18nKey)
                if (translation != null) col.title = translation
                col.dataType = UIDataTypeUtils.getDataType(elementInfo)
            }
            add(col)
        }
        return this
    }
}