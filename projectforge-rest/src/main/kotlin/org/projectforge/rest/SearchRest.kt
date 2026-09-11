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

package org.projectforge.rest

import mu.KotlinLogging
import org.projectforge.NextMigration
import org.projectforge.framework.DisplayNameCapable
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.DaoConst
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.IdObject
import org.projectforge.framework.persistence.api.SearchDao
import org.projectforge.framework.persistence.jpa.impl.HibernateSearchFilterUtils
import org.projectforge.registry.Registry
import org.projectforge.registry.RegistryEntry
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractEntityRest
import org.projectforge.security.My2FARequestHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

/**
 * The global full text search of projectforge-next: the successor of the Wicket `SearchPage` (`wa/search`).
 *
 * Unlike the legacy page it exposes the search as plain data, so the next frontend can render both the live
 * hits under the top-nav magnifier and a dedicated search page. One endpoint serves both: they differ only in
 * the set of areas and the per-area limit.
 *
 * An "area" is a [RegistryEntry] the logged-in user may search. The set is deliberately restricted to a fixed
 * whitelist ([ALLOWED_AREAS]) of the entities that carry a name worth searching globally; every other registry area
 * is excluded. Within that whitelist an area only shows if the user may *read* its entries
 * ([BaseDao.hasLoggedInUserSelectAccess]) — read access, not history access, so e.g. a project manager who may read
 * customers but not their history still sees the customer area. Each hit carries the frontend url of its record
 * ([AbstractEntityRest.getStandardEditPage]), so the frontend needs no per-entity navigation logic.
 */
@RestController
@RequestMapping("${Rest.URL}/search")
class SearchRest {
  @Autowired
  private lateinit var searchDao: SearchDao

  @Autowired
  private lateinit var my2FARequestHandler: My2FARequestHandler

  /**
   * All [AbstractEntityRest] beans. The [Registry] maps a searchable area to its DAO, but only the rest knows the
   * frontend url a record opens at, so this is the missing DO-class → rest bridge (built once in [postConstruct]).
   */
  @Autowired
  private lateinit var entityRestBeans: List<AbstractEntityRest<*, *, *>>

  /**
   * DO class → rest, so an area's hits can be turned into navigable urls. Built lazily on first use, not in
   * `@PostConstruct`: reading a rest's [AbstractEntityRest.baseDao] resolves a bean, which mustn't be forced while
   * the context is still wiring.
   */
  private val restByDoClass: Map<Class<*>, AbstractEntityRest<*, *, *>> by lazy {
    val map = mutableMapOf<Class<*>, AbstractEntityRest<*, *, *>>()
    entityRestBeans.forEach { rest ->
      val doClass = rest.baseDao.doClass
      val existing = map[doClass]
      // A DO may be served by more than one rest (e.g. a multi-select page extends the same chain). Prefer the
      // one whose category matches the Registry area id — that is the list page a search hit should open.
      if (existing == null || prefer(rest, existing, doClass)) {
        map[doClass] = rest
      }
    }
    // A searchable area whose DO has no rest can't produce a navigable hit and is silently skipped at query time;
    // log it once so an unreachable area doesn't go unnoticed.
    Registry.getInstance().orderedList
      .filter { it.isSearchable && !map.containsKey(it.getDOClass()) }
      .forEach { log.info { "Searchable area '${it.id}' has no AbstractEntityRest and is excluded from the global search." } }
    map
  }

  /**
   * The searchable areas the logged-in user may use, in [PRIORITY_AREAS] order first (addresses lead), then the
   * Registry order. Feeds the search page's scope panel and the "search more" (Weitersuchen) request.
   */
  @GetMapping("areas")
  fun availableAreas(): List<SearchArea> {
    return accessibleAreas().map { SearchArea(it.id, translate(it.i18nTitleHeading)) }
  }

  /**
   * @param term        The search string, empty answers with no areas.
   * @param areas       Area ids to search; null/empty falls back to all accessible areas (MVP has no persisted scope yet).
   * @param maxPerArea  Hits per area before a [SearchAreaResult.hasMore] marker; the magnifier passes a small value.
   */
  @GetMapping("query")
  fun query(
    @RequestParam("q") term: String,
    @RequestParam("areas", required = false) areas: List<String>?,
    @RequestParam("maxPerArea", required = false) maxPerArea: Int?,
  ): SearchResponse {
    if (term.isBlank()) {
      return SearchResponse(term, emptyList())
    }
    val requested = areas?.takeIf { it.isNotEmpty() }?.toSet()
    val limit = maxPerArea?.coerceIn(1, MAX_PER_AREA_LIMIT) ?: DEFAULT_MAX_PER_AREA
    val searchString = HibernateSearchFilterUtils.modifySearchString(term, true)
    val results = accessibleAreas()
      .filter { requested == null || it.id in requested }
      .mapNotNull { entry -> searchArea(entry, searchString, limit) }
    return SearchResponse(term, results)
  }

  private fun searchArea(entry: RegistryEntry, searchString: String, limit: Int): SearchAreaResult? {
    val rest = restByDoClass[entry.getDOClass()] ?: return null
    val filter = BaseSearchFilter().also {
      it.searchString = searchString
      it.maxRows = limit
    }
    val entries = searchDao.getEntries(filter, entry.doClass, entry.dao) ?: return null
    if (entries.isEmpty()) {
      return null
    }
    val editPage = rest.getStandardEditPage()
    var hasMore = false
    val hits = entries.mapNotNull { data ->
      val dataObject = data.dataObject ?: run { hasMore = true; return@mapNotNull null }
      val id = (dataObject as? IdObject<*>)?.id?.let { (it as? Number)?.toLong() } ?: return@mapNotNull null
      SearchHit(
        id = id,
        displayName = (dataObject as? DisplayNameCapable)?.displayName ?: dataObject.toString(),
        viewUrl = editPage.replace(NextMigration.ID_PLACEHOLDER, id.toString()),
      )
    }
    if (hits.isEmpty()) {
      return null
    }
    // Where "more in <area>" leads: the area's own native list page, so the whole area is searched there
    // with the term pre-filled (the frontend appends `?q=<term>`). A next list seeds its search box from
    // `?q=` client-side (see the next EntityListPage); a React/generic list is seeded server-side in
    // AbstractPagesRest.requestInitialList, which the React app feeds the url's query params. Keyed by the
    // rest category, not the registry id, so the `orderBook` → `order` route difference is resolved right.
    val listUrl = NextMigration.listUrl(rest.category)
    return SearchAreaResult(entry.id, translate(entry.i18nTitleHeading), hasMore, hits, listUrl)
  }

  /**
   * The Registry areas the logged-in user may search, ordered by [PRIORITY_AREAS] then registration order.
   *
   * Restricted to the [ALLOWED_AREAS] whitelist, and within it to areas the user may *read*
   * ([BaseDao.hasLoggedInUserSelectAccess] — the read right the requirement asks for, so customer/project/employee
   * appear only for users who may see their entries). Areas that need an unmet second factor are dropped as well (see
   * [needsUnmet2FA]) so the aggregated search can't leak a 2FA-protected area's rows.
   */
  private fun accessibleAreas(): List<RegistryEntry> {
    return Registry.getInstance().orderedList
      .filter { entry ->
        entry.id in ALLOWED_AREAS &&
            entry.isSearchable &&
            restByDoClass.containsKey(entry.getDOClass()) &&
            entry.dao.hasLoggedInUserSelectAccess(false) &&
            !needsUnmet2FA(entry)
      }
      .sortedBy { priorityIndex(it.id) }
  }

  /**
   * True if the area's rest path is 2FA-protected and the user's factor is missing or expired. A read across many
   * areas shouldn't interrupt for one, so such an area is silently excluded rather than prompting.
   */
  private fun needsUnmet2FA(entry: RegistryEntry): Boolean {
    val rest = restByDoClass[entry.getDOClass()] ?: return false
    val remaining = my2FARequestHandler.getRemainingPeriod("${Rest.URL}/${rest.category}") ?: return false
    return remaining <= 0
  }

  private fun priorityIndex(areaId: String): Int {
    val idx = PRIORITY_AREAS.indexOf(areaId)
    return if (idx >= 0) idx else PRIORITY_AREAS.size
  }

  /** Prefer the rest whose category equals the area id (its list page) over another rest sharing the DO class. */
  private fun prefer(
    candidate: AbstractEntityRest<*, *, *>,
    existing: AbstractEntityRest<*, *, *>,
    doClass: Class<*>,
  ): Boolean {
    @Suppress("UNCHECKED_CAST")
    val entry = Registry.getInstance().getEntryByDO(doClass as Class<out org.projectforge.framework.persistence.api.BaseDO<*>>)
    val areaId = entry?.id ?: return false
    return candidate.category == areaId && existing.category != areaId
  }

  companion object {
    private const val DEFAULT_MAX_PER_AREA = 10
    private const val MAX_PER_AREA_LIMIT = 50

    /**
     * The only registry areas that take part in the global search: addresses, customers, projects, users, groups,
     * tasks, books and employees (registry ids, see `DaoConst`). Every other area is excluded, whatever its
     * `isSearchable` flag says. Customer/project/employee are additionally read-access gated per user (see
     * [accessibleAreas]).
     */
    private val ALLOWED_AREAS = setOf(
      DaoConst.ADDRESS,
      DaoConst.CUSTOMER,
      DaoConst.PROJECT,
      DaoConst.USER,
      DaoConst.GROUP,
      DaoConst.TASK,
      DaoConst.BOOK,
      DaoConst.EMPLOYEE,
    )

    /**
     * The order the areas are shown in, addresses first — the product-defined order of [ALLOWED_AREAS]. An id not
     * listed here (there is none, by construction) would sort last.
     */
    private val PRIORITY_AREAS = listOf(
      DaoConst.ADDRESS,
      DaoConst.CUSTOMER,
      DaoConst.PROJECT,
      DaoConst.USER,
      DaoConst.GROUP,
      DaoConst.TASK,
      DaoConst.BOOK,
      DaoConst.EMPLOYEE,
    )
  }
}

/** One searchable area (a [RegistryEntry]) the current user may use. */
class SearchArea(val areaId: String, val title: String)

/** A single hit, ready for the frontend: [viewUrl] is `resolveMenuUrl`-ready (e.g. `next/book/17`, `react/konto/edit/5`). */
class SearchHit(
  val id: Long,
  val displayName: String,
  val secondaryInfo: String? = null,
  val viewUrl: String,
)

/** The hits of one area; [hasMore] mirrors the Wicket "more entries" marker. */
class SearchAreaResult(
  val areaId: String,
  val title: String,
  val hasMore: Boolean,
  val hits: List<SearchHit>,
  /**
   * The area's own native list page (`resolveMenuUrl`-ready, e.g. `next/book` or `react/project`), where
   * "more in …" leads. The frontend appends `?q=<term>` so the list opens pre-filtered.
   */
  val listUrl: String,
)

class SearchResponse(val term: String, val areas: List<SearchAreaResult>)
