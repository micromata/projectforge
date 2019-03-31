package org.projectforge.ui

import com.google.gson.annotations.SerializedName

enum class UIButtonStyle {
    @SerializedName("danger")
    DANGER,
    @SerializedName("info")
    INFO,
    @SerializedName("link")
    LINK,
    @SerializedName("primary")
    PRIMARY,
    @SerializedName("secondary")
    SECONDARY,
    @SerializedName("success")
    SUCCESS,
    @SerializedName("warning")
    WARNING
}
