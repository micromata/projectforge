package org.projectforge.ui

data class UICheckbox(val id: String,
                      val label: String? = null) : UIElement(UIElementType.CHECKBOX)
