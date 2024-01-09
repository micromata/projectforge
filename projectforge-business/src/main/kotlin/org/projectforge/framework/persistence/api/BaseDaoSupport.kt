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

import de.micromata.genome.db.jpa.history.entities.HistoryMasterBaseDO
import mu.KotlinLogging
import org.apache.commons.lang3.Validate
import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.ToStringUtil
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.i18n.InternalErrorException
import org.projectforge.framework.persistence.history.DisplayHistoryEntry
import org.projectforge.framework.persistence.history.HistoryBaseDaoAdapter
import org.projectforge.framework.persistence.history.entities.PfHistoryMasterDO
import org.projectforge.framework.persistence.jpa.PfEmgr
import org.projectforge.framework.persistence.jpa.impl.BaseDaoJpaAdapter
import org.projectforge.framework.persistence.user.entities.PFUserDO

private val log = KotlinLogging.logger {}

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object BaseDaoSupport {
  class ResultObject<O : ExtendedBaseDO<Int>>(
    var dbObjBackup: O? = null,
    var wantsReindexAllDependentObjects: Boolean = false,
    var modStatus: ModificationStatus? = null
  )

  @JvmStatic
  fun <O : ExtendedBaseDO<Int>> internalSave(baseDao: BaseDao<O>, obj: O): Int? {
    preInternalSave(baseDao, obj)
    baseDao.emgrFactory.runInTrans { emgr ->
      internalSave(emgr, baseDao, obj)
      null
    }
    postInternalSave(baseDao, obj)
    return obj.id
  }

  private fun <O : ExtendedBaseDO<Int>> internalSave(emgr: PfEmgr, baseDao: BaseDao<O>, obj: O) {
    BaseDaoJpaAdapter.prepareInsert(emgr, obj)
    val em = emgr.entityManager
    em.persist(obj)
    if (baseDao.logDatabaseActions) {
      log.info("New ${baseDao.clazz.simpleName} added (${obj.id}): $obj")
    }
    baseDao.prepareHibernateSearch(obj, OperationType.INSERT)
    em.merge(obj)
    try {
      em.flush()
    } catch (ex: Exception) {
      // Exception stack trace:
      // org.postgresql.util.PSQLException: FEHLER: ungültige Byte-Sequenz für Kodierung »UTF8«: 0x00
      log.error("${ex.message} while saving object: ${ToStringUtil.toJsonString(obj)}", ex)
      throw ex
    }
    baseDao.flushSearchSession(em)
    HistoryBaseDaoAdapter.inserted(emgr, obj)
  }

  private fun <O : ExtendedBaseDO<Int>> preInternalSave(baseDao: BaseDao<O>, obj: O) {
    Validate.notNull<O>(obj)
    obj.setCreated()
    obj.setLastUpdate()
    baseDao.onSave(obj)
    baseDao.onSaveOrModify(obj)
  }

  private fun <O : ExtendedBaseDO<Int>> postInternalSave(baseDao: BaseDao<O>, obj: O) {
    baseDao.afterSaveOrModify(obj)
    baseDao.afterSave(obj)
  }

  @JvmStatic
  fun <O : ExtendedBaseDO<Int>> internalUpdate(baseDao: BaseDao<O>, obj: O, checkAccess: Boolean): ModificationStatus? {
    preInternalUpdate(baseDao, obj, checkAccess)
    val res = ResultObject<O>()
    baseDao.emgrFactory.runInTrans { emgr ->
      internalUpdate(emgr, baseDao, obj, checkAccess, res)
    }
    postInternalUpdate<O>(baseDao, obj, res)
    return res.modStatus
  }

  private fun <O : ExtendedBaseDO<Int>> preInternalUpdate(baseDao: BaseDao<O>, obj: O, checkAccess: Boolean) {
    baseDao.beforeSaveOrModify(obj)
    baseDao.onSaveOrModify(obj)
    if (checkAccess) {
      baseDao.accessChecker.checkRestrictedOrDemoUser()
    }
  }

  private fun <O : ExtendedBaseDO<Int>> internalUpdate(
    emgr: PfEmgr,
    baseDao: BaseDao<O>,
    obj: O,
    checkAccess: Boolean,
    res: ResultObject<O>
  ) {
    val em = emgr.entityManager
    val dbObj = em.find(baseDao.clazz, obj.id)
    if (checkAccess) {
      baseDao.checkLoggedInUserUpdateAccess(obj, dbObj)
    }
    baseDao.onChange(obj, dbObj)
    if (baseDao.supportAfterUpdate) {
      res.dbObjBackup = baseDao.getBackupObject(dbObj)
    } else {
      res.dbObjBackup = null
    }
    res.wantsReindexAllDependentObjects = baseDao.wantsReindexAllDependentObjects(obj, dbObj)
    res.modStatus = HistoryBaseDaoAdapter.wrapHistoryUpdate(emgr, dbObj) {
      val result = baseDao.copyValues(obj, dbObj)
      if (result != ModificationStatus.NONE) {
        BaseDaoJpaAdapter.prepareUpdate(emgr, dbObj)
        dbObj.setLastUpdate()
        // } else {
        //   log.info("No modifications detected (no update needed): " + dbObj.toString());
        baseDao.prepareHibernateSearch(obj, OperationType.UPDATE)
        em.merge(dbObj)
        try {
          em.flush()
        } catch (ex: Exception) {
          // Exception stack trace:
          // org.postgresql.util.PSQLException: FEHLER: ungültige Byte-Sequenz für Kodierung »UTF8«: 0x00
          log.error("${ex.message} while updating object: ${ToStringUtil.toJsonString(obj)}", ex)
          throw ex
        }
        if (baseDao.logDatabaseActions) {
          log.info("${baseDao.clazz.simpleName} updated: $dbObj")
        }
        baseDao.flushSearchSession(em)
      }
      result
    }
  }

  private fun <O : ExtendedBaseDO<Int>> postInternalUpdate(baseDao: BaseDao<O>, obj: O, res: ResultObject<O>) {
    baseDao.afterSaveOrModify(obj)
    if (baseDao.supportAfterUpdate) {
      baseDao.afterUpdate(obj, res.dbObjBackup, res.modStatus != ModificationStatus.NONE)
      baseDao.afterUpdate(obj, res.dbObjBackup)
    } else {
      baseDao.afterUpdate(obj, null, res.modStatus != ModificationStatus.NONE)
      baseDao.afterUpdate(obj, null)
    }
    if (res.wantsReindexAllDependentObjects) {
      baseDao.reindexDependentObjects(obj)
    }
  }


  @JvmStatic
  fun <O : ExtendedBaseDO<Int>> internalMarkAsDeleted(baseDao: BaseDao<O>, obj: O) {
    if (!HistoryBaseDaoAdapter.isHistorizable(obj)) {
      log.error(
        "Object is not historizable. Therefore marking as deleted is not supported. Please use delete instead."
      )
      throw InternalErrorException("exception.internalError")
    }
    baseDao.onDelete(obj)
    baseDao.emgrFactory.runInTrans { emgr ->
      val em = emgr.entityManager
      val dbObj = em.find(baseDao.clazz, obj.id)
      baseDao.onSaveOrModify(obj)

      HistoryBaseDaoAdapter.wrapHistoryUpdate(emgr, dbObj) {
        BaseDaoJpaAdapter.beforeUpdateCopyMarkDelete(dbObj, obj)
        baseDao.copyValues(obj, dbObj) // If user has made additional changes.
        dbObj.setDeleted(true)
        dbObj.setLastUpdate()
        obj.isDeleted = true                     // For callee having same object.
        obj.setLastUpdate(dbObj.getLastUpdate()) // For callee having same object.
        em.merge(dbObj) //
        em.flush()
        baseDao.flushSearchSession(em)
        null
      }
      if (baseDao.logDatabaseActions) {
        log.info("${baseDao.clazz.simpleName} marked as deleted: $dbObj")
      }
    }
    baseDao.afterSaveOrModify(obj)
    baseDao.afterDelete(obj)
  }

  @JvmStatic
  fun <O : ExtendedBaseDO<Int>> internalUndelete(baseDao: BaseDao<O>, obj: O) {
    baseDao.onSaveOrModify(obj)
    val dbObj: O = baseDao.emgrFactory.runInTrans { emgr ->
      val em = emgr.entityManager
      val dbObj = em.find(baseDao.clazz, obj.id)
      HistoryBaseDaoAdapter.wrapHistoryUpdate(emgr, dbObj) {
        BaseDaoJpaAdapter.beforeUpdateCopyMarkUnDelete(dbObj, obj)
        baseDao.copyValues(obj, dbObj) // If user has made additional changes.
        dbObj.isDeleted = false
        dbObj.setLastUpdate()
        obj.isDeleted = false                   // For callee having same object.
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
      log.info("${baseDao.clazz.simpleName} undeleted: $dbObj")
    }
  }

  @JvmStatic
  fun <O : ExtendedBaseDO<Int>> internalForceDelete(baseDao: BaseDao<O>, obj: O) {
    if (!HistoryBaseDaoAdapter.isHistorizable(obj)) {
      log.error(
        "Object is not historizable. Therefore use normal delete instead."
      )
      throw InternalErrorException("exception.internalError")
    }
    if (!baseDao.forceDeletionSupport) {
      val msg = "Force deletion not supported by '${baseDao.clazz.name}'. Use markAsDeleted instead for: $obj"
      log.error(msg)
      throw RuntimeException(msg)
    }
    val id = obj.id
    if (id == null) {
      val msg = "Could not destroy object unless id is not given: $obj"
      log.error(msg)
      throw RuntimeException(msg)
    }
    baseDao.onDelete(obj)
    val userGroupCache = UserGroupCache.getInstance()
    baseDao.emgrFactory.runInTrans { emgr ->
      val em = emgr.entityManager
      val dbObj = em.find(baseDao.clazz, id)
      em.remove(dbObj)

      val masterClass: Class<out HistoryMasterBaseDO<*, *>?> =
        PfHistoryMasterDO::class.java //HistoryServiceImpl.getHistoryMasterClass()
      emgr.selectAttached(
        masterClass,
        "select h from ${masterClass.name} h where h.entityName = :entityName and h.entityId = :entityId",
        "entityName", baseDao.clazz.name, "entityId", id.toLong()
      ).forEach { historyEntry ->
        em.remove(historyEntry)
        val displayHistoryEntry = if (historyEntry != null) {
          ToStringUtil.toJsonString(DisplayHistoryEntry(userGroupCache, historyEntry))
        } else {
          "???"
        }
        log.info(
          "${baseDao.clazz.simpleName}:$id (forced) deletion of history entry: $displayHistoryEntry"
        )
      }
      if (baseDao.logDatabaseActions) {
        log.info("${baseDao.clazz.simpleName} (forced) deleted: $dbObj")
      }
    }
    baseDao.afterDelete(obj)
  }

  /**
   * Bulk update.
   */
  @JvmStatic
  fun <O : ExtendedBaseDO<Int>> internalSaveOrUpdate(baseDao: BaseDao<O>, col: Collection<O>) {
    baseDao.emgrFactory.runInTrans { emgr ->
      for (obj in col) {
        if (obj.id != null) {
          preInternalUpdate(baseDao, obj, false)
          val res = ResultObject<O>()
          internalUpdate(emgr, baseDao, obj, false, res)
          postInternalUpdate<O>(baseDao, obj, res)
        } else {
          preInternalSave(baseDao, obj)
          internalSave(emgr, baseDao, obj)
          postInternalSave(baseDao, obj)
        }
      }
    }
  }

  /**
   * Bulk update.
   * @param col Entries to save or update without check access.
   * @param blockSize The block size of commit blocks.
   */
  @JvmStatic
  fun <O : ExtendedBaseDO<Int>> internalSaveOrUpdate(baseDao: BaseDao<O>, col: Collection<O>, blockSize: Int) {
    val list: MutableList<O> = ArrayList<O>()
    var counter = 0
    for (obj in col) {
      list.add(obj)
      if (++counter >= blockSize) {
        counter = 0
        internalSaveOrUpdate(baseDao, list)
        list.clear()
      }
    }
    internalSaveOrUpdate(baseDao, list)
  }

  @JvmStatic
  @JvmOverloads
  fun returnFalseOrThrowException(
    throwException: Boolean,
    user: PFUserDO? = null,
    operationType: OperationType? = null,
    msg: String = "access.exception.noAccess",
  ): Boolean {
    if (throwException) {
      val ex =  AccessException(user, msg, operationType)
      ex.operationType = operationType
      throw ex
    }
    return false
  }
}
