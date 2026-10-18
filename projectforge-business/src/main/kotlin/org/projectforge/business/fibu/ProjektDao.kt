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
import org.projectforge.framework.persistence.api.EntityCopyStatus
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
     *   number must not lie in the numbers of a range; it is the local number of the project, too.
     *
     * @throws UserException if the numbers don't fit the configured number ranges or the number is already used.
     */
    internal fun assignProjektnummer(obj: ProjektDO) {
        val numberRanges = numberRangeService.numberRanges
        val kundeNummer = obj.kunde?.nummer
        val customerRange = numberRanges.rangeForCustomer(kundeNummer)
        if (customerRange != null) {
            obj.nummer = localNummer(obj)
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
                ?: nextFreeProjektnummer(obj)
            val range = numberRanges.rangeForProjectNumber(projektnummer)
            if (range != null) {
                if (obj.kunde != null || range.hasCustomers) {
                    throw UserException(NumberRangeViolation.FREE_NUMBER_IN_RANGE.i18nKey)
                }
                obj.nummer = numberRanges.localNumber(projektnummer)!!
            } else {
                numberRanges.validateFreeProjectNumber(projektnummer)?.let { throw UserException(it.i18nKey) }
                // A free project has no local number of its own (unique per customer as the project number is):
                obj.nummer = projektnummer
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
        // Another project of the customer may still hold a free number as local number (e.g. one left without project
        // number by the migration), and the local numbers are unique per customer, deleted ones included:
        if (customerRange == null && localNumbersOfCustomer(obj).contains(obj.nummer)) {
            throw UserException(NUMBER_EXISTS_FOR_CUSTOMER_I18N_KEY)
        }
    }

    /**
     * The local number of the project within the number range of its customer. A project saved as free project
     * (no range configured then) stores its project number as local number, see [assignProjektnummer]. If its
     * project number lies in the range of its customer now (e.g. the legacy ranges with Kost2 activated), the
     * local number is derived from the project number.
     */
    open fun localNummer(obj: ProjektDO): Int {
        val projektnummer = obj.projektnummer
        val kundeNummer = obj.kunde?.nummer
        if (projektnummer == null || kundeNummer == null || obj.nummer != projektnummer) {
            return obj.nummer
        }
        val numberRanges = numberRangeService.numberRanges
        if (numberRanges.rangeForCustomer(kundeNummer) == null || numberRanges.customerNumber(projektnummer) != kundeNummer) {
            return obj.nummer
        }
        return numberRanges.localNumber(projektnummer) ?: obj.nummer
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
     * The next free local number of a project within the number range of its customer [kunde] (or, without customer,
     * of the range without customers), null without such a range or if it is exhausted. Taken are the local numbers
     * of the customer's other projects (deleted ones included) and the project numbers used in the range.
     *
     * @param projectId The project itself, whose own number doesn't count as taken.
     */
    open fun nextFreeLocalNumber(kunde: KundeDO?, projectId: Long?): Int? {
        val numberRanges = numberRangeService.numberRanges
        val kundeNummer = kunde?.nummer
        val range = if (kunde != null) {
            numberRanges.rangeForCustomer(kundeNummer) ?: return null
        } else {
            numberRanges.rangeWithoutCustomers ?: return null
        }
        val usedInRange = persistenceService.executeQuery(
            "SELECT p.id, p.projektnummer FROM ProjektDO p WHERE p.projektnummer BETWEEN :first AND :last",
            Array<Any>::class.java,
            Pair("first", range.firstProjectNumber),
            Pair("last", range.lastProjectNumber),
        ).filter { (it[0] as Number).toLong() != projectId }
            .map { (it[1] as Number).toInt() }
            .filter { !range.hasCustomers || numberRanges.customerNumber(it) == kundeNummer }
            .mapNotNull { numberRanges.localNumber(it) }
        val used = usedInRange + localNumbersOfCustomer(kunde?.id, projectId)
        return numberRanges.nextFreeLocalNumber(range, used)
    }

    /**
     * Without customer the next free number of the range without customers (if configured), otherwise the next
     * free number above all free project numbers, which no other project of the customer has as local number.
     */
    private fun nextFreeProjektnummer(obj: ProjektDO): Int {
        val numberRanges = numberRangeService.numberRanges
        if (obj.kunde == null) {
            numberRanges.rangeWithoutCustomers?.let { range ->
                nextFreeLocalNumber(null, obj.id)?.let { return range.firstProjectNumber + it }
            }
        }
        val used = persistenceService.executeQuery(
            "SELECT p.projektnummer FROM ProjektDO p WHERE p.projektnummer IS NOT NULL",
            Integer::class.java,
        ).map { it.toInt() }
        var next = (used.filter { numberRanges.rangeForProjectNumber(it) == null }.maxOrNull() ?: 0) + 1
        val localNumbers = localNumbersOfCustomer(obj)
        while (localNumbers.contains(next)) {
            next++
        }
        return next
    }

    /** The local numbers of the other projects of the customer of [obj], deleted ones included (none without customer). */
    private fun localNumbersOfCustomer(obj: ProjektDO): Set<Int> = localNumbersOfCustomer(obj.kunde?.id, obj.id)

    /** The local numbers of the projects of the customer except [projectId], deleted ones included. */
    private fun localNumbersOfCustomer(kundeId: Long?, projectId: Long?): Set<Int> {
        kundeId ?: return emptySet()
        return persistenceService.executeQuery(
            "SELECT p.id, p.nummer FROM ProjektDO p WHERE p.kunde.id = :kundeId",
            Array<Any>::class.java,
            Pair("kundeId", kundeId),
        ).filter { (it[0] as Number).toLong() != projectId }.map { (it[1] as Number).toInt() }.toSet()
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
        // A legacy project without number (not migrated) gets one on its next save:
        return dbObj.projektnummer != null && obj.projektnummer != dbObj.projektnummer
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

    override fun insert(obj: ProjektDO, checkAccess: Boolean): Long =
        translateProjektnummerCollision(obj) { super.insert(obj, checkAccess) }

    override fun update(obj: ProjektDO, checkAccess: Boolean): EntityCopyStatus =
        translateProjektnummerCollision(obj) { super.update(obj, checkAccess) }

    /**
     * The check of [assignProjektnummer] runs before the flush: two projects saved at the same time may get the
     * same (next free) number, the second then fails on the unique constraint. Answered as the check would, so
     * saving again takes the next free number.
     */
    private fun <T> translateProjektnummerCollision(obj: ProjektDO, block: () -> T): T {
        try {
            return block()
        } catch (ex: Exception) {
            if (isProjektnummerCollision(ex)) {
                throw UserException(PROJEKTNUMMER_EXISTS_I18N_KEY, obj.projektnummer)
            }
            throw ex
        }
    }

    companion object {
        val USER_RIGHT_ID: UserRightId = UserRightId.PM_PROJECT

        const val NUMBER_LOCKED_I18N_KEY = "fibu.projekt.validation.numberLocked"
        const val PROJEKTNUMMER_EXISTS_I18N_KEY = "fibu.projekt.validation.projektnummerExists"

        /** The local number is unique per customer (constraint of nummer and kunde). */
        const val NUMBER_EXISTS_FOR_CUSTOMER_I18N_KEY = "fibu.projekt.validation.numbernotfreeforcustomer"
        const val LOCAL_NUMBER_OUT_OF_RANGE_I18N_KEY = "fibu.projekt.validation.localNumberOutOfRange"

        /** The unique constraint of [ProjektDO.projektnummer] (Flyway V9.0.8.4). */
        private const val PROJEKTNUMMER_CONSTRAINT = "unique_t_fibu_projekt_projektnummer"

        /** Whether the exception (or a cause) names the constraint; HSQLDB reports it in upper case. */
        internal fun isProjektnummerCollision(ex: Throwable): Boolean =
            generateSequence(ex) { it.cause.takeIf { cause -> cause !== it } }
                .any { it.message?.contains(PROJEKTNUMMER_CONSTRAINT, ignoreCase = true) == true }
        private val ADDITIONAL_SEARCH_FIELDS = arrayOf(
            "kunde.name", "kunde.division", "projektManagerGroup.name",
            "headOfBusinessManager.username", "headOfBusinessManager.firstname", "headOfBusinessManager.lastname",
            "salesManager.username", "salesManager.firstname", "salesManager.lastname",
        )
    }
}
