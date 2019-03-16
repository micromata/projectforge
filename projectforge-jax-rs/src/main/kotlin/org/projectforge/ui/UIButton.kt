package org.projectforge.ui

data class UIButton(val id : String, open var title : String, val style : UIButtonStyle? = null) : UIElement(UIElementType.BUTTON)