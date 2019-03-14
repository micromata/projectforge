package org.projectforge.ui

import com.google.gson.annotations.SerializedName

enum class UIElementType {
    @SerializedName("group")
    GROUP {
        override fun contentAllowed() = true
    },
    @SerializedName("row")
    ROW {
        override fun contentAllowed() = true
    },
    @SerializedName("col")
    COL {
        override fun contentAllowed() = true
    },
    @SerializedName("label")
    LABEL {
        override fun contentAllowed() = false
    },
    @SerializedName("text")
    TEXT {
        override fun contentAllowed() = false
    },
    @SerializedName("select")
    SELECT {
        override fun contentAllowed() = false
    },
    @SerializedName("checkbox")
    CHECKBOX {
        override fun contentAllowed() = false
    };

    abstract fun contentAllowed(): Boolean
}
