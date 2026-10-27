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

package org.projectforge.business.fibu.kost.reallocation

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import jakarta.persistence.*
import org.projectforge.business.fibu.kost.BuchungssatzDO
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.json.IdOnlySerializer
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * A kost2 share of an accounting record. The active shares of a record ([isActive]) sum up to its amount (same sign);
 * a record without active shares counts on its own kost2.
 *
 * Shares are versioned by run: a run splitting a record anew marks the active shares as superseded by itself
 * ([supersededByRun]) and inserts its own. So a revert can restore the previous shares. The shares are no collection
 * of [BuchungssatzDO]: the DATEV import rebuilds the records and would remove them.
 */
@Entity
@Table(
    name = "t_fibu_buchungssatz_kost2_share",
    indexes = [
        Index(name = "idx_fk_t_fibu_buchungssatz_kost2_share_buchungssatz_fk", columnList = "buchungssatz_fk"),
        Index(name = "idx_fk_t_fibu_buchungssatz_kost2_share_kost2_fk", columnList = "kost2_fk"),
        Index(name = "idx_fk_t_fibu_buchungssatz_kost2_share_run_fk", columnList = "run_fk"),
    ]
)
open class BuchungssatzKost2ShareDO : DefaultBaseDO() {
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "buchungssatz_fk", nullable = false)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var buchungssatz: BuchungssatzDO? = null

    @PropertyInfo(i18nKey = "fibu.kost2")
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "kost2_fk", nullable = false)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var kost2: Kost2DO? = null

    @PropertyInfo(i18nKey = "fibu.common.betrag")
    @get:Column(nullable = false, scale = 2, precision = 18)
    open var betrag: BigDecimal? = null
        set(value) {
            field = value?.setScale(2, RoundingMode.HALF_UP)
        }

    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20, nullable = false)
    open var source: Kost2ShareSource? = null

    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "run_fk")
    @JsonSerialize(using = IdOnlySerializer::class)
    open var run: Kost2ReallocationDO? = null

    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "superseded_by_run_fk")
    @JsonSerialize(using = IdOnlySerializer::class)
    open var supersededByRun: Kost2ReallocationDO? = null

    /** Not deleted and not superseded by a later run. */
    val isActive: Boolean
        @Transient
        get() = !deleted && supersededByRun == null
}
