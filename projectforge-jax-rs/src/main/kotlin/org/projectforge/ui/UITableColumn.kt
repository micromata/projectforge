package org.projectforge.ui

data class UITableColumn(val id: String,
                         var title: String? = null,
                         val dataType: UIDataType = UIDataType.STRING,
                         val sortable: Boolean = true)
    : UIElement(UIElementType.TABLE_COLUMN)