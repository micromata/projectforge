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

class UIFieldset(
        /**
         * Length in grid system (1-12)
         */
        length: Int? = null,
        /**
         * Length for small screens (1-12)
         */
        smLength: Int? = null,
        /**
         * Length for large screens (1-12)
         */
        mdLength: Int? = null,
        /**
         * Length for large screens (1-12)
         */
        lgLength: Int? = null,
        /**
         * Length for large screens (1-12)
         */
        xlLength: Int? = null,
        var title: String? = null) :
        UICol(length, smLength, mdLength, lgLength, xlLength, type = UIElementType . FIELDSET)
