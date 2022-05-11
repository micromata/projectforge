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
import org.projectforge.framework.jcr.AttachmentsEventType
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.jcr.FileInfo
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
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

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  open fun insert(audit: DataTransferAuditDO) {
    if (audit.timestamp == null) {
      // Should only be preset for test cases.
      audit.timestamp = Date()
    }
    em.persist(audit)
    em.flush()
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  open fun notificationsSentFor(auditEntries: Collection<DataTransferAuditDO>) {
    auditEntries.chunked(50).forEach { subList ->
      em.createNamedQuery(DataTransferAuditDO.UPDATE_NOTIFICATION_STATUS)
        .setParameter("idList", subList.map { it.id }).executeUpdate()
    }
  }

  open fun getEntriesByAreaId(areaId: Int): List<DataTransferAuditDO> {
    return em.createNamedQuery(DataTransferAuditDO.FIND_BY_AREA_ID, DataTransferAuditDO::class.java)
      .setParameter("areaId", areaId).resultList
  }

  @Transactional(propagation = Propagation.REQUIRED)
  open fun deleteOldEntries(beforeDate: Date) {
    em.createNamedQuery(DataTransferAuditDO.DELETE_OLD_ENTRIES)
      .setParameter("timestamp", beforeDate).executeUpdate()
  }

  fun insertAudit(
    eventType: AttachmentsEventType,
    dbObj: DataTransferAreaDO,
    byUser: PFUserDO?,
    byExternalUser: String?,
    file: FileInfo? = null,
  ) {
    val audit = DataTransferAuditDO()
    audit.areaId = dbObj.id
    audit.eventType = eventType
    audit.byUser = byUser
    audit.byExternalUser = byExternalUser
    audit.filename = file?.fileName
    insert(audit)
  }
}
