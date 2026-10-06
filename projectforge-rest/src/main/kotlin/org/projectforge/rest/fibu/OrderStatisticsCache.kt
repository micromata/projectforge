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

package org.projectforge.rest.fibu

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.projectforge.business.fibu.AuftragDO
import org.projectforge.business.fibu.AuftragDao
import org.projectforge.business.fibu.AuftragsCache
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.fibu.RechnungDO
import org.projectforge.business.fibu.RechnungCache
import org.projectforge.business.fibu.RechnungDao
import org.projectforge.business.fibu.kost.BuchungssatzDO
import org.projectforge.business.fibu.kost.BuchungssatzDao
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.cache.CacheListener
import org.projectforge.framework.persistence.api.BaseDOModifiedListener
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.framework.persistence.api.MagicFilter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.Locale

private val log = KotlinLogging.logger {}

/**
 * The results of the forecast and contribution margin calculations of the order statistics page
 * (`/next/orderStatistics`), so switching between the tabs, favorites and back again doesn't recalculate
 * everything. The charts and the tables below them are served from the same entry, so they are calculated once.
 *
 * The user is part of the key, as the orders found depend on the user's rights: no user ever gets a result
 * calculated for another one. The callers check the access before asking the cache.
 *
 * Any change of orders, invoices, projects or accounting records clears the whole cache, as does a reload of the
 * order or invoice cache (e.g. "refresh caches" of the administration). Time sheets aren't
 * watched (the contribution margin ends with the previous month), the entries expire after [TTL_MILLIS] anyway.
 */
@Service
class OrderStatisticsCache {
  /** The current time in millis, replaceable by tests. */
  internal var clock: () -> Long = System::currentTimeMillis

  enum class Kind { FORECAST, CONTRIBUTION_MARGIN }

  data class Key(
    val userId: Long?,
    val locale: Locale?,
    val kind: Kind,
    /** The criteria of the filter, see [filterKey]. */
    val filter: String,
    val startDate: LocalDate?,
    val planningDate: LocalDate? = null,
    /** The forecast variant, see [org.projectforge.business.fibu.ForecastOrderPosInfo.distributeUnusedBudget]. */
    val distributeUnusedBudget: Boolean? = null,
  )

  private class Entry(val created: Long, val value: Lazy<Any?>)

  // Access order: the least recently used entry is the first one, removed if there are more than MAX_ENTRIES.
  private val entries = object : LinkedHashMap<Key, Entry>(16, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Key, Entry>?): Boolean = size > MAX_ENTRIES
  }

  @Autowired
  private lateinit var auftragDao: AuftragDao

  @Autowired
  private lateinit var rechnungDao: RechnungDao

  @Autowired
  private lateinit var projektDao: ProjektDao

  @Autowired
  private lateinit var buchungssatzDao: BuchungssatzDao

  @Autowired
  private lateinit var auftragsCache: AuftragsCache

  @Autowired
  private lateinit var rechnungCache: RechnungCache

  /** A reload of the cache (e.g. by "refresh caches" of the administration) clears this cache as well. */
  private val reloadListener = object : CacheListener {
    override fun onAfterCacheRefresh() {
      clear()
    }
  }

  @PostConstruct
  private fun postConstruct() {
    auftragDao.register(listener<AuftragDO>())
    rechnungDao.register(listener<RechnungDO>())
    projektDao.register(listener<ProjektDO>())
    buchungssatzDao.register(listener<BuchungssatzDO>())
    auftragsCache.register(reloadListener)
    rechnungCache.register(reloadListener)
  }

  /**
   * The cached value of [key], calculated by [calculate] if there is none (or it expired). Concurrent calls for
   * the same key wait for the one calculation. A failed calculation isn't cached.
   */
  fun <T> get(key: Key, calculate: () -> T): T {
    val entry = synchronized(entries) {
      val now = clock()
      entries[key]?.takeIf { now - it.created < TTL_MILLIS }
        ?: Entry(now, lazy(LazyThreadSafetyMode.SYNCHRONIZED) { calculate() }).also { entries[key] = it }
    }
    try {
      @Suppress("UNCHECKED_CAST")
      return entry.value.value as T
    } catch (ex: Exception) {
      synchronized(entries) {
        if (entries[key] === entry) {
          entries.remove(key)
        }
      }
      throw ex
    }
  }

  fun clear() {
    synchronized(entries) {
      if (entries.isNotEmpty()) {
        log.debug { "Clearing ${entries.size} cached order statistics." }
      }
      entries.clear()
    }
  }

  internal val size: Int
    get() = synchronized(entries) { entries.size }

  private fun <O : ExtendedBaseDO<Long>> listener() = object : BaseDOModifiedListener<O> {
    override fun afterInsertOrModify(obj: O, operationType: OperationType) {
      clear()
    }
  }

  companion object {
    internal const val MAX_ENTRIES = 30

    internal const val TTL_MILLIS = 10 * 60 * 1000L

    /**
     * The criteria of [filter] as a string independent of their order. Name and id of the favorite are left out:
     * the same criteria give the same result, whichever favorite they came from.
     */
    fun filterKey(filter: MagicFilter): String {
      return filter.entries
        .map { entry ->
          val value = entry.value
          // Labels and display names are left out: they don't change the result.
          val values = value.values?.sorted()?.joinToString(",")
          "${entry.field}=${value.value}|${value.fromValue}|${value.toValue}|${value.id}|$values"
        }
        .sorted()
        .joinToString("&")
    }
  }
}
