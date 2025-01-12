package org.projectforge.common.html

enum class CssClass(val cls: String) {
    ERROR("error"),
    WARNING("warning"),
    BOLD("bold"),

    /**
     * Fixed width for th and td (minimal with, nowrap).
     */
    FIXED_WIDTH_NO_WRAP("fixed-width-no-wrap"),
    /**
     * Expand for th and td (minimal with, nowrap).
     */
    EXPAND("expand"),
}
