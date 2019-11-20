/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.commons.lang3.Validate
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.i18n.InternalErrorException
import org.projectforge.framework.persistence.history.HistoryBaseDaoAdapter
import org.projectforge.framework.persistence.jpa.impl.BaseDaoJpaAdapter
import org.slf4j.LoggerFactory

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object BaseDaoSupport {
    private val log = LoggerFactory.getLogger(BaseDaoSupport::class.java)

    @JvmStatic
    fun <O : ExtendedBaseDO<Int>> internalSave(baseDao: BaseDao<O>, obj: O): Int? {
        Validate.notNull<O>(obj)
        //TODO: Muss der richtige Tenant gesetzt werden. Ist nur Workaround.
        if (obj.getTenant() == null) {
            obj.setTenant(baseDao.getDefaultTenant())
        }
        obj.setCreated()
        obj.setLastUpdate()
        baseDao.onSave(obj)
        baseDao.onSaveOrModify(obj)
        BaseDaoJpaAdapter.prepareInsert(obj)
        baseDao.emgrFactory.runInTrans { emgr ->
            val em = emgr.entityManager
            em.persist(obj)
            if (baseDao.logDatabaseActions) {
                log.info("New " + baseDao.clazz.getSimpleName() + " added (" + obj.getId() + "): " + obj.toString())
            }
            baseDao.prepareHibernateSearch(obj, OperationType.INSERT)
            em.merge(obj)
            em.flush()
            baseDao.flushSearchSession(em)
            null
        }
        HistoryBaseDaoAdapter.inserted(obj)
        baseDao.afterSaveOrModify(obj)
        baseDao.afterSave(obj)

        return obj.getId()
    }

    @JvmStatic
    fun <O : ExtendedBaseDO<Int>> internalUpdate(baseDao: BaseDao<O>, obj: O, checkAccess: Boolean): ModificationStatus? {
        baseDao.beforeSaveOrModify(obj)
        baseDao.tenantChecker.isTenantSet(obj, true)
        baseDao.onSaveOrModify(obj)
        if (checkAccess) {
            baseDao.accessChecker.checkRestrictedOrDemoUser()
        }
        class ResultObject(var dbObjBackup: O? = null, var wantsReindexAllDependentObjects: Boolean = false, var modStatus: ModificationStatus? = null)

        val res = ResultObject()
        baseDao.emgrFactory.runInTrans { emgr ->
            val em = emgr.entityManager
            val dbObj = em.find(baseDao.clazz, obj.id)
            if (checkAccess) {
                baseDao.checkPartOfCurrentTenant(obj, OperationType.UPDATE)
                baseDao.checkLoggedInUserUpdateAccess(obj, dbObj)
            }
            baseDao.onChange(obj, dbObj)
            if (baseDao.supportAfterUpdate) {
                res.dbObjBackup = baseDao.getBackupObject(dbObj)
            } else {
                res.dbObjBackup = null
            }
            res.wantsReindexAllDependentObjects = baseDao.wantsReindexAllDependentObjects(obj, dbObj)
            res.modStatus = HistoryBaseDaoAdapter.wrappHistoryUpdate(dbObj) {
                val result = baseDao.copyValues(obj, dbObj)
                if (result != ModificationStatus.NONE) {
                    BaseDaoJpaAdapter.prepareUpdate(dbObj)
                    dbObj.setLastUpdate()
                    // } else {
                    //   log.info("No modifications detected (no update needed): " + dbObj.toString());
                    baseDao.prepareHibernateSearch(obj, OperationType.UPDATE)
                    em.merge(dbObj)
                    em.flush()
                    if (baseDao.logDatabaseActions) {
                        log.info(baseDao.clazz.getSimpleName() + " updated: " + dbObj.toString())
                    }
                    baseDao.flushSearchSession(em)
                }
                result
            }
            null
        }
        baseDao.afterSaveOrModify(obj)
        if (baseDao.supportAfterUpdate) {
            baseDao.afterUpdate(obj, res.dbObjBackup, res.modStatus !== ModificationStatus.NONE)
            baseDao.afterUpdate(obj, res.dbObjBackup)
        } else {
            baseDao.afterUpdate(obj, null, res.modStatus !== ModificationStatus.NONE)
            baseDao.afterUpdate(obj, null)
        }
        if (res.wantsReindexAllDependentObjects) {
            baseDao.reindexDependentObjects(obj)
        }
        return res.modStatus
    }

    @JvmStatic
    fun <O : ExtendedBaseDO<Int>> internalMarkAsDeleted(baseDao: BaseDao<O>, obj: O) {
        if (!HistoryBaseDaoAdapter.isHistorizable(obj)) {
            log.error(
                    "Object is not historizable. Therefore marking as deleted is not supported. Please use delete instead.")
            throw InternalErrorException("exception.internalError")
        } else {
            baseDao.onDelete(obj)
            baseDao.emgrFactory.runInTrans { emgr ->
                val em = emgr.entityManager
                val dbObj = em.find(baseDao.clazz, obj.id)
                baseDao.onSaveOrModify(obj)

                HistoryBaseDaoAdapter.wrappHistoryUpdate(dbObj) {
                    BaseDaoJpaAdapter.beforeUpdateCopyMarkDelete(dbObj, obj)
                    baseDao.copyValues(obj, dbObj) // If user has made additional changes.
                    dbObj.setDeleted(true)
                    dbObj.setLastUpdate()
                    obj.isDeleted = true                     // For callee having same object.
                    obj.setLastUpdate(dbObj.getLastUpdate()) // For callee having same object.
                    em.merge(dbObj)
                    em.flush()
                    baseDao.flushSearchSession(em)
                    null
                }
                if (baseDao.logDatabaseActions) {
                    log.info(baseDao.clazz.getSimpleName() + " marked as deleted: " + dbObj.toString())
                }
            }
            baseDao.afterSaveOrModify(obj)
            baseDao.afterDelete(obj)
        }
    }

    @JvmStatic
    fun <O : ExtendedBaseDO<Int>> internalUndelete(baseDao: BaseDao<O>, obj: O) {
        baseDao.onSaveOrModify(obj)
        val dbObj: O = baseDao.emgrFactory.runInTrans { emgr ->
            val em = emgr.entityManager
            val dbObj = em.find(baseDao.clazz, obj.id)
            HistoryBaseDaoAdapter.wrappHistoryUpdate(dbObj) {
                BaseDaoJpaAdapter.beforeUpdateCopyMarkUnDelete(dbObj, obj)
                baseDao.copyValues(obj, dbObj) // If user has made additional changes.
                dbObj.isDeleted = false
                dbObj.setLastUpdate()
                obj.isDeleted =  false                   // For callee having same object.
                obj.setLastUpdate(dbObj.getLastUpdate()) // For callee having same object.
                em.merge(dbObj)
                em.flush()
                baseDao.flushSearchSession(em)
                null
            }
            dbObj
        }

        baseDao.afterSaveOrModify(obj)
        baseDao.afterUndelete(obj)
        if (baseDao.logDatabaseActions) {
            log.info(baseDao.clazz.getSimpleName() + " undeleted: " + dbObj.toString())
        }
    }
}
