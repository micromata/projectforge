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

package org.projectforge.framework.persistence.api

import jakarta.persistence.criteria.Root
import mu.KotlinLogging
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.Validate
import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.user.UserRight
import org.projectforge.common.i18n.UserException
import org.projectforge.common.mgc.MGCClassUtils
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.persistence.api.impl.DBQuery
import org.projectforge.framework.persistence.api.impl.HibernateSearchMeta.getClassInfo
import org.projectforge.framework.persistence.database.DatabaseDao
import org.projectforge.framework.persistence.database.DatabaseDao.Companion.createReindexSettings
import org.projectforge.framework.persistence.history.*
import org.projectforge.framework.persistence.history.HistoryLoadContext
import org.projectforge.framework.persistence.jpa.PersistenceCallsRecorder
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.search.HibernateSearchDependentObjectsReindexer
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.loggedInUser
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.requiredLoggedInUser
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDateTime.Companion.now
import org.springframework.beans.factory.annotation.Autowired
import java.io.Serializable
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
abstract class BaseDao<O : ExtendedBaseDO<Long>>
/**
 * The setting of the DO class is required.
 */
protected constructor(open var doClass: Class<O>) : IDao<O>, BaseDaoPersistenceListener<O> {
    protected val baseDOChangedRegistry = BaseDOChangedRegistry<O>(this)

    internal val changedRegistry = baseDOChangedRegistry

    var identifier: String? = null
        /**
         * Identifier should be unique in application (including all plugins). This identifier is also used as category in rest services
         * or in React pages.
         * At default, it's the simple name of the DO clazz without extension "DO".
         */
        get() {
            if (field == null) {
                field = StringUtils.uncapitalize(
                    StringUtils.removeEnd(
                        doClass.simpleName,
                        "DO"
                    )
                )
            }
            return field
        }
        private set

    @JvmField
    var logDatabaseActions: Boolean = true

    @Autowired
    lateinit var accessChecker: AccessChecker

    @Autowired
    protected lateinit var baseDOPersistenceService: BaseDOPersistenceService

    @Autowired
    protected lateinit var dbQuery: DBQuery

    @Autowired
    protected lateinit var databaseDao: DatabaseDao

    /**
     * @return the UserGroupCache with groups and rights .
     */
    @Autowired
    protected lateinit var userGroupCache: UserGroupCache

    @get:Synchronized
    val searchFields: Array<String>
        /**
         * Get all declared hibernate search fields. These fields are defined over annotations in the database object class.
         * The names are the property names or, if defined the name declared in the annotation of a field. <br></br>
         * The user can search in these fields explicit by typing e. g. authors:beck (<field>:<searchString>)
        </searchString></field> */
        get() = getClassInfo(this).allFieldNames

    @JvmField
    protected var userRightId: IUserRightId? = null

    /**
     * Should the id check (on null) be avoided before save (in save method)? This is use-full if the derived dao manages
     * the id itself (as e. g. KundeDao, Kost2ArtDao).
     */
    @JvmField
    protected var avoidNullIdCheckBeforeSave: Boolean = false

    /**
     * Set this to true if you overload [.afterUpdate] and you need the origin data
     * base entry in this method.
     */
    @JvmField
    var supportAfterUpdate: Boolean = false

    @JvmField
    protected var idProperty: String = "id"

    /**
     * If true, deletion of historizable objects is supported (including any history entry) due to privacy protection (e. g. addresses).
     * Otherwise, objects may only be marked as deleted (default).
     */
    var isForceDeletionSupport: Boolean = false
        protected set

    @Autowired
    open lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var userRights: UserRightService

    @Autowired
    private lateinit var hibernateSearchDependentObjectsReindexer: HibernateSearchDependentObjectsReindexer

    @Autowired
    protected lateinit var historyService: HistoryService

    open val additionalSearchFields: Array<String>?
        /**
         * Overwrite this method for adding search fields manually (e. g. for embedded objects). For example see TimesheetDao.
         */
        get() = null

    open val defaultSortProperties: Array<SortProperty>?
        /**
         * Overwrite this method for having a standard sort of result lists (supported by [BaseDao.select].
         */
        get() = null

    abstract fun newInstance(): O

    /**
     * If the user has select access then the object will be returned. If not, the hibernate proxy object will be get via
     * [jakarta.persistence.EntityManager.getReference] and returned. If the object is not found, null will be returned.
     */
    @JvmOverloads
    fun findOrLoad(id: Long?, checkAccess: Boolean = true): O? {
        val obj = findById(id) ?: return null
        if (!checkAccess || hasLoggedInUserSelectAccess(obj, false)) {
            baseDOChangedRegistry.afterLoad(obj)
            return obj
        }
        return persistenceService.runReadOnly { context ->
            context.em.getReference(doClass, id)
        }
    }

    /**
     * If the user has select access then the object will be returned.
     * If not, the hibernate proxy object will be get via
     * [jakarta.persistence.EntityManager.getReference] and returned.
     * If the object is not found, null will be returned.
     * Calls [BaseDOChangedRegistry.afterLoad] also for the given object, if found and the user has access.
     * @param id primary key of the base object.
     * @param attached if true, the object will not be detached. Default is false.
     * @return the object with the given id or null if not found.
     */
    @Throws(AccessException::class)
    @JvmOverloads
    open fun find(id: Serializable?, checkAccess: Boolean = true, attached: Boolean = false): O? {
        val obj = findById(id, attached = attached) ?: return null
        if (checkAccess) {
            checkLoggedInUserSelectAccess(obj)
        }
        baseDOChangedRegistry.afterLoad(obj)
        return obj
    }

    private fun findById(id: Serializable?, attached: Boolean = false): O? {
        id ?: return null
        val obj = persistenceService.find(doClass, id, attached = attached)
        if (obj == null) {
            log.error { "Can't load object of type ${doClass.name}. Object with given id #$id not found." }
            return null
        }
        return obj
    }

    @JvmOverloads
    fun selectAllNotDeleted(checkAccess: Boolean = true): List<O> {
        return select(deleted = false, checkAccess)
    }

    /**
     * @return All objects of this class or empty list if no object found
     */
    @JvmOverloads
    open fun selectAll(checkAccess: Boolean = true): List<O> {
        return select(deleted = null, checkAccess = checkAccess, useRoot = null)
    }

    /**
     * @return All objects of this class or empty list if no object found
     */
    fun select(deleted: Boolean? = null, checkAccess: Boolean = true, useRoot: ((Root<O>) -> Unit)? = null): List<O> {
        if (checkAccess) {
            checkLoggedInUserSelectAccess()
        }
        val list = persistenceService.runReadOnly { context ->
            val em = context.em
            // Native query: doClass.simpleName doesn't match the table name in the database.
            // val query = em.createNativeQuery("SELECT * FROM ${doClass.simpleName}", doClass)
            // val query = em.createQuery("SELECT e FROM ${doClass.simpleName} e", doClass)
            // @Suppress("UNCHECKED_CAST")
            // return query.resultList as List<O>

            val cb = em.criteriaBuilder
            val cq = cb.createQuery(doClass)
            val root = cq.from(doClass)
            useRoot?.let {
                useRoot(root)
            }
            cq.select(root)
            if (deleted != null) {
                val deletedPredicate = cb.equal(root.get<Boolean>("deleted"), deleted)
                cq.where(deletedPredicate)  // Fügen Sie die Bedingung zur Abfrage hinzu
            }
            val query = em.createQuery(cq)
            context.logAndAdd(PersistenceCallsRecorder.CallType.QUERY, doClass.simpleName, "selectAll")
            query.resultList
        }
        return filterAccess(list, checkAccess = checkAccess, callAfterLoad = true)
    }

    fun select(idList: Collection<Serializable>?, checkAccess: Boolean = true): List<O>? {
        if (idList == null) {
            return null
        }
        if (checkAccess) {
            checkLoggedInUserSelectAccess()
        }
        val list = persistenceService.runReadOnly { context ->
            val em = context.em
            val cr = em.criteriaBuilder.createQuery(doClass)
            val root = cr.from(doClass)
            cr.select(root).where(root.get<Any>(idProperty).`in`(idList)).distinct(true)
            context.logAndAdd(
                PersistenceCallsRecorder.CallType.QUERY,
                doClass.simpleName,
                "select by ids=${idList.joinToString()}"
            )
            em.createQuery(cr).resultList
        }
        return filterAccess(list, checkAccess = checkAccess, callAfterLoad = true)
    }

    /**
     * This method is used by the searchDao and calls [.getList] by default.
     *
     * @return A list of found entries or empty list. PLEASE NOTE: Returns null only if any error occured.
     * @see .getList
     */
    @JvmOverloads
    open fun selectForSearchDao(filter: BaseSearchFilter, checkAccess: Boolean = true): List<O> {
        return select(filter, checkAccess)
    }

    /**
     * Builds query filter by simply calling constructor of QueryFilter with given search filter and calls
     * getList(QueryFilter). Override this method for building more complex query filters.
     *
     * @return A list of found entries or empty list. PLEASE NOTE: Returns null only if any error occured.
     */
    override fun select(filter: BaseSearchFilter): List<O> {
        return select(filter, true)
    }

    open fun select(filter: BaseSearchFilter, checkAccess: Boolean = true): List<O> {
        val queryFilter = createQueryFilter(filter)
        return select(queryFilter, checkAccess)
    }

    open fun createQueryFilter(filter: BaseSearchFilter? = null): QueryFilter {
        return QueryFilter(filter)
    }

    /**
     * Gets the list filtered by the given filter.
     */
    @Throws(AccessException::class)
    @JvmOverloads
    open fun select(filter: QueryFilter, checkAccess: Boolean = true): List<O> {
        return select(filter, null, checkAccess)
    }

    /**
     * Gets the list filtered by the given filter.
     */
    @Throws(AccessException::class)
    @JvmOverloads
    open fun select(
        filter: QueryFilter,
        customResultFilters: List<CustomResultFilter<O>>?,
        checkAccess: Boolean = true,
    ): List<O> {
        if (checkAccess) {
            checkLoggedInUserSelectAccess()
        }
        val list = dbQuery.select(this, filter, customResultFilters, checkAccess)
        baseDOChangedRegistry.afterLoad(list)
        return list
    }

    /**
     * idSet.contains(entry.getId()) at default.
     */
    open fun contains(idSet: Set<Long>?, entry: O): Boolean {
        return idSet?.contains(entry.id) == true
    }

    /**
     * Filter the given list by calling hasSelectAccess for every object in the list. If checkAccess is false, the list
     * will be returned without any filtering. If callAfterLoad is true, afterLoad will be called for every object in the
     * list.
     */
    protected fun filterAccess(origList: List<O>, checkAccess: Boolean, callAfterLoad: Boolean): List<O> {
        val result = if (checkAccess) {
            origList.filter { hasSelectAccess(it, requiredLoggedInUser) }
        } else {
            origList
        }
        if (callAfterLoad) {
            result.forEach { baseDOChangedRegistry.afterLoad(it) }
        }
        return result
    }

    /**
     * @param obj          The object to check.
     * @param loggedInUser The current logged-in user.
     * @return true if loggedInUser has select access.
     * @see .hasUserSelectAccess
     */
    fun hasSelectAccess(obj: O, loggedInUser: PFUserDO): Boolean {
        return hasUserSelectAccess(loggedInUser, obj, false)
    }

    /**
     * Gets the history entries of the object.
     * If the user has no access an empty list will be returned.
     */
    @JvmOverloads
    fun loadHistory(
        obj: O,
        checkAccess: Boolean = true,
        loadContext: HistoryLoadContext = HistoryLoadContext(this),
    ): HistoryLoadContext {
        if (obj.id == null || (checkAccess && !hasLoggedInUserHistoryAccess(obj, false))) {
            return loadContext
        }
        historyService.loadAndMergeHistory(obj, loadContext)
        addOwnHistoryEntries(obj, loadContext)
        // Sorts the list by the 'modifiedAt' field, placing entries with null values at the beginning.
        // The first part of the comparator checks if 'modifiedAt' is null and gives it higher priority
        // (placing null entries first). The second part sorts the remaining entries based on the
        // non-null 'modifiedAt' values.
        return loadContext
    }

    /**
     * Override this method if you want to add your own history entries to the list or modify the existing ones.
     * Called by [loadHistory].
     * Does nothing at default.
     * @param obj The object for which the history entries are loaded.
     * @param list The list with entries loaded for given obj. Add here your own entries.
     */
    protected open fun addOwnHistoryEntries(
        obj: O,
        context: HistoryLoadContext
    ) {
    }

    /**
     * For customizing [DisplayHistoryEntry]'s. The context holds the current processed [HistoryEntry] as well as the current
     * created [DisplayHistoryEntry]. Called after converting to display history entries.
     * The [DisplayHistoryEntryAttr] attributes are already set in the [DisplayHistoryEntry].
     * Get the current [DisplayHistoryEntry] by calling [HistoryLoadContext.requiredDisplayHistoryEntry].
     * Does nothing at default.
     */
    open fun customizeDisplayHistoryEntry(context: HistoryLoadContext) {
    }

    /**
     * For customizing [DisplayHistoryEntryAttr]'s. The context holds the current processed [HistoryEntryAttr] as well as the current
     * created [DisplayHistoryEntryAttr]. Called after converting to display history entries.
     * Get the current [DisplayHistoryEntryAttr] by calling [HistoryLoadContext.requiredDisplayHistoryEntryAttr].
     * Does nothing at default.
     */
    open fun customizeHistoryEntryAttr(context: HistoryLoadContext) {
    }

    /**
     * Creates a new transaction (if not already in a transaction) and calls internalSaveOrUpdate.
     * @return the generated identifier, if save method is used, otherwise null.
     */
    @Throws(AccessException::class)
    @JvmOverloads
    open fun insertOrUpdate(obj: O, checkAccess: Boolean = true): Serializable? {
        var id: Serializable? = null
        if (obj.id != null && obj.created != null) { // obj.created is needed for KundeDO (id isn't null for inserting new customers).
            update(obj, checkAccess = checkAccess)
        } else {
            id = insert(obj, checkAccess = checkAccess)
        }
        return id
    }

    /**
     * Call save(O) for every object in the given list.
     */
    @Throws(AccessException::class)
    @JvmOverloads
    fun insert(objects: List<O>, checkAccess: Boolean = true) {
        persistenceService.runInTransaction { _ ->
            for (obj in objects) {
                insert(obj, checkAccess)
            }
        }
    }

    /**
     * @return the generated identifier.
     */
    @Throws(AccessException::class)
    @JvmOverloads
    open fun insert(obj: O, checkAccess: Boolean = true): Long {
        if (!avoidNullIdCheckBeforeSave) {
            Validate.isTrue(obj.id == null)
        }
        baseDOChangedRegistry.beforeInsertOrModify(obj, OperationType.INSERT)
        return baseDOPersistenceService.insert(this, obj, checkAccess = checkAccess)!!
    }

    @JvmOverloads
    fun insertOrUpdate(col: Collection<O>, checkAccess: Boolean = true) {
        persistenceService.runInTransaction { _ ->
            for (obj in col) {
                insertOrUpdate(obj, checkAccess)
            }
        }
    }

    fun insertOrUpdate(col: Collection<O>, blockSize: Int, checkAccess: Boolean = true) {
        persistenceService.runInTransaction { context ->
            val list = mutableListOf<O>()
            var counter = 0
            for (obj in col) {
                list.add(obj)
                if (++counter >= blockSize) {
                    counter = 0
                    insertOrUpdate(list, checkAccess)
                    context.flush()
                    list.clear()
                }
            }
            insertOrUpdate(list, checkAccess)
        }
    }

    /**
     * @return true, if modifications were done, false if no modification detected.
     * @see .internalUpdate
     */
    @Throws(AccessException::class)
    @JvmOverloads
    open fun update(obj: O, checkAccess: Boolean = true): EntityCopyStatus {
        return baseDOPersistenceService.update(this, obj = obj, checkAccess = checkAccess)
    }

    /**
     * Thin wrapper for generic usage.
     *
     * @return true, if modifications were done, false if no modification detected.
     * @see .internalUpdate
     */
    @Throws(AccessException::class)
    @JvmOverloads
    fun updateAny(obj: Any, checkAccess: Boolean = true): EntityCopyStatus {
        @Suppress("UNCHECKED_CAST")
        return update(obj as O, checkAccess)
    }

    /**
     * @return If true (default if not minor Change) all dependent data-base objects will be re-indexed. For e. g.
     * PFUserDO all time-sheets etc. of this user will be re-indexed. It's called after internalUpdate. Refer
     * UserDao to see more.
     * @see BaseDO.isMinorChange
     */
    open fun wantsReindexAllDependentObjects(obj: O, dbObj: O): Boolean {
        return !obj.isMinorChange
    }

    /**
     * Used by internal update if supportAfterUpdate is true for storing db object version for afterUpdate. Override this
     * method to implement your own copy method.
     */
    open fun getBackupObject(dbObj: O): O {
        val backupObj = newInstance()
        copyValues(dbObj, backupObj)
        return backupObj
    }

    /**
     * Overwrite this method if you have lazy exceptions while Hibernate-Search re-indexes. See e. g. AuftragDao.
     */
    open fun prepareHibernateSearch(obj: O, operationType: OperationType) {
    }

    /**
     * Object will be marked as deleted (boolean flag), therefore undelete is always possible without any loss of data.
     */
    @Throws(AccessException::class)
    @JvmOverloads
    open fun markAsDeleted(obj: O, checkAccess: Boolean = true) {
        baseDOPersistenceService.markAsDeleted(this, obj, checkAccess = checkAccess, logMessage = logDatabaseActions)
    }

    /**
     * Historizable objects will be deleted (including all history entries). This option is used to fullfill the
     * privacy protection rules.
     */
    @Throws(AccessException::class)
    @JvmOverloads
    fun forceDelete(obj: O, checkAccess: Boolean = true) {
        baseDOPersistenceService.delete(this, obj, checkAccess = checkAccess, force = true)
    }

    /**
     * Object will be deleted finally out of the data base.
     */
    @Throws(AccessException::class)
    @JvmOverloads
    fun delete(obj: O, checkAccess: Boolean = true) {
        baseDOPersistenceService.delete(this, obj, checkAccess = checkAccess)
    }

    /**
     * Object will be marked as deleted (booelan flag), therefore undelete is always possible without any loss of data.
     */
    @Throws(AccessException::class)
    @JvmOverloads
    open fun undelete(obj: O, checkAccess: Boolean = true) {
        baseDOPersistenceService.undelete(this, obj, checkAccess = checkAccess)
    }

    /**
     * Checks the basic select access right. Overload this method if your class supports this right.
     */
    @Throws(AccessException::class)
    fun checkLoggedInUserSelectAccess() {
        if (!hasUserSelectAccess(requiredLoggedInUser, true)) {
            // Should not occur!
            log.error { "Development error: Subclass should throw an exception instead of returning false." }
            throw UserException(UserException.I18N_KEY_PLEASE_CONTACT_DEVELOPER_TEAM)
        }
    }

    @Throws(AccessException::class)
    fun checkLoggedInUserSelectAccess(obj: O) {
        if (!hasUserSelectAccess(requiredLoggedInUser, obj, true)) {
            // Should not occur!
            log.error("Development error: Subclass should throw an exception instead of returning false.")
            throw UserException(UserException.I18N_KEY_PLEASE_CONTACT_DEVELOPER_TEAM)
        }
    }

    @Throws(AccessException::class)
    private fun checkLoggedInUserHistoryAccess(obj: O) {
        if (!hasHistoryAccess(requiredLoggedInUser, true)
            || !hasLoggedInUserHistoryAccess(obj, true)
        ) {
            // Should not occur!
            log.error("Development error: Subclass should throw an exception instead of returning false.")
            throw UserException(UserException.I18N_KEY_PLEASE_CONTACT_DEVELOPER_TEAM)
        }
    }

    @Throws(AccessException::class)
    fun checkLoggedInUserInsertAccess(obj: O) {
        checkInsertAccess(requiredLoggedInUser, obj)
    }

    @Throws(AccessException::class)
    protected open fun checkInsertAccess(user: PFUserDO, obj: O) {
        if (!hasInsertAccess(user, obj, true)) {
            // Should not occur!
            log.error("Development error: Subclass should throw an exception instead of returning false.")
            throw UserException(UserException.I18N_KEY_PLEASE_CONTACT_DEVELOPER_TEAM)
        }
    }

    /**
     * @param dbObj The original object (stored in the database)
     */
    @Throws(AccessException::class)
    fun checkLoggedInUserUpdateAccess(obj: O, dbObj: O) {
        checkUpdateAccess(requiredLoggedInUser, obj, dbObj)
    }

    /**
     * @param dbObj The original object (stored in the database)
     */
    @Throws(AccessException::class)
    protected open fun checkUpdateAccess(user: PFUserDO, obj: O, dbObj: O) {
        if (!hasUpdateAccess(user, obj, dbObj, true)) {
            // Should not occur!
            log.error("Development error: Subclass should throw an exception instead of returning false.")
            throw UserException(UserException.I18N_KEY_PLEASE_CONTACT_DEVELOPER_TEAM)
        }
    }

    @Throws(AccessException::class)
    internal fun checkLoggedInUserDeleteAccess(obj: O, dbObj: O) {
        if (!hasLoggedInUserDeleteAccess(obj, dbObj, true)) {
            // Should not occur!
            log.error("Development error: Subclass should throw an exception instead of returning false.")
            throw UserException(UserException.I18N_KEY_PLEASE_CONTACT_DEVELOPER_TEAM)
        }
    }

    /**
     * Checks the basic select access right. Overwrite this method if the basic select access should be checked.
     *
     * @return true at default or if readWriteUserRightId is given hasReadAccess(boolean).
     * @see .hasUserSelectAccess
     */
    fun hasLoggedInUserSelectAccess(throwException: Boolean): Boolean {
        return hasUserSelectAccess(requiredLoggedInUser, throwException)
    }

    /**
     * Checks the basic select access right. Overwrite this method if the basic select access should be checked.
     *
     * @return true at default or if readWriteUserRightId is given hasReadAccess(boolean).
     * @see .hasAccess
     */
    open fun hasUserSelectAccess(user: PFUserDO, throwException: Boolean): Boolean {
        return hasAccess(user, null, null, OperationType.SELECT, throwException)
    }

    /**
     * If userRightId is given then [AccessChecker.hasAccess]
     * is called and returned. If not given a UnsupportedOperationException is thrown. Checks the user's access to the
     * given object.
     *
     * @param obj           The object.
     * @param oldObj        The old version of the object (is only given for operationType [OperationType.UPDATE]).
     * @param operationType The operation type (select, insert, update or delete)
     * @return true, if the user has the access right for the given operation type and object.
     */
    fun hasLoggedInUserAccess(
        obj: O?, oldObj: O?, operationType: OperationType,
        throwException: Boolean
    ): Boolean {
        return hasAccess(requiredLoggedInUser, obj, oldObj, operationType, throwException)
    }

    /**
     * If userRightId is given then [AccessChecker.hasAccess]
     * is called and returned. If not given a UnsupportedOperationException is thrown. Checks the user's access to the
     * given object.
     *
     * @param user          Check the access for the given user instead of the logged-in user.
     * @param obj           The object.
     * @param oldObj        The old version of the object (is only given for operationType [OperationType.UPDATE]).
     * @param operationType The operation type (select, insert, update or delete)
     * @return true, if the user has the access right for the given operation type and object.
     */
    open fun hasAccess(
        user: PFUserDO, obj: O?, oldObj: O?, operationType: OperationType,
        throwException: Boolean
    ): Boolean {
        if (userRightId != null) {
            return accessChecker.hasAccess(user, userRightId, obj, oldObj, operationType, throwException)
        }
        throw UnsupportedOperationException(
            "readWriteUserRightId not given. Override this method or set readWriteUserRightId in constructor."
        )
    }

    /**
     * @param obj Check access to this object.
     * @see .hasUserSelectAccess
     */
    fun hasLoggedInUserSelectAccess(obj: O, throwException: Boolean): Boolean {
        return hasUserSelectAccess(requiredLoggedInUser, obj, throwException)
    }

    /**
     * @param user Check the access for the given user instead of the logged-in user. Checks select access right by
     * calling hasAccess(obj, OperationType.SELECT).
     * @param obj  Check access to this object.
     * @see .hasAccess
     */
    open fun hasUserSelectAccess(user: PFUserDO, obj: O, throwException: Boolean): Boolean {
        return hasAccess(user, obj, null, OperationType.SELECT, throwException)
    }

    /**
     * Has the user access to the history of the given object. At default this method calls hasHistoryAccess(boolean)
     * first and then hasSelectAccess.
     */
    fun hasLoggedInUserHistoryAccess(obj: O, throwException: Boolean): Boolean {
        return hasHistoryAccess(requiredLoggedInUser, obj, throwException)
    }

    /**
     * Has the user access to the history of the given object. At default this method calls hasHistoryAccess(boolean)
     * first and then hasSelectAccess.
     */
    open fun hasHistoryAccess(user: PFUserDO, obj: O, throwException: Boolean): Boolean {
        if (!hasHistoryAccess(user, throwException)) {
            return false
        }
        if (userRightId != null) {
            return accessChecker.hasHistoryAccess(user, userRightId, obj, throwException)
        }
        return hasUserSelectAccess(user, obj, throwException)
    }

    /**
     * Has the user access to the history in general of the objects. At default this method calls hasSelectAccess.
     */
    fun hasLoggedInUserHistoryAccess(throwException: Boolean): Boolean {
        return hasHistoryAccess(requiredLoggedInUser, throwException)
    }

    /**
     * Has the user access to the history in general of the objects. At default this method calls hasSelectAccess.
     */
    open fun hasHistoryAccess(user: PFUserDO, throwException: Boolean): Boolean {
        if (userRightId != null) {
            return accessChecker.hasHistoryAccess(user, userRightId, null, throwException)
        }
        return hasUserSelectAccess(user, throwException)
    }

    /**
     * Checks insert access right by calling hasAccess(obj, OperationType.INSERT).
     *
     * @param obj Check access to this object.
     * @see .hasInsertAccess
     */
    open fun hasLoggedInUserInsertAccess(obj: O?, throwException: Boolean): Boolean {
        return hasInsertAccess(requiredLoggedInUser, obj, throwException)
    }

    /**
     * Checks insert access right by calling hasAccess(obj, OperationType.INSERT).
     *
     * @param obj Check access to this object.
     * @see .hasAccess
     */
    open fun hasInsertAccess(user: PFUserDO, obj: O?, throwException: Boolean): Boolean {
        return hasAccess(user, obj, null, OperationType.INSERT, throwException)
    }

    /**
     * Checks write access of the readWriteUserRight. If not given, true is returned at default. This method should only
     * be used for checking the insert access to show an insert button or not. Before inserting any object the write
     * access is checked by has*Access(...) independent of the result of this method.
     *
     * @see org.projectforge.framework.persistence.api.IDao.hasInsertAccess
     */
    open fun hasLoggedInUserInsertAccess(): Boolean {
        return hasInsertAccess(requiredLoggedInUser)
    }

    /**
     * Checks write access of the readWriteUserRight. If not given, true is returned at default. This method should only
     * be used for checking the insert access to show an insert button or not. Before inserting any object the write
     * access is checked by has*Access(...) independent of the result of this method.
     *
     * @see AccessChecker.hasInsertAccess
     */
    override fun hasInsertAccess(user: PFUserDO): Boolean {
        if (userRightId != null) {
            return accessChecker.hasInsertAccess(user, userRightId, false)
        }
        return true
    }

    /**
     * Checks update access right by calling hasAccess(obj, OperationType.UPDATE).
     *
     * @param dbObj The original object (stored in the database)
     * @param obj   Check access to this object.
     * @see .hasUpdateAccess
     */
    open fun hasLoggedInUserUpdateAccess(obj: O, dbObj: O, throwException: Boolean): Boolean {
        return hasUpdateAccess(requiredLoggedInUser, obj, dbObj, throwException)
    }

    /**
     * Checks update access right by calling hasAccess(obj, OperationType.UPDATE).
     *
     * @param dbObj The original object (stored in the database)
     * @param obj   Check access to this object.
     * @see .hasAccess
     */
    open fun hasUpdateAccess(user: PFUserDO, obj: O, dbObj: O?, throwException: Boolean): Boolean {
        return hasAccess(user, obj, dbObj, OperationType.UPDATE, throwException)
    }

    /**
     * Checks delete access right by calling hasAccess(obj, OperationType.DELETE).
     *
     * @param obj   Check access to this object.
     * @param dbObj current version of this object in the data base.
     * @see .hasDeleteAccess
     */
    open fun hasLoggedInUserDeleteAccess(obj: O, dbObj: O, throwException: Boolean): Boolean {
        return hasDeleteAccess(loggedInUser!!, obj, dbObj, throwException)
    }

    /**
     * Checks delete access right by calling hasAccess(obj, OperationType.DELETE).
     *
     * @param obj   Check access to this object.
     * @param dbObj current version of this object in the data base.
     * @see .hasAccess
     */
    open fun hasDeleteAccess(user: PFUserDO, obj: O, dbObj: O?, throwException: Boolean): Boolean {
        return hasAccess(user, obj, dbObj, OperationType.DELETE, throwException)
    }

    val userRight: UserRight?
        get() = if (userRightId != null) {
            userRights.getRight(userRightId)
        } else {
            null
        }

    /**
     * Overload this method for copying field manually. Used for modifiing fields inside methods: update, markAsDeleted
     * and undelete.
     *
     * @return true, if any field was modified, otherwise false.
     * @see BaseDO.copyValuesFrom
     */
    open fun copyValues(src: O, dest: O, vararg ignoreFields: String): EntityCopyStatus? {
        return dest.copyValuesFrom(src, *ignoreFields)
    }

    /**
     * SECURITY ADVICE:
     * For security reasons every property must be enabled for autocompletion. Otherwise the user may select
     * too much information, because only generic select access of an entity is checked. Example: The user has
     * select access to users, therefore he may select all password fields!!!
     * <br></br>
     * Refer implementation of ContractDao as example.
     */
    open fun isAutocompletionPropertyEnabled(property: String?): Boolean {
        return false
    }

    /**
     * SECURITY ADVICE:
     * Only generic check access will be done. The matching entries will not be checked!
     *
     * @param property     Property of the data base entity.
     * @param searchString String the user has typed in.
     * @return All matching entries (like search) for the given property modified or updated in the last 2 years.
     */
    open fun getAutocompletion(property: String, searchString: String): List<String> {
        checkLoggedInUserSelectAccess()
        if (!isAutocompletionPropertyEnabled(property)) {
            log.warn("Security alert: The user tried to select property '" + property + "' of entity '" + doClass.name + "'.")
            return ArrayList()
        }
        if (StringUtils.isBlank(searchString)) {
            return ArrayList()
        }
        return persistenceService.runReadOnly { context ->
            val em = context.em
            val cb = em.criteriaBuilder
            val cr = cb.createQuery(String::class.java)
            val root = cr.from(doClass)
            val yearsAgo = now().minusYears(2).utilDate
            cr.select(root.get(property)).where(
                cb.equal(root.get<Any>("deleted"), false),
                cb.greaterThan(root.get("lastUpdate"), yearsAgo),
                cb.like(cb.lower(root.get(property)), "%" + StringUtils.lowerCase(searchString) + "%")
            )
                .orderBy(cb.asc(root.get<Any>(property)))
                .distinct(true)
            em.createQuery(cr).resultList
        }
    }

    /**
     * Re-indexes the entries of the last day, 1,000 at max.
     *
     * @see DatabaseDao.createReindexSettings
     */
    open fun rebuildDatabaseIndex4NewestEntries() {
        val settings = createReindexSettings(true)
        databaseDao.rebuildDatabaseSearchIndices(doClass, settings)
        databaseDao.rebuildDatabaseSearchIndices(HistoryEntryDO::class.java, settings)
    }

    /**
     * Re-indexes all entries (full re-index).
     */
    open fun rebuildDatabaseIndex() {
        val settings = createReindexSettings(false)
        databaseDao.rebuildDatabaseSearchIndices(doClass, settings)
    }

    /**
     * Re-index all dependent objects manually (hibernate search). Hibernate doesn't re-index these objects, does it?
     */
    open fun reindexDependentObjects(obj: O) {
        hibernateSearchDependentObjectsReindexer.reindexDependents(obj)
    }

    protected open val additionalHistorySearchDOs: Array<Class<*>>? = null

    /**
     * @return Wether the data object (BaseDO) this dao is responsible for is from type Historizable or not.
     */
    override fun isHistorizable(): Boolean {
        return HistoryBaseDaoAdapter.isHistorizable(doClass)
    }

    open fun getEntityClass(): Class<O> {
        @Suppress("UNCHECKED_CAST")
        return MGCClassUtils.getGenericTypeArgument(javaClass, 0) as Class<O>
    }

    /**
     * Register given listener.
     * The listener is called every time an object was selected, inserted, updated or deleted.
     *
     * @param objectChangedListener
     */
    fun register(objectChangedListener: BaseDOModifiedListener<O>) {
        baseDOChangedRegistry.register(objectChangedListener)
    }

    companion object {
        const val EXCEPTION_HISTORIZABLE_NOTDELETABLE: String =
            "Could not delete of historizable objects (contact your software developer): "

        /**
         * Maximum allowed mass updates within one massUpdate call.
         */
        const val MAX_MASS_UPDATE: Int = 100
        const val MAX_MASS_UPDATE_EXCEEDED_EXCEPTION_I18N: String =
            "massUpdate.error.maximumNumberOfAllowedMassUpdatesExceeded"
    }
}
