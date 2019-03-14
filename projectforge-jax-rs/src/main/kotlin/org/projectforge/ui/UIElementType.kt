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
    @SerializedName("checkbox")
    CHECKBOX
}
