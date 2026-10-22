/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.framework.i18n.addTranslations

/**
 * Helpers for the elements built from the entities' property infos (see [ElementsRegistry]): the filter
 * fields of the list pages and the field metadata of the mass updates.
 */
object LayoutUtils {

    @JvmStatic
    fun addCommonTranslations(translations: MutableMap<String, String>) {
        addTranslations(
            "calendar.today", // Used for date picker.
            "cancel",
            "finish", // Color picker
            "save", // Color picker
            "select.placeholder",
            "yes",
            translations = translations,
        )
    }

    /**
     * @param layoutContext One element is returned including the label (e. g. UIInput).
     * @param minLengthOfTextArea For text fields longer than minLengthOfTextArea, a UITextArea is used instead of UIInput.
     *                            Default length is [DEFAULT_MIN_LENGTH_OF_TEXT_AREA], meaning fields with max length of more
     *                            than [DEFAULT_MIN_LENGTH_OF_TEXT_AREA] will be displayed as TextArea.
     */
    internal fun buildLabelInputElement(
        layoutContext: LayoutContext,
        id: String,
        minLengthOfTextArea: Int = DEFAULT_MIN_LENGTH_OF_TEXT_AREA,
    ): UIElement {
        return ElementsRegistry.buildElement(layoutContext, id, minLengthOfTextArea)
    }

    /**
     * @param createRowCol If true, a new [UIRow] containing a new [UICol] with the given element is returned,
     * otherwise the element itself without any other operation.
     * @return The element itself or the surrounding [UIRow].
     */
    internal fun prepareElementToAdd(element: UIElement, createRowCol: Boolean): UIElement {
        return if (createRowCol) {
            val row = UIRow()
            val col = UICol()
            row.add(col)
            col.add(element)
            row
        } else {
            element
        }
    }

    internal fun setLabels(elementInfo: ElementInfo?, element: UILabelledElement) {
        if (elementInfo == null)
            return
        if (!elementInfo.i18nKey.isNullOrEmpty())
            element.label = elementInfo.i18nKey
        if (!elementInfo.additionalI18nKey.isNullOrEmpty() && !element.ignoreAdditionalLabel)
            element.additionalLabel = elementInfo.additionalI18nKey
        if (!elementInfo.tooltipI18nKey.isNullOrEmpty() && !element.ignoreTooltip)
            element.tooltip = elementInfo.tooltipI18nKey
    }

    /**
     * @return The id of the given element if supported.
     */
    internal fun getId(element: UIElement?, followLabelReference: Boolean = true): String? {
        if (element == null) return null
        if (followLabelReference && element is UILabel) {
            return getId(element.reference)
        }
        return when (element) {
            is UIInput -> element.id
            is UICheckbox -> element.id
            is UIReadOnlyField -> element.id
            is UISelect<*> -> element.id
            is UITextArea -> element.id
            else -> null
        }
    }

    const val DEFAULT_MIN_LENGTH_OF_TEXT_AREA = 256
}
