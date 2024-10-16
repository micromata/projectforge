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
import org.hibernate.search.mapper.orm.Search
import org.projectforge.framework.ToStringUtil
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.i18n.InternalErrorException
import org.projectforge.framework.persistence.candh.CandHMaster
import org.projectforge.framework.persistence.history.DisplayHistoryEntry
import org.projectforge.framework.persistence.history.EntityOpType
import org.projectforge.framework.persistence.history.HistoryBaseDaoAdapter
import org.projectforge.framework.persistence.history.HistoryService
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


private val log = KotlinLogging.logger {}

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
class BaseDOPersistenceService {
    class ResultObject<O : ExtendedBaseDO<Long>>(
        var dbObjBackup: O? = null,
        var wantsReindexAllDependentObjects: Boolean = false,
        var modStatus: EntityCopyStatus? = null
    )

    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var historyService: HistoryService

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    internal fun <O : ExtendedBaseDO<Long>> insert(baseDao: BaseDao<O>, obj: O): Long? {
        insert(obj, baseDao, logMessage = baseDao.logDatabaseActions)
        return obj.id
    }

    fun <O : ExtendedBaseDO<Long>> insert(obj: O): Long? {
        insert(obj, logMessage = true)
        return obj.id
    }

    private fun <O : ExtendedBaseDO<Long>> insert(
        obj: O,
        baseDao: BaseDao<O>? = null,
        clazz: Class<O>? = null,
        logMessage: Boolean = true,
    ) {
        persistenceService.runInTransaction { context ->
            baseDao?.changedRegistry?.onInsert(obj)
            baseDao?.changedRegistry?.onInsertOrModify(obj, OperationType.INSERT)
            val em = context.em
            obj.setCreated()
            obj.setLastUpdate()
            val useClass = clazz ?: baseDao?.doClass!!
            em.persist(obj)
            if (logMessage) {
                log.info { "New ${useClass.simpleName} added (${obj.id}): $obj" }
            }
            baseDao?.prepareHibernateSearch(obj, OperationType.INSERT)
            em.merge(obj)
            HistoryBaseDaoAdapter.inserted(obj, context)
            log.info { "${useClass.simpleName} updated: $obj" }
            try {
                em.flush()
            } catch (ex: Exception) {
                // Exception stack trace:
                // org.postgresql.util.PSQLException: FEHLER: ungültige Byte-Sequenz für Kodierung »UTF8«: 0x00
                log.error(ex) { "${ex.message} while saving object: ${ToStringUtil.toJsonString(obj)}" }
                throw ex
            }
            em.detach(obj)
            baseDao?.changedRegistry?.afterInsert(obj)
        }
        baseDao?.changedRegistry?.afterInsertOrModify(obj, operationType = OperationType.INSERT)
    }

    internal fun <O : ExtendedBaseDO<Long>> update(
        baseDao: BaseDao<O>,
        obj: O,
        checkAccess: Boolean,
        dbObj: O? = null,
    ): EntityCopyStatus {
        val res = ResultObject<O>()
        update(
            obj = obj,
            res = res,
            baseDao = baseDao,
            checkAccess = checkAccess,
            dbObj = dbObj,
            logMessage = baseDao.logDatabaseActions,
        )
        return res.modStatus!!
    }

    fun <O : ExtendedBaseDO<Long>> update(
        clazz: Class<O>? = null,
        obj: O,
        res: ResultObject<O>,
    ) {
        update(obj, res, clazz = clazz)
    }

    private fun <O : ExtendedBaseDO<Long>> update(
        obj: O,
        res: ResultObject<O>,
        baseDao: BaseDao<O>? = null,
        checkAccess: Boolean = true,
        dbObj: O? = null,
        clazz: Class<O>? = null,
        logMessage: Boolean = true,
    ) {
        if (obj.id == null) {
            val msg = "Could not update object unless id is not given:$obj"
            log.error(msg)
            throw RuntimeException(msg)
        }
        baseDao?.changedRegistry?.beforeInsertOrModify(obj, OperationType.UPDATE)
        persistenceService.runInTransaction { context ->
            val em = context.em
            val useClass = clazz ?: baseDao?.doClass!!
            val useDbObj = dbObj ?: requireDbObj(em, useClass, obj.id, "update")
            if (checkAccess) {
                accessChecker.checkRestrictedOrDemoUser()
                baseDao?.checkLoggedInUserUpdateAccess(obj, useDbObj)
            }
            baseDao?.changedRegistry?.onInsertOrModify(obj, OperationType.UPDATE)
            baseDao?.changedRegistry?.onUpdate(obj, useDbObj)
            if (baseDao?.supportAfterUpdate == true) {
                res.dbObjBackup = baseDao.getBackupObject(useDbObj)
            } else {
                res.dbObjBackup = null
            }
            res.wantsReindexAllDependentObjects = baseDao?.wantsReindexAllDependentObjects(obj, useDbObj) == true
            val candHContext = CandHMaster.copyValues(src = obj, dest = useDbObj, entityOpType = EntityOpType.Update)
            val modStatus = candHContext.currentCopyStatus
            res.modStatus = modStatus
            if (modStatus != EntityCopyStatus.NONE) {
                useDbObj.setLastUpdate()
                baseDao?.prepareHibernateSearch(obj, OperationType.UPDATE)
                val merged = em.merge(useDbObj)
                try {
                    em.flush()
                } catch (ex: Exception) {
                    // Exception stack trace:
                    // org.postgresql.util.PSQLException: FEHLER: ungültige Byte-Sequenz für Kodierung »UTF8«: 0x00
                    log.error(ex) { "${ex.message} while updating object: ${ToStringUtil.toJsonString(obj)}" }
                    throw ex
                }
                candHContext.preparedHistoryEntries(merged, useDbObj)
                HistoryBaseDaoAdapter.updated(merged, candHContext.historyEntries, context)
                em.flush()
                if (logMessage) {
                    log.info { "${useClass.simpleName} updated: $merged" }
                }
                flushSearchSession(em)
            }
            if (baseDao?.supportAfterUpdate == true) {
                baseDao.changedRegistry.afterUpdate(
                    obj,
                    res.dbObjBackup,
                    isModified = res.modStatus != EntityCopyStatus.NONE
                )
            } else {
                baseDao?.changedRegistry?.afterUpdate(obj, null, res.modStatus != EntityCopyStatus.NONE)
            }
            if (res.wantsReindexAllDependentObjects) {
                baseDao?.reindexDependentObjects(obj)
            }
        }
        baseDao?.changedRegistry?.afterInsertOrModify(obj, OperationType.UPDATE)
    }

    internal fun <O : ExtendedBaseDO<Long>> markAsDeleted(baseDao: BaseDao<O>, obj: O, checkAccess: Boolean) {
        if (obj.id == null) {
            val msg = "Could not mark object as deleted unless id is not given:$obj"
            log.error { msg }
            throw RuntimeException(msg)
        }
        if (!HistoryBaseDaoAdapter.isHistorizable(obj)) {
            log.error {
                "Object is not historizable. Therefore, marking as deleted is not supported. Please use delete instead."
            }
            throw InternalErrorException("exception.internalError")
        }
        baseDao.changedRegistry.beforeInsertOrModify(obj, OperationType.DELETE)
        persistenceService.runInTransaction { context ->
            baseDao.changedRegistry.onDelete(obj)
            baseDao.changedRegistry.onInsertOrModify(obj, OperationType.DELETE)
            val em = context.em
            val dbObj = requireDbObj(em, baseDao.doClass, obj.id, "markAsDeleted")
            if (checkAccess) {
                accessChecker.checkRestrictedOrDemoUser()
                baseDao.checkLoggedInUserDeleteAccess(obj, dbObj)
            }
            val candHContext = CandHMaster.copyValues(
                src = obj,
                dest = dbObj,
                entityOpType = EntityOpType.Delete
            ) // If user has made additional changes.
            dbObj.deleted = true
            dbObj.setLastUpdate()
            obj.deleted = true                     // For callee having same object.
            obj.lastUpdate = dbObj.lastUpdate // For callee having same object.
            val merged = em.merge(dbObj) //
            em.flush()
            candHContext.preparedHistoryEntries(merged, dbObj)
            HistoryBaseDaoAdapter.updated(dbObj, candHContext.historyEntries, context)
            baseDao.changedRegistry.afterDelete(obj)
            baseDao.changedRegistry.afterInsertOrModify(obj, OperationType.DELETE)
            if (baseDao.logDatabaseActions) {
                log.info { "${baseDao.doClass.simpleName} marked as deleted: $dbObj" }
            }
        }
    }

    internal fun <O : ExtendedBaseDO<Long>> undelete(baseDao: BaseDao<O>, obj: O, checkAccess: Boolean) {
        if (obj.id == null) {
            val msg = "Could not undelete object unless id is not given:$obj"
            log.error(msg)
            throw RuntimeException(msg)
        }
        baseDao.changedRegistry.beforeInsertOrModify(obj, OperationType.UNDELETE)
        persistenceService.runInTransaction { context ->
            baseDao.changedRegistry.onUndelete(obj)
            baseDao.changedRegistry.onInsertOrModify(obj, OperationType.UNDELETE)
            val em = context.em
            val dbObj = requireDbObj(em, baseDao.doClass, obj.id, "undelete")
            if (checkAccess) {
                accessChecker.checkRestrictedOrDemoUser()
                baseDao.checkLoggedInUserInsertAccess(obj)
            }
            val candHContext = CandHMaster.copyValues(
                src = obj,
                dest = dbObj,
                entityOpType = EntityOpType.Undelete
            ) // If user has made additional changes.
            dbObj.deleted = false
            dbObj.setLastUpdate()
            obj.deleted = false                   // For callee having same object.
            obj.lastUpdate = dbObj.lastUpdate // For callee having same object.
            val merged = em.merge(dbObj)
            em.flush()
            candHContext.preparedHistoryEntries(merged, dbObj)
            HistoryBaseDaoAdapter.updated(dbObj, candHContext.historyEntries, context)
            baseDao.changedRegistry.afterUndelete(obj)
            baseDao.changedRegistry.afterInsertOrModify(obj, OperationType.UNDELETE)
            if (baseDao.logDatabaseActions) {
                log.info { "${baseDao.doClass.simpleName} undeleted: $dbObj" }
            }
        }
    }

    /**
     * @param force If true, the object will be deleted without any checks. This is needed to force deleting objects that are historizable.
     */
    internal fun <O : ExtendedBaseDO<Long>> delete(
        baseDao: BaseDao<O>,
        obj: O,
        checkAccess: Boolean,
        force: Boolean = false
    ) {
        val id = obj.id
        if (id == null) {
            val msg = "Could not delete object unless id is not given:$obj"
            log.error(msg)
            throw RuntimeException(msg)
        }
        if (HistoryBaseDaoAdapter.isHistorizable(obj)) {
            if (!baseDao.isForceDeletionSupport) {
                val msg =
                    "${BaseDao.EXCEPTION_HISTORIZABLE_NOTDELETABLE} Force deletion not supported by '${baseDao.doClass.name}'. Use markAsDeleted instead for: $obj"
                log.error { msg }
                throw RuntimeException(msg)
            }
            if (!force) {
                val msg =
                    "${BaseDao.EXCEPTION_HISTORIZABLE_NOTDELETABLE} Therefore use markAsDeleted instead (or use force=true)."
                log.error { msg }
                throw RuntimeException(msg)
            }
        }
        baseDao.changedRegistry.beforeInsertOrModify(obj, OperationType.DELETE)
        persistenceService.runInTransaction { context ->
            baseDao.changedRegistry.onDelete(obj)
            baseDao.changedRegistry.onInsertOrModify(obj, OperationType.DELETE)
            val em = context.em
            val dbObj = requireDbObj(em, baseDao.doClass, obj.id, "delete (force=$force)")
            if (checkAccess) {
                accessChecker.checkRestrictedOrDemoUser()
                baseDao.checkLoggedInUserDeleteAccess(obj, dbObj)
            }
            em.remove(dbObj)
            em.flush()
            if (HistoryBaseDaoAdapter.isHistorizable(obj)) {
                // Remove all history entries (including all attributes) from the database:
                historyService.loadHistory(obj).forEach { historyEntry ->
                    em.remove(historyEntry)
                    val displayHistoryEntry = ToStringUtil.toJsonString(DisplayHistoryEntry(historyEntry))
                    log.info { "${baseDao.doClass.simpleName}:$id (forced) deletion of history entry: $displayHistoryEntry" }
                }
            }
            if (baseDao.logDatabaseActions) {
                log.info { "${baseDao.doClass.simpleName} (forced) deleted: $dbObj" }
            }
            em.detach(dbObj)
        }
        baseDao.changedRegistry.afterDelete(obj)
        baseDao.changedRegistry.afterInsertOrModify(obj, OperationType.DELETE)
    }

    /**
     * Throws exception, if object not found.
     * @param entityClass The class of the object.
     * @param id The id of the object.
     * @param caller The caller of this method. Used for logging (in exception message).
     * @return The object.
     */
    @Throws(IllegalArgumentException::class)
    private fun <O> requireDbObj(em: EntityManager, entityClass: Class<O>, id: Long?, caller: String): O {
        id
            ?: throw IllegalArgumentException("Can't load object ${entityClass.simpleName} with id=null, caller=$caller.")
        val dbOj = em.find(entityClass, id)
            ?: throw IllegalArgumentException("Object ${entityClass.simpleName} not found by id $id, caller=$caller.")
        return dbOj
    }

    private fun flushSearchSession(em: EntityManager?) {
        if (LUCENE_FLUSH_ALWAYS) {
            val searchSession = Search.session(em)
            // Flushing the index changes asynchonously
            searchSession.indexingPlan().execute()
        }
    }

    companion object {
        private const val LUCENE_FLUSH_ALWAYS = false

        @JvmStatic
        @JvmOverloads
        fun returnFalseOrThrowException(
            throwException: Boolean,
            user: PFUserDO? = null,
            operationType: OperationType? = null,
            msg: String = "access.exception.noAccess",
        ): Boolean {
            if (throwException) {
                val ex = AccessException(user, msg, operationType)
                ex.operationType = operationType
                throw ex
            }
            return false
        }
    }
}
