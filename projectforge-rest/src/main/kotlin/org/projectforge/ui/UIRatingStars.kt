/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

data class UIRatingStars(var id: String,
                         @Transient
                         override val layoutContext: LayoutContext? = null,
                         /**
                          * The values behind the stars. The size of this array defines the number of stars.
                          */
                         @Suppress("ArrayInDataClass")
                         var values: Array<String>,
                         override var tooltip: String? = null,
                         override var label: String? = null,
                         override var additionalLabel: String? = null,
                         @Transient
                         override val ignoreAdditionalLabel: Boolean = false,
                         @Transient
                         override val ignoreTooltip: Boolean = false)
    : UIElement(UIElementType.RATING), UILabelledElement
