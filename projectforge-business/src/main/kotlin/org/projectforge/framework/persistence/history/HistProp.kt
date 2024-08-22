/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.persistence.history

/**
 * Description of a History Property.
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
class HistProp {
    /**
     * The name.
     */
    var name: String? = null

    /**
     * The type.
     */
    var type: String? = null

    /**
     * The value.
     */
    @JvmField
    var value: String? = null

    constructor()

    constructor(name: String?, type: String?, value: String?) : super() {
        this.name = name
        this.type = type
        this.value = value
    }
}
