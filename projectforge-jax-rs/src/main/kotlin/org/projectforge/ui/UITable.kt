package org.projectforge.ui

data class UITable(val id : String, val columns : MutableList<UITableColumn> = mutableListOf()) : UIElement(UIElementType.TABLE) {
    fun add(column: UITableColumn): UITable {
        columns.add(column)
        return this
    }

    /**
     * For adding columns with the given ids
     */
    fun add(vararg columnIds: String): UITable {
        columnIds.forEach {
            add(UITableColumn(it))
        }
        return this
    }
}