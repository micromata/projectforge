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

package org.projectforge.framework.persistence.entities

import de.micromata.genome.db.jpa.tabattr.entities.TimeableBaseDO
import org.projectforge.framework.time.PFDay
import java.time.LocalDate
import javax.persistence.Transient

/**
 * Should be used, because in ProjectForge TimeableBaseDO is used with startTime with day or month precision. So the callers should
 * use startDay instead of startTime.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Suppress("FINITE_BOUNDS_VIOLATION_IN_JAVA")
abstract class PFTimeableBaseDO<Self : TimeableBaseDO<Self, Int>> : TimeableBaseDO<Self, Int>() {
    /**
     * For working with local dates. This converts startTime from and to localDate. This should be used instead of startTime.
     */
    @get:Transient
    var startDay: LocalDate?
        get() = PFDay.fromOrNullUTC(startTime)?.localDate
        set(value) {
            startTime = PFDay.fromOrNull(value)?.utilDateUTC
        }
}
