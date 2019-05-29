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
    AUFTRAG_POSITION,
    DATE,
    COST1,
    COST2,
    CUSTOMER,
    GROUP,
    KONTO,
    PROJECT,
    TASK_PATH,
    TIMESTAMP_MINUTES,
    USER
}
