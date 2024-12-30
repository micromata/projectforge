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

package org.projectforge.plugins.datatransfer

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed
import org.projectforge.Constants
import org.projectforge.framework.i18n.TimeAgo
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.jcr.AttachmentsEventType
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.util.*
import jakarta.persistence.*
import org.projectforge.framework.json.IdOnlySerializer

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "t_plugin_datatransfer_audit")
@NamedQueries(
  NamedQuery(
    name = DataTransferAuditDO.DELETE_OLD_ENTRIES,
    query = "delete from DataTransferAuditDO where timestamp<=:timestamp"
  ),
  NamedQuery(
    name = DataTransferAuditDO.FIND_DOWNLOADS_BY_AREA_ID,
    query = "from DataTransferAuditDO where areaId=:areaId and eventType in :eventTypes order by timestamp desc"
  ),
  NamedQuery(
    name = DataTransferAuditDO.FIND_BY_AREA_ID,
    query = "from DataTransferAuditDO where areaId=:areaId order by timestamp desc"
  ),
  NamedQuery(
    name = DataTransferAuditDO.FIND_BY_ID,
    query = "from DataTransferAuditDO where id=:id"
  ),
  NamedQuery(
    name = DataTransferAuditDO.FIND_QUEUED_ENTRIES_SENT_BY_AREA_ID_IGNORE_TYPES,
    query = "from DataTransferAuditDO where areaId=:areaId and notified=false and eventType not in :eventTypes order by timestamp desc"
  ),
  NamedQuery(
    name = DataTransferAuditDO.UPDATE_NOTIFICATION_STATUS,
    query = "update DataTransferAuditDO set notified=true where id IN :idList"
  ),
)
open class DataTransferAuditDO {
  enum class Action { DELETE, DOWNLOAD, MODIFIED, UPLOAD }

  @get:Id
  @get:GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
  @get:Column(name = "pk")
  open var id: Long? = null

  @get:Column
  open var timestamp: Date? = null

  /**
   * No constraint (area might be deleted in the mean time).
   */
  @get:Column(name = "area_fk", nullable = false)
  open var areaId: Long? = null

  @get:ManyToOne(fetch = FetchType.LAZY)
  @get:JoinColumn(name = "by_user_fk")
  @JsonSerialize(using = IdOnlySerializer::class)
  open var byUser: PFUserDO? = null

  @get:Column(name = "by_external_user", length = 4000)
  open var byExternalUser: String? = null


  @get:Enumerated(EnumType.STRING)
  @get:Column(name = "event_type", length = 20, nullable = false)
  open var eventType: AttachmentsEventType? = null

  @get:Column(length = Constants.LENGTH_TEXT)
  open var description: String? = null

  /**
   * Old value of description (if modified).
   */
  @get:Column(name = "description_old", length = Constants.LENGTH_TEXT)
  open var descriptionOld: String? = null

  /**
   * The user uploaded the file. It's stored here, because the file could be deleted in the mean time to inform
   * this user about the deletion and any other modifications and downloads.
   */
  @get:ManyToOne(fetch = FetchType.LAZY)
  @get:JoinColumn(name = "upload_by_user_fk")
  @JsonSerialize(using = IdOnlySerializer::class)
  open var uploadByUser: PFUserDO? = null

  @get:Column(length = 1000)
  open var filename: String? = null

  /**
   * Old value of description (if modified).
   */
  @get:Column(name = "filename_old", length = 1000)
  open var filenameOld: String? = null

  /**
   * Notification to all observers sent? Is also set to true after processing, if no observer is registered.
   */
  @get:Column(name = "notified")
  open var notified: Boolean = false

  /**
   * Time stamp of event as human-readable time ago message.
   */
  @get:Transient
  @get:JsonProperty(access = JsonProperty.Access.READ_ONLY)
  open val timeAgo
    get() = TimeAgo.getMessage(timestamp)

  @get:Transient
  @get:JsonProperty(access = JsonProperty.Access.READ_ONLY)
  open val eventAsString
    get() = eventType?.i18nKey?.let { translate(it) } ?: ""

  @get:Transient
  @get:JsonProperty
  open var byUserAsString: String? = null
    get() {
      if (field == null) {
        createdByUserAsString()
      }
      return field
    }

  fun createdByUserAsString(locale: Locale? = null) {
    byUserAsString = DataTransferAreaDao.getTranslatedUserString(byUser, byExternalUser, locale)
  }

  @get:Transient
  @get:JsonProperty(access = JsonProperty.Access.READ_ONLY)
  open val filenameAsString
    get() = filename ?: ""

  @get:Transient
  @get:JsonProperty(access = JsonProperty.Access.READ_ONLY)
  open val descriptionAsString
    get() = description ?: ""

  override fun toString(): String {
    return "DataTransferAuditDO(id=$id, timestamp=$timestamp, areaId=$areaId, byUser=${byUser?.id}, byExternalUser=$byExternalUser, eventType=$eventType, description=$description, descriptionOld=$descriptionOld, uploadByUser=${uploadByUser?.id}, filename=$filename, filenameOld=$filenameOld, notified=$notified)"
  }

  companion object {
    internal const val DELETE_OLD_ENTRIES = "DataTransferAuditDO_DeleteOldEntries"
    internal const val FIND_BY_AREA_ID = "DataTransferAuditDO_FindByAreaId"
    internal const val FIND_BY_ID = "DataTransferAuditDO_FindById"
    internal const val FIND_DOWNLOADS_BY_AREA_ID = "DataTransferAuditDO_FindDownloadsByAreaId"
    internal const val FIND_QUEUED_ENTRIES_SENT_BY_AREA_ID_IGNORE_TYPES = "DataTransferAuditDO_FindQueuedEntriesByAreaId"
    internal const val UPDATE_NOTIFICATION_STATUS = "DataTransferAuditDO_UpdateNotificationStatus"
  }
}
