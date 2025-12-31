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

package org.projectforge.framework.persistence.entities

import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Transient
import org.projectforge.framework.persistence.history.WithHistory
import java.io.Serializable

/**
 * Declares lastUpdate and created as invalidHistorizableProperties.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@MappedSuperclass
@WithHistory
abstract class AbstractHistorizableBaseDO<I : Serializable> : AbstractBaseDO<I>() {
    /**
     * @see org.projectforge.framework.persistence.history.HistoryEntryDO.userComment
     */
    @get:Transient
    var historyUserComment: String? = null

    companion object {
        private const val serialVersionUID = -5980671510045450615L
    }
}
