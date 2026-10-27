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
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.json.IdOnlySerializer
import org.projectforge.framework.persistence.entities.DefaultBaseDO

/**
 * A rule A -> B of a [Kost2ReallocationDO]: the records of the period on the source kost2 (effective kost2, optionally
 * restricted to an account range) go completely to the target kost2. Salary records are left to the time sheet based
 * salary shares, unless the account range covers the salary account explicitly.
 */
@Entity
@Table(
    name = "t_fibu_kost2_reallocation_rule",
    indexes = [Index(name = "idx_fk_t_fibu_kost2_reallocation_rule_run_fk", columnList = "run_fk")]
)
open class Kost2ReallocationRuleDO : DefaultBaseDO() {
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "run_fk", nullable = false)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var run: Kost2ReallocationDO? = null

    @PropertyInfo(i18nKey = "fibu.kost2Reallocation.rule.source")
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "source_kost2_fk", nullable = false)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var sourceKost2: Kost2DO? = null

    @PropertyInfo(i18nKey = "fibu.kost2Reallocation.rule.target")
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "target_kost2_fk", nullable = false)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var targetKost2: Kost2DO? = null

    /** Optional lower bound (inclusive) of the account number. */
    @PropertyInfo(i18nKey = "fibu.kost2Reallocation.rule.kontoFrom")
    @get:Column(name = "konto_from")
    open var kontoFrom: Int? = null

    /** Optional upper bound (inclusive) of the account number. */
    @PropertyInfo(i18nKey = "fibu.kost2Reallocation.rule.kontoTo")
    @get:Column(name = "konto_to")
    open var kontoTo: Int? = null

    /** Does the account range of this rule include the given account number (no range: all accounts)? */
    fun matchesKonto(kontoNummer: Int?): Boolean {
        if (kontoFrom == null && kontoTo == null) {
            return true
        }
        kontoNummer ?: return false
        return (kontoFrom == null || kontoNummer >= kontoFrom!!) && (kontoTo == null || kontoNummer <= kontoTo!!)
    }

    /** Is the given account explicitly part of the account range (not only by an open range)? */
    fun coversKontoExplicitly(kontoNummer: Int): Boolean {
        return (kontoFrom != null || kontoTo != null) && matchesKonto(kontoNummer)
    }
}
