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

package org.projectforge.business.fibu.orderbookstorage

import com.fasterxml.jackson.core.type.TypeReference
import mu.KotlinLogging
import org.projectforge.business.fibu.AuftragDO
import org.projectforge.business.fibu.AuftragDao
import org.projectforge.framework.json.JsonUtils
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

private val log = KotlinLogging.logger {}

@Service
class OrderbookStorageService {
    class Stats(val date: LocalDate, val count: Int, val gzBytes: ByteArray?)

    @Autowired
    private lateinit var auftragDao: AuftragDao

    @Autowired
    private lateinit var orderConverterService: OrderConverterService

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    /**
     * Stores the current orderbook in the database.
     * If there are no orders, nothing is stored.
     * If today's orderbook is already stored, it will be updated/overwritten.
     * @return the date and the number of stored orders.
     */
    @JvmOverloads
    fun storeOrderbook(returnGZipBytes: Boolean = false): Stats {
        log.info { "Storing orderbook..." }
        // First, select all orders that are not deleted:
        val auftragList = auftragDao.select(deleted = false, checkAccess = false)
        log.info { "Converting ${auftragList.size} orders..." }
        val orderbook = orderConverterService.convertFromAuftragDO(auftragList)
        val date = LocalDate.now()
        if (orderbook.isNullOrEmpty()) {
            log.warn { "No orders found to store!!!" }
            return Stats(date, 0, null)
        }
        val count = orderbook.size
        log.info { "Converting ${auftragList.size} orders to json..." }
        val json = JsonUtils.toJson(orderbook)
        log.info { "Zipping ${auftragList.size} orders..." }
        val gzipBytes = gzip(json)
        // Store the orderbook in the database:
        OrderbookStorageDO().also {
            it.date = date
            it.serializedOrderBook = gzipBytes
        }.let {
            persistenceService.runInTransaction { context ->
                val entry = persistenceService.selectNamedSingleResult(
                    OrderbookStorageDO.FIND_BY_DATE, OrderbookStorageDO::class.java,
                    "date" to date
                )
                if (entry != null) {
                    entry.serializedOrderBook = it.serializedOrderBook
                    context.em.merge(entry)
                } else {
                    context.em.persist(it)
                }
            }
        }
        log.info { "Storing orderbook done." }
        return Stats(date, count, if (returnGZipBytes) gzipBytes else null)
    }

    fun restoreOrderbook(date: LocalDate): List<AuftragDO>? {
        val entry = persistenceService.selectNamedSingleResult(
            OrderbookStorageDO.FIND_BY_DATE, OrderbookStorageDO::class.java,
            "date" to date
        ) ?: return null
        val serialized = entry.serializedOrderBook ?: return emptyList()
        val json = gunzip(serialized)
        val result = JsonUtils.fromJson(json, object : TypeReference<List<Order?>?>() {})
        return orderConverterService.convertFromOrder(result)
    }

    private fun gzip(str: String): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        GZIPOutputStream(byteArrayOutputStream).use { gzipStream ->
            gzipStream.write(str.toByteArray(Charsets.UTF_8))
        }
        return byteArrayOutputStream.toByteArray()
    }

    fun gunzip(compressed: ByteArray): String {
        val byteArrayInputStream = ByteArrayInputStream(compressed)
        GZIPInputStream(byteArrayInputStream).use { gzipStream ->
            return gzipStream.readBytes().toString(Charsets.UTF_8)
        }
    }
}
