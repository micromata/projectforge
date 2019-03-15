package org.projectforge.ui

data class UITable(val id : String, val columns : MutableList<UITableColumn> = mutableListOf()) : UIElement(UIElementType.TABLE) {
    fun add(column: UITableColumn): UITable {
        columns.add(column)
        return this
    }
}