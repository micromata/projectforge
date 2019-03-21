package org.projectforge.ui

data class UIButton(val id : String,
                    /** May be null for standard buttons. For standard buttons the title will be set dependent on the id. */
                    var title : String? = null,
                    val style : UIButtonStyle? = null)
    : UIElement(UIElementType.BUTTON)