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

data class UILabel(
        override var label: String? = null,
        /**
         * i18n key will be automatically translated. For protecting the auto-translation, please set [protectLabels] = true
         * or simply use trailing '^' char, e. g. "^My untranslated label".
         */
        var labelFor: String? = null,
        @Transient
        override val layoutContext: LayoutContext? = null,
        @Transient
        var reference: UIElement? = null,
        override var additionalLabel: String? = null,
        override var tooltip: String? = null,
        @Transient
        override val ignoreAdditionalLabel: Boolean = false)
    : UIElement(UIElementType.LABEL), UILabelledElement
