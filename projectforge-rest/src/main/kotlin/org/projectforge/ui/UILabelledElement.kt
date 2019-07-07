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

interface UILabelledElement {
    var label: String?
    var additionalLabel: String?
    var tooltip: String?
    /**
     * Only the clazz property of layout setting is used for getting i18n keys from the entity fields if not given.
     */
    val layoutContext: LayoutContext?
    /**
     * If true, the additional label will be ignored (neither auto translated nor serialized).
     */
    val ignoreAdditionalLabel: Boolean
}
