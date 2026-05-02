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

/**
 * Special radio button component for Poll plugin that handles Boolean array values.
 * 
 * Unlike UIRadioButton which expects single values and uses radio button grouping,
 * this component is designed specifically for Poll questions where:
 * - Each answer option has its own Boolean value in an array
 * - Single-choice questions need visual radio button behavior
 * - The data structure uses Boolean arrays (not single values)
 * 
 * This component works with the DynamicPollRadioButton.jsx frontend component
 * which properly handles Boolean array values and radio button grouping.
 */
data class UIPollRadioButton(val id: String,
                             val value: Any,
                             val name: String = id,
                             @Transient
                             override val layoutContext: LayoutContext? = null,
                             override var tooltip: String? = null,
                             override var label: String? = null,
                             override var additionalLabel: String? = null,
                             @Transient
                             override val ignoreAdditionalLabel: Boolean = false,
                             @Transient
                             override val ignoreTooltip: Boolean = false)
    : UIElement(UIElementType.POLL_RADIOBUTTON), UILabelledElement
