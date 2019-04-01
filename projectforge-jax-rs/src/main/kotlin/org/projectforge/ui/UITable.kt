package org.projectforge.ui

data class UITable(val id : String, val columns : MutableList<UITableColumn> = mutableListOf()) : UIElement(UIElementType.TABLE) {
    companion object {
       fun UIResultSetTable() : UITable {
           return UITable("resultSet")
       }
    }

    fun add(column: UITableColumn): UITable {
        columns.add(column)
        return this
    }

    /**
     * For adding columns with the given ids
     */
    fun add(layoutSettings: LayoutContext, vararg columnIds: String): UITable {
        columnIds.forEach {
            val col = UITableColumn(it)
            col.protectTitle = true
            val elementInfo = ElementsRegistry.getElementInfo(layoutSettings.dataObjectClazz, it)
            if (elementInfo != null) {
                col.title = elementInfo.i18nKey
                col.dataType = UIDataTypeUtils.getDataType(elementInfo)
            }
            add(col)
        }
        return this
    }
}