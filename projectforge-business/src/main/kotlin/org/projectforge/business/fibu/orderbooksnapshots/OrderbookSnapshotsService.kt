/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.fibu.orderbooksnapshots

import com.fasterxml.jackson.core.type.TypeReference
import jakarta.annotation.PostConstruct
import jakarta.persistence.Tuple
import mu.KotlinLogging
import org.projectforge.Constants
import org.projectforge.business.fibu.AuftragDO
import org.projectforge.business.fibu.AuftragDao
import org.projectforge.business.jobs.CronSanityCheckJob
import org.projectforge.framework.json.JsonUtils
import org.projectforge.framework.persistence.database.TupleUtils
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.time.PFDateTimeUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.util.*
import java.util.zip.Deflater
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

private val log = KotlinLogging.logger {}

@Service
class OrderbookSnapshotsService {
    class SerializedSnapshot(val count: Int, val gzBytes: ByteArray?, var date: LocalDate = LocalDate.now())

    @Autowired
    private lateinit var auftragDao: AuftragDao

    @Autowired
    private lateinit var orderConverterService: OrderConverterService

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var cronSanityCheckJob: CronSanityCheckJob

    @PostConstruct
    private fun postConstruct() {
        OrderbookSnapshotsSanityCheck(this).let {
            cronSanityCheckJob.registerJob(it)
        }
    }

    /**
     * Creates daily snapshots of the order book at the beginning of a day (after midnight).
     * This jobs runs hourly and check, if a snapshot for today exists. If not, it will be created.
     * For the beginning of a new month, a full backup is created. All other backups are incremental.
     */
    @Scheduled(fixedDelay = 1 * Constants.MILLIS_PER_HOUR, initialDelay = 2 * Constants.MILLIS_PER_MINUTE)
    fun createDailySnapshots() {
        try {
            log.info { "Checking daily snapshots..." }
            persistenceService.runInNewTransaction(recordCallStats = true) { context ->
                val today = LocalDate.now()
                val entry = findEntry(today)
                if (entry != null) {
                    log.info { "Order book snapshot for today ($today UTC) already exists. OK, nothing to do." }
                    return@runInNewTransaction
                }
                var incrementalBasedOn: LocalDate? = null
                if (today.dayOfMonth != 1) {
                    // For the first day of month, full backup is created. So handle all other days as incremental:
                    // Find the last full backup:
                    selectRecentFullBackup()?.let {
                        log.debug { "Found recent full backup: ${it.date}" }
                        it.date?.let { snapshotDate ->
                            if (snapshotDate.month == today.month) {
                                log.info { "Full backup found in the current month: ${it.date}, so store an incremental snapshot..." }
                                try {
                                    // Checking for sanity:
                                    readSnapshot(snapshotDate)
                                    incrementalBasedOn = it.date
                                } catch (e: Exception) {
                                    log.error { "Recent full backup seems to be corrupted. Creating a full backup again: ${e.message}" }
                                }
                            }
                        }
                    }
                }
                storeOrderbookSnapshot(incrementalBasedOn = incrementalBasedOn, date = today)
                log.info { "Checking daily snapshots done. ${context.formatStats()}" }
            }
        } catch (e: Exception) {
            log.error(e) { "Error in createDailySnapshots: ${e.message}" }
        }
    }

    /**
     * Creates a snapshot of the current order book's state.
     * @param incrementalBasedOn if given, this entry contains only the orders which were modified after this date.
     * @param returnGZipBytes if true, the gzipped bytes are returned.
     * @return the date and the number of stored orders.
     */
    @JvmOverloads
    fun createOrderbookSnapshot(incrementalBasedOn: LocalDate? = null): SerializedSnapshot {
        if (incrementalBasedOn != null) {
            log.info { "Creating incremental order book snapshot based on $incrementalBasedOn..." }
        } else {
            log.info { "Creating full order book snapshot..." }
        }
        // First, select all orders that are not deleted:
        val auftragList = auftragDao.select(deleted = false, checkAccess = false)
        val incrementList = if (incrementalBasedOn != null) {
            val basedOn = selectMeta(incrementalBasedOn)
            if (basedOn == null) {
                log.error { "No order book found, which based on given date: $incrementalBasedOn. Falling back to full backup." }
                auftragList
            } else {
                val basedOnDate = PFDateTimeUtils.getBeginOfDateAsUtildate(incrementalBasedOn)
                // Filter all orders that were modified at or after the given date:
                auftragList.filter { (it.lastUpdate ?: Date()) >= basedOnDate }
            }
        } else {
            auftragList
        }
        log.info { "Converting ${incrementList.size}/${auftragList.size} orders..." }
        val orderbook = orderConverterService.convertFromAuftragDO(incrementList)
        if (orderbook.isNullOrEmpty()) {
            log.warn { "No orders found to store!!!" }
            return SerializedSnapshot(count = 0, gzBytes = null)
        }
        val count = orderbook.size
        log.info { "Converting ${incrementList.size}/${auftragList.size} orders to json..." }
        val json = JsonUtils.toJson(orderbook, ignoreNullableProps = true)
        log.info { "Zipping ${incrementList.size} orders..." }
        val gzBytes = gzip(json)
        return SerializedSnapshot(count = count, gzBytes = gzBytes)
    }

    /**
     * If today's order book snapshot is already stored, nothing will be done.
     * @param date the date of the order book's snapshot (default is today). This is for testing purposes only.
     */
    internal fun storeOrderbookSnapshot(
        incrementalBasedOn: LocalDate? = null,
        date: LocalDate = LocalDate.now(),
    ): SerializedSnapshot {
        val rawSnapshot = createOrderbookSnapshot(incrementalBasedOn)
        rawSnapshot.date = date
        // Store the order book in the database:
        OrderbookSnapshotDO().also {
            it.date = date
            it.serializedOrderBook = rawSnapshot.gzBytes
            it.size = it.serializedOrderBook?.size
            it.incrementalBasedOn = incrementalBasedOn
        }.let {
            persistenceService.runInTransaction { context ->
                val entry = selectMeta(date)
                if (entry != null) {
                    entry.serializedOrderBook = it.serializedOrderBook
                    context.em.merge(entry)
                } else {
                    context.em.persist(it)
                }
            }
        }
        log.info { "Storing order book done." }
        return rawSnapshot
    }

    fun readSnapshot(date: LocalDate): List<AuftragDO>? {
        val orderbook = mutableMapOf<Long, Order>()
        readSnapshot(date, orderbook)
        return orderConverterService.convertFromOrder(orderbook.values).also {
            log.info { "${it?.size} orders restored from snapshot of $date." }
        }
    }

    private fun readSnapshot(date: LocalDate, orderbook: MutableMap<Long, Order>) {
        val entry = findEntry(date)
        if (entry == null) {
            log.error { "No order book found for date: $date" }
            return
        }
        if (entry.incremental) {
            log.info { "Restoring order book for date $date (incremental based on ${entry.incrementalBasedOn})..." }
        } else {
            log.info { "Restoring order book for date $date..." }
        }
        entry.incrementalBasedOn?.let { incrementalBasedOn ->
            log.info { "Restoring order book from previous backup first: date=$date..." }
            if (date <= incrementalBasedOn) {
                log.error { "Internal error: Incremental based on date is greater than the date of the order book: $incrementalBasedOn > $date" }
            }
            // Load order book from the incremental based on date first:
            readSnapshot(incrementalBasedOn, orderbook)
        }
        val serialized = entry.serializedOrderBook ?: return
        readSnapshot(serialized, orderbook)
    }

    private fun readSnapshot(serialized: ByteArray, orderbook: MutableMap<Long, Order>) {
        val json = gunzip(serialized)
        JsonUtils.fromJson(json, object : TypeReference<List<Order?>?>() {})?.forEach { order ->
            order?.id?.let { id ->
                orderbook[id] = order
            }
        }
    }

    private fun selectRecentFullBackup(): OrderbookSnapshotDO? {
        // selectMetas is already sorted by date descending, but to be sure:
        return selectMetas(onlyFullBackups = true).sortedByDescending { it.date }.firstOrNull()
    }

    private fun selectMeta(date: LocalDate): OrderbookSnapshotDO? {
        return persistenceService.selectNamedSingleResult(
            OrderbookSnapshotDO.FIND_META_BY_DATE,
            Tuple::class.java,
            "date" to date,
        )?.let {
            OrderbookSnapshotDO().also { result ->
                result.date = TupleUtils.getLocalDate(it, "date")
                result.incrementalBasedOn = TupleUtils.getLocalDate(it, "incrementalBasedOn")
                result.size = TupleUtils.getInt(it, "size")
            }
        }
    }

    internal fun selectMetas(onlyFullBackups: Boolean = false): List<OrderbookSnapshotDO> {
        val named =
            if (onlyFullBackups) OrderbookSnapshotDO.SELECT_ALL_FULLBACKUP_METAS else OrderbookSnapshotDO.SELECT_ALL_METAS
        val res = persistenceService.executeNamedQuery(
            named,
            Tuple::class.java,
        ).map {
            OrderbookSnapshotDO().also { result ->
                result.date = TupleUtils.getLocalDate(it, "date")
                result.incrementalBasedOn = TupleUtils.getLocalDate(it, "incrementalBasedOn")
                result.size = TupleUtils.getInt(it, "size")
            }
        }
        return if (onlyFullBackups) {
            res.filter { it.incrementalBasedOn == null } // incrementalBasedOn == null means full backup (double check)
        } else {
            return res
        }
    }

    private fun findEntry(date: LocalDate): OrderbookSnapshotDO? {
        return persistenceService.find(OrderbookSnapshotDO::class.java, date)
    }

    private fun gzip(str: String): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val gzipStream = object : GZIPOutputStream(byteArrayOutputStream) {
            init {
                def.setLevel(Deflater.BEST_COMPRESSION)
            }
        }
        gzipStream.use { gzipStream ->
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
