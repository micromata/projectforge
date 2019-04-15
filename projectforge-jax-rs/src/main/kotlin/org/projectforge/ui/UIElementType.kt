package org.projectforge.ui

import com.google.gson.annotations.SerializedName

enum class UIElementType {
    @SerializedName("group")
    GROUP,
    @SerializedName("row")
    ROW,
    @SerializedName("col")
    COL,
    @SerializedName("label")
    LABEL,
    @SerializedName("textarea")
    TEXTAREA,
    @SerializedName("input")
    INPUT,
    @SerializedName("select")
    SELECT,
    @SerializedName("multi-select")
    MULTI_SELECT,
    @SerializedName("checkbox")
    CHECKBOX,
    @SerializedName("table")
    TABLE,
    @SerializedName("table-column")
    TABLE_COLUMN,
    @SerializedName("button")
    BUTTON,
    @SerializedName("named-container")
    NAMED_CONTAINER,
    @SerializedName("customized")
    CUSTOMIZED
}
