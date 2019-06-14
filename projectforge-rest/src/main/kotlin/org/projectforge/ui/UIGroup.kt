/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.ui

/**
 * A group represents a group of UI elements, such as a pair of a Label and an Input field.
 */
data class UIGroup(val content: MutableList<UIElement> = mutableListOf()) : UIElement(UIElementType.GROUP) {
    @Transient
    private val log = org.slf4j.LoggerFactory.getLogger(UIGroup::class.java)

    fun add(element: UIElement): UIGroup {
        content.add(element)
        return this
    }

    /**
     * Convenient method:
     * Adds a UILabel and the given element. UILabel.labelFor is set with id of given element.
     * @param label The value of the label to add as UILabel.
     * @param element The element (of type UIInput, UITextArea or UISelect).
     */
    fun add(id: String, element: UIElement?): UIGroup? {
        if (element == null)
            return null
        val label = when (element) {
            is UILabelledElement -> element.label
            else -> "??? ${id} ???"
        }
        return add(UILabel(label = label), element)
    }

    /**
     * Convenient method:
     * Adds a UILabel and the given element. UILabel.labelFor is set with id of given element.
     * @param label The label to add.
     * @param element The element (of type UIInput, UITextArea or UISelect).
     */
    fun add(label: UILabel, element: UIElement?): UIGroup? {
        if (element == null)
            return null
        label.reference = element
        label.labelFor =
                when (element) {
                    is UIInput -> element.id
                    is UITextArea -> element.id
                    is UISelect<*> -> element.id
                    else -> null
                }
        if (label.labelFor == null)
            log.error("Unsupported element for method add(UILabel, UIElement): '${element}'. Supported elements are UIInput, UISelect and UITextArea")
        content.add(label)
        content.add(element)
        return this;
    }
}
