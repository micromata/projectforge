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

package org.projectforge.business.fibu

import org.hibernate.Hibernate
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.business.fibu.numberrange.NumberRangeService
import org.projectforge.business.fibu.numberrange.NumberRangeViolation
import org.projectforge.business.orga.VisitorbookEntryDO
import org.projectforge.business.task.TaskDao
import org.projectforge.business.task.TaskTree
import org.projectforge.business.user.GroupDao
import org.projectforge.business.user.UserRightId
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.QueryFilter.Companion.eq
import org.projectforge.framework.persistence.api.QueryFilter.Companion.isNull
import org.projectforge.framework.persistence.api.QueryFilter.Companion.ne
import org.projectforge.framework.persistence.api.QueryFilter.Companion.or
import org.projectforge.framework.persistence.api.SortProperty.Companion.asc
import org.projectforge.framework.persistence.history.HistoryLoadContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

// private val log = KotlinLogging.logger {}

@Service
open class ProjektDao : BaseDao<ProjektDO>(ProjektDO::class.java) {
    @Autowired
    private lateinit var kostCache: KostCache

    @Autowired
    private lateinit var kundeDao: KundeDao

    @Autowired
    private lateinit var numberRangeService: NumberRangeService

    @Autowired
    private lateinit var groupDao: GroupDao

    @Autowired
    private lateinit var taskDao: TaskDao

    @Autowired
    private lateinit var taskTree: TaskTree

    init {
        this.supportAfterUpdate = true
        userRightId = USER_RIGHT_ID
    }

    override val additionalSearchFields: Array<String>
        get() = ADDITIONAL_SEARCH_FIELDS

    /**
     * @param projekt
     * @param kundeId If null, then kunde will be set to null;
     * @see BaseDao.findOrLoad
     */
    fun setKunde(projekt: ProjektDO, kundeId: Long?) {
        if (kundeId == null) {
            projekt.kunde = null
        } else {
            val kunde = kundeDao.findOrLoad(kundeId)
            projekt.kunde = kunde
        }
    }

    /**
     * @param projekt
     * @param taskId  If null, then task will be set to null;
     * @see BaseDao.findOrLoad
     */
    fun setTask(projekt: ProjektDO, taskId: Long?) {
        if (taskId == null) {
            projekt.task = null
        } else {
            val task = taskDao.findOrLoad(taskId)
            projekt.task = task
        }
    }

    fun setProjektManagerGroup(projekt: ProjektDO, groupId: Long?) {
        if (groupId == null) {
            projekt.projektManagerGroup = null
        } else {
            val group = groupDao.findOrLoad(groupId)
            projekt.projektManagerGroup = group
        }
    }

    /**
     * Initializes the projekt (projektManagerGroup), so any LazyInitializationException are avoided.
     *
     * @param projekt Null safe.
     */
    fun initializeProjektManagerGroup(projekt: ProjektDO?) {
        if (projekt == null) {
            return
        }
        // Needed because Hibernate Search rolls back because the project manager group is not loadable.
        Hibernate.initialize(projekt)
        val projectManagerGroup = projekt.projektManagerGroup
        if (projectManagerGroup != null) {
            val group = groupDao.find(projectManagerGroup.id, checkAccess = false)
            projekt.projektManagerGroup = group
            //Hibernate.initialize(projectManagerGroup); // Does not work.
        }
    }

    fun getProjekt(kunde: KundeDO?, nummer: Long): ProjektDO? {
        return persistenceService.selectSingleResult(
            "SELECT p FROM ProjektDO p WHERE p.kunde = :kunde and p.nummer = :nummer",
            ProjektDO::class.java,
            Pair("kunde", kunde),
            Pair("nummer", nummer)
        )
    }

    open fun getProjektByProjektnummer(projektnummer: Int): ProjektDO? {
        return persistenceService.selectSingleResult(
            "SELECT p FROM ProjektDO p WHERE p.projektnummer = :projektnummer",
            ProjektDO::class.java,
            Pair("projektnummer", projektnummer),
        )
    }

    fun getProjekt(intern_kost2_4: Int, nummer: Int): ProjektDO? {
        return persistenceService.selectNamedSingleResult(
            ProjektDO.FIND_BY_INTERNKOST24_AND_NUMMER,
            ProjektDO::class.java,
            Pair("internKost24", intern_kost2_4),
            Pair("nummer", nummer),
        )
    }

    override fun select(filter: BaseSearchFilter): List<ProjektDO> {
        val myFilter = if (filter is ProjektFilter) {
            filter
        } else {
            ProjektFilter(filter)
        }
        val queryFilter = QueryFilter(myFilter)
        if (myFilter.isEnded) {
            queryFilter.add(eq("status", ProjektStatus.ENDED))
        } else if (myFilter.isNotEnded) {
            queryFilter.add(or(ne("status", ProjektStatus.ENDED), isNull("status")))
        }
        queryFilter.addOrder(asc("projektnummer"))
        return select(queryFilter)
    }

    fun getKundenProjekte(kundeId: Int?): List<ProjektDO?>? {
        if (kundeId == null) {
            return null
        }
        val queryFilter = QueryFilter()
        queryFilter.add(eq("kunde.id", kundeId))
        queryFilter.addOrder(asc("nummer"))
        return select(queryFilter)
    }

    override fun onInsertOrModify(obj: ProjektDO, operationType: OperationType) {
        if (obj.status == ProjektStatus.NONE) {
            obj.status = null
        }
        if (operationType == OperationType.INSERT || operationType == OperationType.UPDATE) {
            assignProjektnummer(obj)
            // Legacy field, the local number of a project without customer covers all digits (see ProjektDO):
            obj.internKost2_4 = null
        }
    }

    /**
     * Sets [ProjektDO.projektnummer]:
     * - customer of a number range: computed from customer and local [ProjektDO.nummer],
     * - otherwise the given number (or the next free one, if not given). A given number of a range without
     *   customers (e.g. 412345 for internal projects) sets the local number of a project without customer. A free
     *   number must not lie in the numbers of a range.
     *
     * @throws UserException if the numbers don't fit the configured number ranges or the number is already used.
     */
    internal fun assignProjektnummer(obj: ProjektDO) {
        val numberRanges = numberRangeService.numberRanges
        val kundeNummer = obj.kunde?.nummer
        val customerRange = numberRanges.rangeForCustomer(kundeNummer)
        if (customerRange != null) {
            if (obj.nummer !in 0..customerRange.maxLocalNumber) {
                throw UserException(LOCAL_NUMBER_OUT_OF_RANGE_I18N_KEY, customerRange.maxLocalNumber)
            }
            obj.projektnummer = numberRanges.projectNumber(customerRange, kundeNummer, obj.nummer)
        } else {
            if (kundeNummer != null) {
                numberRanges.validateCustomerNumber(kundeNummer)?.let { throw UserException(it.i18nKey) }
            }
            val projektnummer = obj.projektnummer
                ?: legacyProjektnummer(obj.kunde != null, obj.internKost2_4, obj.nummer)
                ?: nextFreeProjektnummer(obj.kunde == null)
            val range = numberRanges.rangeForProjectNumber(projektnummer)
            if (range != null) {
                if (obj.kunde != null || range.hasCustomers) {
                    throw UserException(NumberRangeViolation.FREE_NUMBER_IN_RANGE.i18nKey)
                }
                obj.nummer = numberRanges.localNumber(projektnummer)!!
            } else {
                numberRanges.validateFreeProjectNumber(projektnummer)?.let { throw UserException(it.i18nKey) }
            }
            obj.projektnummer = projektnummer
        }
        val other = persistenceService.selectSingleResult(
            "SELECT p.id FROM ProjektDO p WHERE p.projektnummer = :projektnummer",
            java.lang.Long::class.java,
            Pair("projektnummer", obj.projektnummer),
        )
        // Scalar query: doesn't load (and detach) another project instance inside the update.
        if (other != null && other.toLong() != obj.id) {
            throw UserException(PROJEKTNUMMER_EXISTS_I18N_KEY, obj.projektnummer)
        }
    }

    /**
     * A project without customer and project number may be given in the positional Kost2 format: legacy
     * [ProjektDO.internKost2_4] as digits 2-4 and [ProjektDO.nummer] as digits 5-6 (e.g. 4.123.45), if exactly one
     * range without customers is configured.
     */
    open fun legacyProjektnummer(hasKunde: Boolean, internKost2_4: Int?, nummer: Int): Int? {
        if (hasKunde || internKost2_4 == null || internKost2_4 !in 0..999 || nummer !in 0..99) {
            return null
        }
        val range = numberRangeService.numberRanges.rangeWithoutCustomers ?: return null
        return range.firstProjectNumber + internKost2_4 * 100 + nummer
    }

    /**
     * Without customer the next free number of the range without customers (if configured), otherwise the next
     * free number above all free project numbers.
     */
    private fun nextFreeProjektnummer(withoutCustomer: Boolean): Int {
        val numberRanges = numberRangeService.numberRanges
        val used = persistenceService.executeQuery(
            "SELECT p.projektnummer FROM ProjektDO p WHERE p.projektnummer IS NOT NULL",
            Integer::class.java,
        ).map { it.toInt() }
        if (withoutCustomer) {
            numberRanges.rangeWithoutCustomers?.let { range ->
                val usedLocal = used.filter { range.containsProjectNumber(it) }.map { it - range.firstProjectNumber }
                numberRanges.nextFreeLocalNumber(range, usedLocal)?.let { return range.firstProjectNumber + it }
            }
        }
        return (used.filter { numberRanges.rangeForProjectNumber(it) == null }.maxOrNull() ?: 0) + 1
    }

    /**
     * The number of a project (nummernkreis, bereich, teilbereich, i.e. customer or internal range and
     * [ProjektDO.nummer]) is fixed once it has cost 2 units: each [Kost2DO] got a copy of it when it was
     * created ([org.projectforge.business.fibu.kost.Kost2Dao.setProjekt]), and nothing renumbers them, so
     * the project and its cost 2 units would part ways — and so would everything booked on them.
     */
    override fun onUpdate(obj: ProjektDO, dbObj: ProjektDO) {
        if (isNumberLocked(obj.id) && isNumberChanged(obj, dbObj)) {
            throw UserException(NUMBER_LOCKED_I18N_KEY)
        }
    }

    /**
     * True if the project has cost 2 units, deleted ones included (they may be undeleted). Its number and
     * customer can't be changed then, see [onUpdate].
     */
    open fun isNumberLocked(projektId: Long?): Boolean {
        projektId ?: return false
        return kostCache.getKost2ForProjekt(projektId, includeDeleted = true).isNotEmpty()
    }

    private fun isNumberChanged(obj: ProjektDO, dbObj: ProjektDO): Boolean {
        return obj.projektnummer != dbObj.projektnummer
    }

    override fun afterInsertOrModify(obj: ProjektDO, operationType: OperationType) {
        obj.task?.id?.let { taskId ->
            taskTree.internalSetProject(taskId, obj)
        }
    }

    override fun afterUpdate(obj: ProjektDO, dbObj: ProjektDO?, isModified: Boolean) {
        if (dbObj?.task?.id != null && obj.task?.id == null) {
            // Project task was removed:
            taskTree.internalSetProject(dbObj.task?.id!!, null)
        }
    }

    /**
     * Gets history entries of super and adds all history entries of the RechnungsPositionDO children.
     */
    override fun addOwnHistoryEntries(obj: ProjektDO, context: HistoryLoadContext) {
        // Batch the children's history (one query) instead of once per instance, see
        // HistoryService.loadAndMergeHistory(entityClass, entityIds, ...).
        kostCache.getKost2ForProjekt(obj.id).mapNotNull { it.id }.takeIf { it.isNotEmpty() }?.let { ids ->
            historyService.loadAndMergeHistory(Kost2DO::class.java, ids, context)
        }
    }

    // Kost2DO is the doClass of Kost2Dao as well, so its history is covered by two list pages. Harmless: a partial
    // re-index doesn't purge, so both runs merely refresh the same documents.
    override val additionalHistoryEntityClasses: List<Class<*>> = listOf(Kost2DO::class.java)

    override fun getHistoryPropertyPrefix(context: HistoryLoadContext): String? {
        val entry = context.requiredHistoryEntry
        val item = context.findLoadedEntity(entry)
        return if (item is Kost2DO) {
            item.formattedNumber
        } else {
            null
        }
    }

    override fun newInstance(): ProjektDO {
        return ProjektDO()
    }

    companion object {
        val USER_RIGHT_ID: UserRightId = UserRightId.PM_PROJECT

        const val NUMBER_LOCKED_I18N_KEY = "fibu.projekt.validation.numberLocked"
        const val PROJEKTNUMMER_EXISTS_I18N_KEY = "fibu.projekt.validation.projektnummerExists"
        const val LOCAL_NUMBER_OUT_OF_RANGE_I18N_KEY = "fibu.projekt.validation.localNumberOutOfRange"
        private val ADDITIONAL_SEARCH_FIELDS = arrayOf(
            "kunde.name", "kunde.division", "projektManagerGroup.name",
            "headOfBusinessManager.username", "headOfBusinessManager.firstname", "headOfBusinessManager.lastname",
            "salesManager.username", "salesManager.firstname", "salesManager.lastname",
        )
    }
}
