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

package org.projectforge.business.fibu.customergroup

import jakarta.annotation.PostConstruct
import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.business.fibu.AuftragDO
import org.projectforge.business.fibu.AuftragDao
import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.persistence.api.BaseDOModifiedListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

private val log = KotlinLogging.logger {}

/**
 * The [CustomerUsage]: the customers of all orders and the projects booked on by timesheets, read in two
 * aggregating queries. Expired by any change of an order; the timesheets only by the hourly expiry, since a
 * listener on every booking would refresh far too often for what rarely changes (the first sheet of a project).
 */
@Component
class CustomerUsageCache : AbstractCache(TICKS_PER_HOUR) {
    @Autowired
    private lateinit var auftragDao: AuftragDao

    @Autowired
    private lateinit var kostCache: KostCache

    private var usage = CustomerUsage.EMPTY

    @PostConstruct
    private fun postConstruct() {
        auftragDao.register(object : BaseDOModifiedListener<AuftragDO> {
            override fun afterInsertOrModify(obj: AuftragDO, operationType: OperationType) {
                setExpired()
            }
        })
    }

    /** Replaced as a whole by each refresh, so a caller can tell by identity whether it is still current. */
    val all: CustomerUsage
        get() {
            checkRefresh()
            return usage
        }

    override fun refresh() {
        log.info { "Initializing CustomerUsageCache ..." }
        // As KundeCache: the refresh may run inside a write transaction (the order listener).
        runReadOnlyForCacheMaintenance { context ->
            val orders = mutableMapOf<Pair<CustomerKey, Long?>, LocalDate>()
            context.executeQuery(
                "select k.id, t.kundeText, p.id, max(coalesce(t.angebotsDatum, t.erfassungsDatum)) from AuftragDO t left join t.kunde k left join t.projekt p where t.deleted = false group by k.id, t.kundeText, p.id",
                Array<Any?>::class.java,
            ).forEach { row ->
                val key = CustomerKey.of((row[0] as? Number)?.toLong(), row[1] as? String) ?: return@forEach
                val date = row[3] as? LocalDate ?: return@forEach
                orders.merge(key to (row[2] as? Number)?.toLong(), date, ::maxOf)
            }
            val sheets = mutableMapOf<Long, LocalDate>()
            context.executeQuery(
                "select t.kost2.id, max(t.startTime) from TimesheetDO t where t.deleted = false and t.kost2 is not null group by t.kost2.id",
                Array<Any?>::class.java,
            ).forEach { row ->
                val projektId = kostCache.getKost2((row[0] as? Number)?.toLong())?.projekt?.id ?: return@forEach
                val date = dateOf(row[1]) ?: return@forEach
                sheets.merge(projektId, date, ::maxOf)
            }
            usage = CustomerUsage(orders, sheets)
            log.info { "Initializing of CustomerUsageCache done: ${orders.size} order customers, ${sheets.size} projects with timesheets. ${context.formatStats()}" }
        }
    }

    private fun dateOf(value: Any?): LocalDate? = when (value) {
        is LocalDate -> value
        is LocalDateTime -> value.toLocalDate()
        is Date -> value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        else -> null
    }
}
