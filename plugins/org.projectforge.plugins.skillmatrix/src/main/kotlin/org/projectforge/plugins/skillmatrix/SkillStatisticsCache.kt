/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.skillmatrix

import mu.KotlinLogging
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.persistence.api.BaseDOChangedListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import javax.annotation.PostConstruct

private val log = KotlinLogging.logger {}

/**
 * Skill statistics cache holds ratings of all skills stored in the data base.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
open class SkillStatisticsCache : AbstractCache(), BaseDOChangedListener<SkillEntryDO> {
    @Autowired
    private lateinit var skillEntryDao: SkillEntryDao

    class SkillStatistic(val skill: String, val totalCounter: Int, val ratingMean: BigDecimal, val interestsMean: BigDecimal)

    private data class Entry(var skill: String,
                             var totalCounter: Int = 0,
                             /**
                              * Sum of all skill ratings.
                              */
                             var ratingSum: Int = 0,
                             /**
                              * Counter of ratings (not null) for getting average.
                              */
                             var ratingCounter: Int = 0,
                             /**
                              * Sum of all interest ratings.
                              */
                             var interestSum: Int = 0,
                             /**
                              * Counter of ratings (not null) for getting average.
                              */
                             var interstCounter: Int = 0) {
        val ratingMean: BigDecimal
            get() {
                return meanValue(ratingSum, ratingCounter)
            }

        val interestMean: BigDecimal
            get() {
                return meanValue(interestSum, interstCounter)
            }

        fun add(skillEntry: SkillEntryDO) {
            totalCounter++
            skillEntry.rating?.let {
                // rating is 0..3
                ratingSum += it
                ratingCounter++
            }
            skillEntry.interest?.let {
                // interest is 0..3
                interestSum += it
                interstCounter++
            }
        }

        fun meanValue(sum: Int, counter: Int): BigDecimal {
            return if (counter > 0)
                BigDecimal(sum).divide(BigDecimal(counter), 1, RoundingMode.HALF_UP)
            else
                BigDecimal.ZERO
        }
    }

    private lateinit var skillStatistics: List<SkillStatistic>

    val statistics: List<SkillStatistic>
        get() {
            checkRefresh()
            return skillStatistics
        }

    @PostConstruct
    private fun postConstruct() {
        skillEntryDao.register(this)
    }

    override fun afterSaveOrModify(changedObject: SkillEntryDO, operationType: OperationType) {
        setExpired()
    }

    override fun refresh() {
        log.info("Refreshing SkillMatrixCache ...")
        val skillStatisticsMap = mutableMapOf<String, Entry>()
        skillEntryDao.internalLoadAll()
                .filter { !it.isDeleted } // Ignore deleted skill entries.
                .sortedByDescending { it.lastUpdate } // Use skill syntax of last edited one (older ones will be normalized)
                .forEach { skillEntry ->
                    val skillName = skillEntry.skill ?: ""
                    val normalizedSkillName = skillEntry.normalizedSkill
                    val entry = skillStatisticsMap[normalizedSkillName] ?: run {
                        val newEntry = Entry(skillName)
                        skillStatisticsMap[normalizedSkillName] = newEntry
                        newEntry
                    }
                    entry.add(skillEntry)
                }
        skillStatistics = skillStatisticsMap.values
                .sortedBy { it.skill.lowercase() }
                .map { SkillStatistic(it.skill, it.totalCounter, it.ratingMean, it.interestMean) }
    }
}
