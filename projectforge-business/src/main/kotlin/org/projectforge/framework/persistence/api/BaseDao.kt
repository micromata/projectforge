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

import jakarta.persistence.EntityManager
import mu.KotlinLogging
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.collections4.PredicateUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.Validate
import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.user.UserRight
import org.projectforge.common.i18n.UserException
import org.projectforge.common.mgc.ClassUtils
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseDaoSupport.internalForceDelete
import org.projectforge.framework.persistence.api.BaseDaoSupport.internalMarkAsDeleted
import org.projectforge.framework.persistence.api.BaseDaoSupport.internalSave
import org.projectforge.framework.persistence.api.BaseDaoSupport.internalSaveOrUpdate
import org.projectforge.framework.persistence.api.BaseDaoSupport.internalUndelete
import org.projectforge.framework.persistence.api.BaseDaoSupport.internalUpdate
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.persistence.api.impl.DBQuery
import org.projectforge.framework.persistence.api.impl.HibernateSearchMeta.getClassInfo
import org.projectforge.framework.persistence.database.DatabaseDao
import org.projectforge.framework.persistence.database.DatabaseDao.Companion.createReindexSettings
import org.projectforge.framework.persistence.history.*
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.requiredLoggedInUser
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.user
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDateTime.Companion.now
import org.springframework.beans.factory.annotation.Autowired
import java.io.Serializable
import java.util.*
import java.util.stream.Collectors

private val log = KotlinLogging.logger {}

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
abstract class BaseDao<O : ExtendedBaseDO<Int>>
/**
 * The setting of the DO class is required.
 */
protected constructor(open var doClass: Class<O>) : IDao<O> {
    protected open val objectChangedListeners = mutableListOf<BaseDOChangedListener<O>>()

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

    open val additionalSearchFields: Array<String>?
        /**
         * Overwrite this method for adding search fields manually (e. g. for embedded objects). For example see TimesheetDao.
         */
        get() = null

    open val defaultSortProperties: Array<SortProperty>?
        /**
         * Overwrite this method for having a standard sort of result lists (supported by [BaseDao.getList].
         */
        get() = null

    abstract fun newInstance(): O

    /**
     * getOrLoad checks first weather the id is valid or not. Default implementation: id != 0 && id &gt; 0. Overload this,
     * if the id of the DO can be 0 for example.
     */
    protected open fun isIdValid(id: Int?): Boolean {
        return (id != null && id > 0)
    }

    /**
     * If the user has select access then the object will be returned. If not, the hibernate proxy object will be get via
     * getSession().load();
     */
    fun getOrLoad(id: Int?): O? {
        if (!isIdValid(id)) {
            return null
        }
        val obj = internalGetById(id)
        if (obj == null) {
            log.error("Can't load object of type " + doClass.name + ". Object with given id #" + id + " not found.")
            return null
        }
        if (hasLoggedInUserSelectAccess(obj, false)) {
            return obj
        }
        return persistenceService.runReadOnly { context ->
            context.em.getReference(doClass, id)
        }
    }

    fun internalLoadAllNotDeleted(): List<O> {
        return internalLoadAll().stream().filter { o: O -> !o.deleted }.collect(Collectors.toList())
    }

    open fun internalLoadAll(): List<O> {
        return persistenceService.runReadOnly { context ->
            val em = context.em
            val cb = em.criteriaBuilder
            val cq = cb.createQuery(doClass)
            val query = cq.select(cq.from(doClass))
            em.createQuery(query).resultList
        }
    }

    fun internalLoad(idList: Collection<Serializable>?): List<O>? {
        if (idList == null) {
            return null
        }
        return persistenceService.runReadOnly { context ->
            val em = context.em
            val cr = em.criteriaBuilder.createQuery(doClass)
            val root = cr.from(doClass)
            cr.select(root).where(root.get<Any>(idProperty).`in`(idList)).distinct(true)
            em.createQuery(cr).resultList
        }
    }

    @JvmField
    protected var idProperty: String = "id"

    fun getListByIds(idList: Collection<Serializable>?): List<O>? {
        if (idList == null) {
            return null
        }
        val list = internalLoad(idList)
        return extractEntriesWithSelectAccess(list!!)
    }

    /**
     * This method is used by the searchDao and calls [.getList] by default.
     *
     * @return A list of found entries or empty list. PLEASE NOTE: Returns null only if any error occured.
     * @see .getList
     */
    open fun getListForSearchDao(filter: BaseSearchFilter): List<O>? {
        return getList(filter)
    }

    /**
     * Builds query filter by simply calling constructor of QueryFilter with given search filter and calls
     * getList(QueryFilter). Override this method for building more complex query filters.
     *
     * @return A list of found entries or empty list. PLEASE NOTE: Returns null only if any error occured.
     */
    override fun getList(filter: BaseSearchFilter): List<O> {
        val queryFilter = createQueryFilter(filter)
        return getList(queryFilter)
    }

    open fun createQueryFilter(filter: BaseSearchFilter?): QueryFilter {
        return QueryFilter(filter)
    }

    /**
     * Gets the list filtered by the given filter.
     */
    @Throws(
        AccessException::class
    )
    open fun getList(filter: QueryFilter): List<O> {
        return getList(filter, null)
    }

    /**
     * Gets the list filtered by the given filter.
     */
    @Throws(
        AccessException::class
    )
    open fun getList(filter: QueryFilter, customResultFilters: List<CustomResultFilter<O>>?): List<O> {
        return dbQuery.getList<O>(this, filter, customResultFilters, true)
    }

    /**
     * Gets the list filtered by the given filter.
     */
    @Throws(
        AccessException::class
    )
    open fun internalGetList(filter: QueryFilter): List<O> {
        return dbQuery.getList<O>(this, filter, null, false)
    }

    /**
     * idSet.contains(entry.getId()) at default.
     */
    open fun contains(idSet: Set<Int?>?, entry: O): Boolean {
        if (idSet == null) {
            return false
        }
        return idSet.contains(entry.id)
    }

    /**
     * idSet.contains(entry.getId()) at default.
     */
    fun containsLong(idSet: Set<Long>?, entry: O): Boolean {
        if (idSet.isNullOrEmpty()) {
            return false
        }
        return idSet.contains(entry.id!!.toLong())
    }

    protected fun selectUnique(list: List<O>?): List<O> {
        val result = CollectionUtils.select(list, PredicateUtils.uniquePredicate()) as List<O>
        return result
    }

    fun extractEntriesWithSelectAccess(origList: List<O>): List<O> {
        val result: MutableList<O> = ArrayList()
        for (obj in origList) {
            if (hasSelectAccess(obj, requiredLoggedInUser)) {
                result.add(obj)
                afterLoad(obj)
            }
        }
        return result
    }

    /**
     * @param obj          The object to check.
     * @param loggedInUser The currend logged in user.
     * @return true if loggedInUser has select access.
     * @see .hasUserSelectAccess
     */
    fun hasSelectAccess(obj: O, loggedInUser: PFUserDO): Boolean {
        return hasUserSelectAccess(loggedInUser, obj, false)
    }

    /**
     * Overwrite this method for own list sorting. This method returns only the given list.
     */
    open fun sorted(list: List<O>): List<O> {
        return list
    }

    /**
     * @param id primary key of the base object.
     */
    @Throws(AccessException::class)
    open fun getById(id: Serializable?): O? {
        id ?: return null
        accessChecker.checkRestrictedUser()
        checkLoggedInUserSelectAccess()
        val obj = internalGetById(id) ?: return null
        checkLoggedInUserSelectAccess(obj)
        return obj
    }

    open fun internalGetById(id: Serializable?): O? {
        if (id == null) {
            return null
        }
        val obj = persistenceService.selectSingleResult(
            "select t from ${doClass.name} t where t.id = :id",
            doClass,
            Pair("id", id),
        ) ?: return null
        afterLoad(obj)
        return obj
    }

    /**
     * Gets the history entries of the object.
     */
    fun getHistoryEntries(obj: O): Array<HistoryEntry<*>> {
        accessChecker.checkRestrictedUser()
        checkLoggedInUserHistoryAccess(obj)
        return internalGetHistoryEntries(obj)
    }

    fun internalGetHistoryEntries(obj: BaseDO<*>): Array<HistoryEntry<*>> {
        accessChecker.checkRestrictedUser()
        // return HistoryBaseDaoAdapter.getHistoryFor(obj);
        return emptyArray()
    }

    /**
     * Gets the history entries of the object in flat format.<br></br>
     * Please note: If user has no access an empty list will be returned.
     */
    open fun getDisplayHistoryEntries(obj: O): MutableList<DisplayHistoryEntry> {
        if (obj.id == null || !hasLoggedInUserHistoryAccess(obj, false)) {
            return mutableListOf()
        }
        return internalGetDisplayHistoryEntries(obj)
    }

    protected fun internalGetDisplayHistoryEntries(obj: BaseDO<*>): MutableList<DisplayHistoryEntry> {
        accessChecker.checkRestrictedUser()
        val entries = internalGetHistoryEntries(obj)
        return persistenceService.runReadOnly { context -> convertAll(entries, context.em) }
    }

    private fun convertAll(entries: Array<HistoryEntry<*>>, em: EntityManager): MutableList<DisplayHistoryEntry> {
        val list: MutableList<DisplayHistoryEntry> = ArrayList()
        for (entry in entries) {
            val l = convert(entry, em)
            list.addAll(l)
        }
        return list
    }

    open fun convert(entry: HistoryEntry<*>, em: EntityManager): List<DisplayHistoryEntry> {
        if (entry.diffEntries.isNullOrEmpty()) {
            val se = DisplayHistoryEntry(userGroupCache, entry)
            return listOf(se)
        }
        val result: MutableList<DisplayHistoryEntry> = ArrayList()
        for (prop in entry.diffEntries!!) {
            val se = DisplayHistoryEntry(userGroupCache, entry, prop, em)
            result.add(se)
        }

        return result
    }

    /**
     * @return the generated identifier, if save method is used, otherwise null.
     */
    @Throws(
        AccessException::class
    )
    open fun saveOrUpdate(obj: O): Serializable? {
        var id: Serializable? = null
        if (obj.id != null && obj.created != null) { // obj.created is needed for KundeDO (id isn't null for inserting new customers).
            update(obj)
        } else {
            id = save(obj)
        }
        return id
    }

    /**
     * @return the generated identifier, if save method is used, otherwise null.
     */
    open fun internalSaveOrUpdate(obj: O): Serializable? {
        var id: Serializable? = null
        if (obj.id != null) {
            internalUpdate(obj)
        } else {
            id = internalSave(obj)
        }
        return id
    }

    /**
     * Call save(O) for every object in the given list.
     */
    @Throws(
        AccessException::class
    )
    open fun save(objects: List<O>) {
        for (obj in objects) {
            save(obj)
        }
    }

    /**
     * @return the generated identifier.
     */
    @Throws(
        AccessException::class
    )
    open fun save(obj: O): Int {
        //long begin = System.currentTimeMillis();
        if (!avoidNullIdCheckBeforeSave) {
            Validate.isTrue(obj.id == null)
        }
        accessChecker.checkRestrictedOrDemoUser()
        beforeSaveOrModify(obj)
        checkLoggedInUserInsertAccess(obj)
        val result = internalSave(obj)
        //long end = System.currentTimeMillis();
        //log.info("BaseDao.save took: " + (end - begin) + " ms.");
        return result!!
    }

    @Throws(
        AccessException::class
    )
    open fun insert(obj: O): Int {
        return save(obj)
    }

    /**
     * This method will be called after loading an object from the data base. Does nothing at default. This method is not
     * called by internalLoadAll.
     */
    open fun afterLoad(obj: O) {
    }

    /**
     * This method will be called after inserting, updating, deleting or marking the data object as deleted. This method
     * is for example needed for expiring the UserGroupCache after inserting or updating a user or group data object. Does
     * nothing at default.
     */
    open fun afterSaveOrModify(obj: O) {
    }

    /**
     * This method will be called after inserting. Does nothing at default.
     *
     * @param obj The inserted object
     */
    open fun afterSave(obj: O) {
        callObjectChangedListeners(obj, OperationType.INSERT)
    }

    /**
     * This method will be called before inserting. Does nothing at default.
     */
    open fun onSave(obj: O) {
    }

    /**
     * This method will be called before inserting, updating, deleting or marking the data object as deleted. Does nothing
     * at default.
     */
    open fun onSaveOrModify(obj: O) {
    }

    /**
     * This method will be called before access check of inserting and updating the object. Does nothing
     * at default.
     */
    open fun beforeSaveOrModify(obj: O) {
    }

    /**
     * This method will be called after updating. Does nothing at default. PLEASE NOTE: If you overload this method don't
     * forget to set [.supportAfterUpdate] to true, otherwise you won't get the origin data base object!
     *
     * @param obj   The modified object
     * @param dbObj The object from data base before modification.
     */
    open fun afterUpdate(obj: O, dbObj: O?) {
        callObjectChangedListeners(obj, OperationType.UPDATE)
    }

    /**
     * This method will be called after updating. Does nothing at default. PLEASE NOTE: If you overload this method don't
     * forget to set [.supportAfterUpdate] to true, otherwise you won't get the origin data base object!
     *
     * @param obj        The modified object
     * @param dbObj      The object from data base before modification.
     * @param isModified is true if the object was changed, false if the object wasn't modified.
     */
    open fun afterUpdate(obj: O, dbObj: O?, isModified: Boolean) {
        callObjectChangedListeners(obj, OperationType.UPDATE)
    }

    /**
     * This method will be called before updating the data object. Will also called if in internalUpdate no modification
     * was detected. Please note: Do not modify the object oldVersion! Does nothing at default.
     *
     * @param obj   The changed object.
     * @param dbObj The current database version of this object.
     */
    open fun onChange(obj: O, dbObj: O) {
    }

    /**
     * This method will be called before deleting. Does nothing at default.
     *
     * @param obj The deleted object.
     */
    open fun onDelete(obj: O) {
    }

    /**
     * This method will be called after deleting as well as after object is marked as deleted. Does nothing at default.
     *
     * @param obj The deleted object.
     */
    open fun afterDelete(obj: O) {
        callObjectChangedListeners(obj, OperationType.DELETE)
    }

    /**
     * This method will be called after undeleting. Does nothing at default.
     *
     * @param obj The deleted object.
     */
    open fun afterUndelete(obj: O) {
        callObjectChangedListeners(obj, OperationType.UNDELETE)
    }

    fun callObjectChangedListeners(obj: O, operationType: OperationType) {
        for (objectChangedListener in objectChangedListeners) {
            objectChangedListener.afterSaveOrModify(obj, operationType)
        }
    }

    /**
     * This method is for internal use e. g. for updating objects without check access.
     *
     * @return the generated identifier.
     */
    open fun internalSave(obj: O): Int? {
        return internalSave(this, obj)
    }

    open fun saveOrUpdate(col: Collection<O>) {
        for (obj in col) {
            saveOrUpdate(obj)
        }
    }

    open fun saveOrUpdate(col: Collection<O>, blockSize: Int) {
        val list: MutableList<O> = ArrayList()
        var counter = 0
        for (obj in col) {
            list.add(obj)
            if (++counter >= blockSize) {
                counter = 0
                saveOrUpdate(list)
                list.clear()
            }
        }
        saveOrUpdate(list)
    }

    /**
     * Bulk update.
     *
     * @param col Entries to save or update without check access.
     */
    open fun internalSaveOrUpdate(col: Collection<O>) {
        internalSaveOrUpdate(this, col)
    }

    /**
     * Bulk update.
     *
     * @param col       Entries to save or update without check access.
     * @param blockSize The block size of commit blocks.
     */
    open fun internalSaveOrUpdate(col: Collection<O>, blockSize: Int) {
        internalSaveOrUpdate(this, col, blockSize)
    }

    /**
     * @return true, if modifications were done, false if no modification detected.
     * @see .internalUpdate
     */
    @Throws(
        AccessException::class
    )
    open fun update(obj: O): EntityCopyStatus {
        if (obj.id == null) {
            val msg = "Could not update object unless id is not given:$obj"
            log.error(msg)
            throw RuntimeException(msg)
        }
        accessChecker.checkRestrictedOrDemoUser()
        return internalUpdate(obj, true)!!
    }

    /**
     * Thin wrapper for generic usage.
     *
     * @return true, if modifications were done, false if no modification detected.
     * @see .internalUpdate
     */
    @Throws(
        AccessException::class
    )
    open fun updateAny(obj: Any): EntityCopyStatus {
        @Suppress("UNCHECKED_CAST")
        return update(obj as O)
    }

    /**
     * Thin wrapper for generic usage.
     *
     * @return true, if modifications were done, false if no modification detected.
     * @see .internalUpdate
     */
    @Throws(
        AccessException::class
    )
    open fun internalUpdateAny(obj: Any): EntityCopyStatus? {
        @Suppress("UNCHECKED_CAST")
        return internalUpdate(obj as O)
    }

    /**
     * This method is for internal use e. g. for updating objects without check access.
     *
     * @return true, if modifications were done, false if no modification detected.
     * @see .internalUpdate
     */
    open fun internalUpdate(obj: O): EntityCopyStatus? {
        return internalUpdate(obj, false)
    }

    /**
     * This method is for internal use e. g. for updating objects without check access.<br></br>
     * Please note: update ignores the field deleted. Use markAsDeleted, delete and undelete methods instead.
     *
     * @param checkAccess If false, any access check will be ignored.
     * @return true, if modifications were done, false if no modification detected.
     */
    open fun internalUpdate(obj: O, checkAccess: Boolean): EntityCopyStatus? {
        return internalUpdate(this, obj, checkAccess)
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
    @Throws(
        AccessException::class
    )
    open fun markAsDeleted(obj: O) {
        if (obj.id == null) {
            val msg = "Could not delete object unless id is not given:$obj"
            log.error(msg)
            throw RuntimeException(msg)
        }
        accessChecker.checkRestrictedOrDemoUser()
        val dbObj = persistenceService.selectById(doClass, obj.id)!!
        checkLoggedInUserDeleteAccess(obj, dbObj)
        internalMarkAsDeleted(obj)
    }

    open fun internalMarkAsDeleted(obj: O) {
        internalMarkAsDeleted(this, obj)
    }

    /**
     * Historizable objects will be deleted (including all history entries). This option is used to fullfill the
     * privacy protection rules.
     */
    @Throws(
        AccessException::class
    )
    open fun forceDelete(obj: O) {
        if (obj.id == null) {
            val msg = "Could not delete object unless id is not given:$obj"
            log.error(msg)
            throw RuntimeException(msg)
        }
        accessChecker.checkRestrictedOrDemoUser()
        val dbObj = persistenceService.selectById(doClass, obj.id)
        if (dbObj == null) {
            log.error("Oups, can't delete $doClass #${obj.id}, because database object doesn't exist.")
            return
        }
        checkLoggedInUserDeleteAccess(obj, dbObj)
        internalForceDelete(obj)
    }

    open fun internalForceDelete(obj: O) {
        internalForceDelete(this, obj)
    }

    /**
     * Object will be deleted finally out of the data base.
     */
    @Throws(
        AccessException::class
    )
    open fun delete(obj: O) {
        accessChecker.checkRestrictedOrDemoUser()
        internalDelete(obj)
    }

    /**
     * Object will be deleted finally out of the data base.
     */
    @Throws(
        AccessException::class
    )
    open fun internalDelete(obj: O) {
        if (HistoryBaseDaoAdapter.isHistorizable(obj)) {
            val msg = EXCEPTION_HISTORIZABLE_NOTDELETABLE + obj.toString()
            log.error(msg)
            throw RuntimeException(msg)
        }
        if (obj.id == null) {
            val msg = "Could not destroy object unless id is not given: $obj"
            log.error(msg)
            throw RuntimeException(msg)
        }
        onDelete(obj)

        persistenceService.runInTransaction { context ->
            val dbObj = context.selectById(doClass, obj.id, attached = true)
            if (dbObj != null) {
                checkLoggedInUserDeleteAccess(obj, dbObj)
                context.em.remove(dbObj)
                if (logDatabaseActions) {
                    log.info(doClass.simpleName + " deleted: " + obj.toString())
                }
            } else {
                log.error("Oups, can't delete $doClass #${obj.id}, not found in database!")
            }
        }
        afterSaveOrModify(obj)
        afterDelete(obj)
    }

    /**
     * Object will be marked as deleted (booelan flag), therefore undelete is always possible without any loss of data.
     */
    @Throws(
        AccessException::class
    )
    open fun undelete(obj: O) {
        if (obj.id == null) {
            val msg = "Could not undelete object unless id is not given:$obj"
            log.error(msg)
            throw RuntimeException(msg)
        }
        accessChecker.checkRestrictedOrDemoUser()
        checkLoggedInUserInsertAccess(obj)
        internalUndelete(obj)
    }

    open fun internalUndelete(obj: O) {
        internalUndelete(this, obj)
    }

    /**
     * Checks the basic select access right. Overload this method if your class supports this right.
     */
    @Throws(AccessException::class)
    fun checkLoggedInUserSelectAccess() {
        if (!hasUserSelectAccess(requiredLoggedInUser, true)) {
            // Should not occur!
            log.error("Development error: Subclass should throw an exception instead of returning false.")
            throw UserException(UserException.I18N_KEY_PLEASE_CONTACT_DEVELOPER_TEAM)
        }
    }

    @Throws(AccessException::class)
    protected fun checkLoggedInUserSelectAccess(obj: O) {
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
    private fun checkLoggedInUserInsertAccess(obj: O) {
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
    private fun checkLoggedInUserDeleteAccess(obj: O, dbObj: O) {
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
        return hasDeleteAccess(user!!, obj, dbObj, throwException)
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

    protected open fun createHistoryEntry(
        entity: Any?, id: Number?, property: String?,
        valueClass: Class<*>?,
        oldValue: Any?, newValue: Any?
    ) {
        accessChecker.checkRestrictedOrDemoUser()
        val contextUser = user
        val userPk = contextUser?.id?.toString()
        if (userPk == null) {
            log.warn("No user found for creating history entry.")
        }
        log.error("******** Not yet implemented: createHistoryEntry")
        // HistoryBaseDaoAdapter.createHistoryEntry(entity, id, userPk, property, valueClass, oldValue, newValue);
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
        databaseDao.rebuildDatabaseSearchIndices(PfHistoryMasterDO::class.java, settings)
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
        return ClassUtils.getGenericTypeArgument(javaClass, 0) as Class<O>
    }

    @Throws(AccessException::class)
    open fun selectByPkDetached(pk: Int): O? {
        // TODO RK not detached here
        return getById(pk)
    }

    /**
     * Register given listener. The listener is called every time an object was inserted, updated or deleted.
     *
     * @param objectChangedListener
     */
    fun register(objectChangedListener: BaseDOChangedListener<O>) {
        log.info(javaClass.simpleName + ": Registering " + objectChangedListener.javaClass.name)
        objectChangedListeners.add(objectChangedListener)
    }

    companion object {
        const val EXCEPTION_HISTORIZABLE_NOTDELETABLE: String =
            "Could not delete of Historizable objects (contact your software developer): "

        /**
         * Maximum allowed mass updates within one massUpdate call.
         */
        const val MAX_MASS_UPDATE: Int = 100
        const val MAX_MASS_UPDATE_EXCEEDED_EXCEPTION_I18N: String =
            "massUpdate.error.maximumNumberOfAllowedMassUpdatesExceeded"
    }
}
