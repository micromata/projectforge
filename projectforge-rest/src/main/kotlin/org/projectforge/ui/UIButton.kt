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

data class UIButton(val id : String,
                    /** May be null for standard buttons. For standard buttons the title will be set dependent on the id. */
                    var title : String? = null,
                    val style : UIStyle? = null,
                    /**
                     * There should be one default button in every form, used if the user hits return.
                     */
                    val default : Boolean? = null,
                    /**
                     * Tell the client of what to do after clicking this button.
                     */
                    val responseAction: ResponseAction? = null)
    : UIElement(UIElementType.BUTTON)
