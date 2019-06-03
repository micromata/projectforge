package org.projectforge.ui

/**
 * Base class of most UI elements.
 */
open class UIElement(val type: UIElementType,
                     /**
                      * The key is an unique id, used e. g. by React for lists.
                      */
                     var key : String? = null,
                     var cssClass : String? = null)