package org.projectforge.ui

data class UICheckbox(val id: String,
                      var label: String? = null,
                      var tooltip: String? = null) : UIElement(UIElementType.CHECKBOX)
