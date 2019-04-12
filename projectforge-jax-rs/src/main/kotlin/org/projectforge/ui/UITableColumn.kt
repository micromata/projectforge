package org.projectforge.ui

data class UITableColumn(var id: String,
                         var title: String? = null,
                         @Transient
                         var protectTitle: Boolean = false,
                         var dataType: UIDataType = UIDataType.STRING,
                         var sortable: Boolean = true,
                         var formatter: Formatter? = null)
    : UIElement(UIElementType.TABLE_COLUMN)

enum class Formatter {
    TIMESTAMP_MINUTES,
    DATE,
    USER
}
