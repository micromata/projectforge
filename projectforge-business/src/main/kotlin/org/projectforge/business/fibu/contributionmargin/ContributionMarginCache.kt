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


package org.projectforge.business.fibu.contributionmargin

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.fibu.RechnungDO
import org.projectforge.business.fibu.RechnungCache
import org.projectforge.business.fibu.RechnungDao
import org.projectforge.business.fibu.kost.BuchungssatzDO
import org.projectforge.business.fibu.kost.BuchungssatzDao
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.Kost2Dao
import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.cache.CacheListener
import org.projectforge.framework.persistence.api.BaseDOModifiedListener
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale

private val log = KotlinLogging.logger {}

/**
 * The contribution margin parts of single projects ([ProjectPart]), so any set of projects (e.g. the ones of a
 * business unit, then all of them) only loads the projects not calculated yet. The parts don't depend on the
 * user: the access to the projects is checked before ([ContributionMarginService.allowedProjectIds]).
 *
 * Any change of invoices, accounting records, projects or kost2 clears the whole cache, as does a reload of the
 * invoice or cost cache (e.g. "refresh caches" of the administration). Time sheets aren't
 * watched, the parts expire after [TTL_MILLIS] anyway.
 */
@Service
class ContributionMarginCache {
  /** The current time in millis, replaceable by tests. */
  internal var clock: () -> Long = System::currentTimeMillis

  /**
   * Everything a part depends on beside its project: parts are only combined if calculated in the same
   * context.
   */
  data class Context(
    val startMonth: YearMonth,
    val today: LocalDate,
    val bookingImportEnd: LocalDate?,
    /** The configuration as json (hourly rate, revenue accounts, kost2 assignments). */
    val config: String?,
    /** The time sheets are assigned to the days of this zone. */
    val zoneId: ZoneId,
    /** The texts of the time sheets and the status of the invoices are translated. */
    val locale: Locale?,
  )

  /** The entries and invoice rows of one project. */
  class ProjectPart(
    val entries: List<ContributionMarginEntry>,
    val invoiceRows: List<ContributionMarginInvoiceRow>,
  )

  private class Part(val created: Long, val part: ProjectPart)

  // Access order: the least recently used context is the first one, removed if there are more than MAX_CONTEXTS.
  private val contexts = object : LinkedHashMap<Context, MutableMap<Long, Part>>(4, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Context, MutableMap<Long, Part>>?): Boolean =
      size > MAX_CONTEXTS
  }

  /** Incremented whenever the cache is cleared, so parts calculated before aren't stored afterwards. */
  private var generation = 0

  @Autowired
  private lateinit var rechnungDao: RechnungDao

  @Autowired
  private lateinit var buchungssatzDao: BuchungssatzDao

  @Autowired
  private lateinit var projektDao: ProjektDao

  @Autowired
  private lateinit var kost2Dao: Kost2Dao

  @Autowired
  private lateinit var rechnungCache: RechnungCache

  @Autowired
  private lateinit var kostCache: KostCache

  /** A reload of the cache (e.g. by "refresh caches" of the administration) clears this cache as well. */
  private val reloadListener = object : CacheListener {
    override fun onAfterCacheRefresh() {
      clear()
    }
  }

  @PostConstruct
  private fun postConstruct() {
    rechnungDao.register(listener<RechnungDO>())
    buchungssatzDao.register(listener<BuchungssatzDO>())
    projektDao.register(listener<ProjektDO>())
    kost2Dao.register(listener<Kost2DO>())
    rechnungCache.register(reloadListener)
    kostCache.register(reloadListener)
  }

  /**
   * The parts of [projectIds] in [context]. The missing (or expired) ones are calculated together by [load],
   * which must return a part for each of the given projects.
   */
  fun get(
    context: Context,
    projectIds: Set<Long>,
    load: (Set<Long>) -> Map<Long, ProjectPart>,
  ): Map<Long, ProjectPart> {
    val result = mutableMapOf<Long, ProjectPart>()
    val startGeneration = synchronized(contexts) {
      val now = clock()
      val parts = contexts[context]
      projectIds.forEach { id ->
        parts?.get(id)?.takeIf { now - it.created < TTL_MILLIS }?.let { result[id] = it.part }
      }
      generation
    }
    val missing = projectIds - result.keys
    if (missing.isEmpty()) {
      return result
    }
    log.debug { "Contribution margin: ${result.size} projects cached, ${missing.size} to calculate." }
    val loaded = load(missing)
    synchronized(contexts) {
      // Not, if the cache was cleared meanwhile: the loaded parts may already be outdated.
      if (startGeneration == generation) {
        val now = clock()
        val parts = contexts.getOrPut(context) { mutableMapOf() }
        loaded.forEach { (id, part) -> parts[id] = Part(now, part) }
      }
    }
    result.putAll(loaded)
    return result
  }

  fun clear() {
    synchronized(contexts) {
      if (contexts.isNotEmpty()) {
        log.debug { "Clearing the cached contribution margins." }
      }
      contexts.clear()
      generation++
    }
  }

  internal val size: Int
    get() = synchronized(contexts) { contexts.values.sumOf { it.size } }

  private fun <O : ExtendedBaseDO<Long>> listener() = object : BaseDOModifiedListener<O> {
    override fun afterInsertOrModify(obj: O, operationType: OperationType) {
      clear()
    }
  }

  companion object {
    internal const val MAX_CONTEXTS = 10

    internal const val TTL_MILLIS = 10 * 60 * 1000L
  }
}
