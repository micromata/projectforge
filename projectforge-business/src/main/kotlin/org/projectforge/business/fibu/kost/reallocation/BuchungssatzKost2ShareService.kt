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

import org.projectforge.business.PfCaches
import org.projectforge.business.fibu.kost.BuchungssatzDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * The effective view of the accounting records: a reallocated record (with active kost2 shares) counts with each share
 * on the share's kost2, a record without shares on its own kost2. Used by the contribution margin, the reporting and
 * the scripting.
 */
@Service
open class BuchungssatzKost2ShareService {
    @Autowired
    private lateinit var shareDao: BuchungssatzKost2ShareDao

    @Autowired
    private lateinit var caches: PfCaches

    @Autowired
    private lateinit var runDao: Kost2ReallocationDao

    /** The active shares of the given records, by record id. Records without shares aren't contained. */
    open fun activeSharesByRecordId(recordIds: Collection<Long>): Map<Long, List<BuchungssatzKost2ShareDO>> {
        if (recordIds.isEmpty()) {
            return emptyMap()
        }
        return shareDao.selectActiveByRecordIds(recordIds.distinct()).groupBy { it.buchungssatz!!.id!! }
    }

    /** The ids of the reallocated records (with active shares) out of the given ones. */
    open fun recordIdsWithActiveShares(recordIds: Collection<Long>): Set<Long> {
        if (recordIds.isEmpty()) {
            return emptySet()
        }
        return shareDao.selectRecordIdsWithActiveShares(recordIds.distinct())
    }

    /**
     * The descriptions of the runs (comma separated) of the active shares of the given records, by record id. Records
     * without shares aren't contained. Shares without run (none yet) count as "?".
     */
    open fun runDescriptionsByRecordId(recordIds: Collection<Long>): Map<Long, String> {
        val sharesByRecord = activeSharesByRecordId(recordIds)
        if (sharesByRecord.isEmpty()) {
            return emptyMap()
        }
        val descriptions = mutableMapOf<Long, String>()
        return sharesByRecord.mapValues { (_, shares) ->
            shares.mapNotNull { it.run?.id }.distinct().joinToString(", ") { runId ->
                descriptions.getOrPut(runId) { runDao.find(runId, checkAccess = false)?.description ?: "#$runId" }
            }.ifEmpty { "?" }
        }
    }

    /** The active shares of the given record (empty, if it isn't reallocated). */
    open fun activeShares(record: BuchungssatzDO): List<BuchungssatzKost2ShareDO> {
        val id = record.id ?: return emptyList()
        return activeSharesByRecordId(listOf(id))[id].orEmpty()
    }

    /**
     * The effective view: one transient copy per active share (kost2 and amount of the share, [BuchungssatzDO.originalRecord]
     * is the stored record), records without shares are returned as they are. The order of the records is kept.
     */
    open fun expand(records: List<BuchungssatzDO>): List<BuchungssatzDO> {
        val sharesByRecord = activeSharesByRecordId(records.mapNotNull { it.id })
        if (sharesByRecord.isEmpty()) {
            return records
        }
        return records.flatMap { record ->
            val shares = record.id?.let { sharesByRecord[it] }
            if (shares.isNullOrEmpty()) {
                listOf(record)
            } else {
                shares.map { copy(record, it) }
            }
        }
    }

    /**
     * The effective amounts by kost2 id of the given record: of its shares, or its whole amount on its own kost2.
     * @param shares The active shares of the record (see [activeSharesByRecordId]).
     */
    open fun effectiveAmounts(record: BuchungssatzDO, shares: List<BuchungssatzKost2ShareDO>?): Map<Long, BigDecimal> {
        if (shares.isNullOrEmpty()) {
            val kost2Id = record.kost2?.id ?: return emptyMap()
            return mapOf(kost2Id to (record.betrag ?: BigDecimal.ZERO))
        }
        val result = linkedMapOf<Long, BigDecimal>()
        shares.forEach { share ->
            val kost2Id = share.kost2!!.id!!
            result[kost2Id] = (result[kost2Id] ?: BigDecimal.ZERO) + (share.betrag ?: BigDecimal.ZERO)
        }
        return result
    }

    private fun copy(record: BuchungssatzDO, share: BuchungssatzKost2ShareDO): BuchungssatzDO {
        return BuchungssatzDO().also {
            it.id = record.id
            it.created = record.created
            it.lastUpdate = record.lastUpdate
            it.deleted = record.deleted
            it.year = record.year
            it.month = record.month
            it.satznr = record.satznr
            it.betrag = share.betrag
            it.sh = record.sh
            it.isIgnore = record.isIgnore
            it.konto = record.konto
            it.gegenKonto = record.gegenKonto
            it.datum = record.datum
            it.beleg = record.beleg
            it.text = record.text
            it.menge = record.menge
            it.kost1 = record.kost1
            it.kost2 = caches.getKost2(share.kost2?.id) ?: share.kost2
            it.comment = record.comment
            it.originalRecord = record
        }
    }

    companion object {
        /**
         * Splits the total into parts proportional to the weights, each rounded to cents. The last part with a weight
         * takes the rounding difference, so the parts always sum up to the total (same sign as the total). Weights must
         * not be negative; if all weights are zero, the last part takes the total.
         */
        @JvmStatic
        fun split(total: BigDecimal, weights: List<BigDecimal>): List<BigDecimal> {
            require(weights.isNotEmpty()) { "No weights given." }
            require(weights.none { it.signum() < 0 }) { "Negative weights aren't supported: $weights" }
            val totalScaled = total.setScale(2, RoundingMode.HALF_UP)
            val sumOfWeights = weights.fold(BigDecimal.ZERO, BigDecimal::add)
            if (sumOfWeights.signum() == 0) {
                return weights.mapIndexed { index, _ ->
                    if (index == weights.size - 1) totalScaled else BigDecimal.ZERO.setScale(2)
                }
            }
            val lastWeighted = weights.indexOfLast { it.signum() > 0 }
            val parts = weights.map { weight ->
                totalScaled.multiply(weight).divide(sumOfWeights, 2, RoundingMode.HALF_UP)
            }.toMutableList()
            val difference = totalScaled - parts.fold(BigDecimal.ZERO, BigDecimal::add)
            parts[lastWeighted] = parts[lastWeighted] + difference
            return parts
        }

        /** [split] with millis as weights. */
        @JvmStatic
        fun splitByMillis(total: BigDecimal, millis: List<Long>): List<BigDecimal> {
            return split(total, millis.map { BigDecimal.valueOf(it) })
        }
    }
}
