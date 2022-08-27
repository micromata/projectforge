/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.datatransfer

import mu.KotlinLogging
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.jcr.AttachmentsEventType
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDateTime
import org.projectforge.jcr.FileInfo
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.temporal.ChronoUnit
import java.util.*
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

private val log = KotlinLogging.logger {}

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Repository
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
open class DataTransferAuditDao {
  @PersistenceContext
  private lateinit var em: EntityManager

  internal open lateinit var dataTransferAreaDao: DataTransferAreaDao

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  internal open fun insert(audit: DataTransferAuditDO) {
    if (audit.timestamp == null) {
      // Should only be preset for test cases.
      audit.timestamp = Date()
    }
    audit.notified = false
    em.persist(audit)
    em.flush()
  }

  /**
   * Set the notificationsSent=true for the given auditEntries.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  internal open fun removeFromQueue(auditEntries: Collection<DataTransferAuditDO>) {
    auditEntries.chunked(50).forEach { subList ->
      em.createNamedQuery(DataTransferAuditDO.UPDATE_NOTIFICATION_STATUS)
        .setParameter("idList", subList.map { it.id })
        .executeUpdate()
    }
  }

  open fun getEntriesByAreaId(areaId: Int?): List<DataTransferAuditDO>? {
    areaId ?: return null
    val area = dataTransferAreaDao.getById(areaId)
    val loggedInUser = ThreadLocalUserContext.user
    requireNotNull(loggedInUser)
    if (!dataTransferAreaDao.hasAccess(loggedInUser, area,null, OperationType.SELECT, false)) {
      // User has no select access to given area.
      return emptyList()
    }
    if (area.isPersonalBox() && area.getPersonalBoxUserId() != loggedInUser.id) {
      // It's not the logged-in user's personal box. No permission on audit entries.
      return emptyList()
    }
    return internalGetEntriesByAreaId(areaId)
  }

  internal open fun internalGetEntriesByAreaId(areaId: Int?): List<DataTransferAuditDO>? {
    areaId ?: return null
    return em.createNamedQuery(DataTransferAuditDO.FIND_BY_AREA_ID, DataTransferAuditDO::class.java)
      .setParameter("areaId", areaId)
      .resultList
  }

  /**
   * @return list of unprocessed audit entries, if exists. If any audit entry (not DOWNLOAD{_ALL}) exists newer than
   * 10 minutes, null is returned.
   */
  internal open fun internalGetQueuedEntriesByAreaId(areaId: Int?): List<DataTransferAuditDO>? {
    areaId ?: return null
    val resultList =
      em.createNamedQuery(DataTransferAuditDO.FIND_QUEUED_ENTRIES_SENT_BY_AREA_ID, DataTransferAuditDO::class.java)
        .setParameter("areaId", areaId)
        .setParameter("eventTypes", downloadEventTypes)
        .resultList
    val tenMinutesAgo = PFDateTime.now().minus(10, ChronoUnit.MINUTES).utilDate
    if (!resultList.isNullOrEmpty()) {
      if (resultList.any {
          val timestamp = it.timestamp
          timestamp != null && timestamp > tenMinutesAgo
        }) {
        // Data transfer area has modifications newer than 10 minutes, wait for other actions before notification.
        return null
      }
    }
    return resultList
  }

  /**
   * @return list of all download events (download, download multi or download all).
   */
  internal open fun internalGetDownloadEntriesByAreaId(areaId: Int?): List<DataTransferAuditDO> {
    areaId ?: return emptyList()
    return em.createNamedQuery(DataTransferAuditDO.FIND_DOWNLOADS_BY_AREA_ID, DataTransferAuditDO::class.java)
      .setParameter("areaId", areaId)
      .setParameter("eventTypes", downloadEventTypes)
      .resultList
  }

  @Transactional(propagation = Propagation.REQUIRED)
  internal open fun deleteOldEntries(beforeDate: PFDateTime): Int {
    val deletedAuditEntries = em.createNamedQuery(DataTransferAuditDO.DELETE_OLD_ENTRIES)
      .setParameter("timestamp", beforeDate.utilDate)
      .executeUpdate()
    if (deletedAuditEntries > 0) {
      log.info { "$deletedAuditEntries outdated audit entries deleted (before ${beforeDate.isoStringSeconds})." }
    }
    return deletedAuditEntries
  }

  @Transactional(propagation = Propagation.REQUIRED)
  open fun insertAudit(
    eventType: AttachmentsEventType,
    dbObj: DataTransferAreaDO,
    byUser: PFUserDO?,
    byExternalUser: String?,
    file: FileInfo? = null,
    timestamp4TestCase: PFDateTime? = null,
  ) {
    val audit = DataTransferAuditDO()
    audit.areaId = dbObj.id
    audit.eventType = eventType
    audit.byUser = byUser
    audit.byExternalUser = byExternalUser
    audit.filename = file?.fileName
    audit.description = file?.description
    timestamp4TestCase?.let {
      audit.timestamp = it.utilDate
    }
    insert(audit)
  }

  companion object {
    /**
     * Notifications will not be sent on download event types.
     */
    val downloadEventTypes =
      listOf(AttachmentsEventType.DOWNLOAD, AttachmentsEventType.DOWNLOAD_MULTI, AttachmentsEventType.DOWNLOAD_ALL)
  }
}
