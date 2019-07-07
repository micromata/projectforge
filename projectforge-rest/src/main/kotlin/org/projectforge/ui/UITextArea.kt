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

data class UITextArea(val id: String,
                      @Transient
                      override val layoutContext: LayoutContext? = null,
                      var maxLength: Int? = null,
                      /**
                       * Number of rows to display initially.
                       */
                      var rows:Int? = 3,
                      /**
                       * Max rows to auto-extend for long content.
                       */
                      var maxRows:Int? = 10,
                      override var label: String? = null,
                      override var additionalLabel: String? = null,
                      override var tooltip: String? = null,
                      override val ignoreAdditionalLabel: Boolean = false)
    : UIElement(UIElementType.TEXTAREA), UILabelledElement
