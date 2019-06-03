package org.projectforge.ui

import com.fasterxml.jackson.annotation.JsonProperty

enum class UIStyle {
    @JsonProperty("danger")
    DANGER,
    @JsonProperty("info")
    INFO,
    @JsonProperty("link")
    LINK,
    @JsonProperty("primary")
    PRIMARY,
    @JsonProperty("secondary")
    SECONDARY,
    @JsonProperty("success")
    SUCCESS,
    @JsonProperty("warning")
    WARNING
}
