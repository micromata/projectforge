/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.teamcal.event.model

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.apache.commons.lang3.StringUtils
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed
import org.projectforge.business.address.AddressDO
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import jakarta.persistence.*
import org.projectforge.framework.json.IdOnlySerializer

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(
  name = "T_PLUGIN_CALENDAR_EVENT_ATTENDEE",
  indexes = [jakarta.persistence.Index(
    name = "idx_fk_t_plugin_calendar_event_attendee_address_id",
    columnList = "address_id"
  ), jakarta.persistence.Index(name = "idx_fk_t_plugin_calendar_event_attendee_user_id", columnList = "user_id")]
)
//@WithHistory(noHistoryProperties = ["loginToken"])
@NamedQueries(
  NamedQuery(
    name = TeamEventAttendeeDO.DELETE_ATTENDEE_BY_ADDRESS_ID_FROM_ALL_EVENTS,
    query = "delete from TeamEventAttendeeDO where address.id=:addressId"
  ),
)
open class TeamEventAttendeeDO : DefaultBaseDO(), Comparable<TeamEventAttendeeDO> {

  @get:Column
  open var number: Short? = null

  /**
   * The url (mail) of the attendee. Isn't used if the attendee is a ProjectForge user.
   */
  @get:Column(length = URL_MAX_LENGTH)
  open var url: String? = null

  /**
   * Is set if the attendee is a ProjectForge user.
   */
  @get:ManyToOne(fetch = FetchType.LAZY)
  @get:JoinColumn(name = "address_id")
  @JsonSerialize(using = IdOnlySerializer::class)
  open var address: AddressDO? = null

  /**
   * Is set if the attendee is a ProjectForge user.
   */
  @get:ManyToOne(fetch = FetchType.LAZY)
  @get:JoinColumn(name = "user_id")
  @JsonSerialize(using = IdOnlySerializer::class)
  open var user: PFUserDO? = null

  /**
   * Is used if the attendee isn't a ProjectForge user for authentication.
   */
  @get:Column(name = "login_token", length = 255)
  open var loginToken: String? = null

  @get:Enumerated(EnumType.STRING)
  @get:Column(length = 100)
  open var status: TeamEventAttendeeStatus? = TeamEventAttendeeStatus.NEEDS_ACTION

  @get:Column(length = 4000)
  open var comment: String? = null

  @get:Column(length = 4000, name = "comment_of_attendee")
  open var commentOfAttendee: String? = null

  @get:Column(length = 256, name = "common_name")
  open var commonName: String? = null

  @get:Column(length = 20, name = "cu_type")
  open var cuType: String? = null

  @get:Column
  open var rsvp: Boolean? = null

  @get:Column
  open var role: String? = null

  @get:Column(length = 1000, name = "additional_params")
  open var additionalParams: String? = null

  val eMailAddress: String?
    @Transient
    get() {
      if (address != null) {
        return address!!.email
      } else if (user != null) {
        return user!!.email
      }
      return null
    }

  val addressId: Long?
    @Transient
    get() = if (this.address == null) {
      null
    } else address!!.id

  val userId: Long?
    @Transient
    get() = if (this.user == null) {
      null
    } else user!!.id

  /**
   * @see java.lang.Comparable.compareTo
   */
  override fun compareTo(other: TeamEventAttendeeDO): Int {
    return if (this.id != null && this.id == other.id) {
      0
    } else this.toString().lowercase().compareTo(other.toString().lowercase())
  }

  /**
   * Equals / HashCode contract is broken because of technical requirements
   *
   * @return
   */
  override fun hashCode(): Int {
    val result = 0
    if (url != null) {
      return 31 * url!!.hashCode()
    }
    if (address != null && address!!.id != null) {
      return 31 * address!!.id.hashCode()
    }
    if (user != null && user!!.id != null) {
      return 31 * user!!.id.hashCode()
    }
    return if (id != null) {
      31 * id.hashCode()
    } else result
  }

  /**
   * Equals / HashCode contract is broken because of technical requirements
   *
   * @return
   */
  override fun equals(other: Any?): Boolean {
    if (other !is TeamEventAttendeeDO) {
      return false
    }
    val o = other as TeamEventAttendeeDO?
    if (this.url != null && o!!.url != null && StringUtils.equals(this.url, o.url)) {
      return true
    }
    if (this.addressId != null && o!!.addressId != null && this.addressId == o.addressId) {
      return true
    }
    if (this.userId != null && o!!.userId != null && this.userId == o.userId) {
      return true
    }
    if (this.id != null && o!!.id != null && this.id == o.id) {
      return true
    }
    return (this.url == null && o!!.url == null && this.addressId == null && o.addressId == null && this.userId == null && o.userId == null && this.id == null && o.id == null)
  }

  override fun toString(): String {
    if (this.user != null) {
      return this.user!!.getFullname() + " (" + this.user!!.email + ")"
    }
    if (this.address != null) {
      return this.address!!.fullName + " (" + this.address!!.email + ")"
    }
    return if (this.url != null) {
      this.url!!
    } else super.toString()
  }

  /**
   * @see java.lang.Object.clone
   */
  fun clone(): TeamEventAttendeeDO {
    val cloneAttendee = TeamEventAttendeeDO()
    cloneAttendee.address = this.address
    cloneAttendee.comment = this.comment
    cloneAttendee.commentOfAttendee = this.commentOfAttendee
    cloneAttendee.loginToken = this.loginToken
    cloneAttendee.number = this.number
    cloneAttendee.status = this.status
    cloneAttendee.url = this.url
    cloneAttendee.user = this.user
    cloneAttendee.commonName = this.commonName
    cloneAttendee.cuType = this.cuType
    cloneAttendee.rsvp = this.rsvp
    cloneAttendee.additionalParams = this.additionalParams
    cloneAttendee.role = this.role

    return cloneAttendee
  }

  companion object {
    internal const val DELETE_ATTENDEE_BY_ADDRESS_ID_FROM_ALL_EVENTS =
      "TeamEventAttendeeDO_deleteAttendeeByAddressIdFromAllEvents"

    private val NON_HISTORIZABLE_ATTRIBUTES: MutableSet<String>

    const val URL_MAX_LENGTH = 255

    init {
      NON_HISTORIZABLE_ATTRIBUTES = HashSet()
      NON_HISTORIZABLE_ATTRIBUTES.add("loginToken")
    }

    fun getUrlMaxLength(): Int {
      return URL_MAX_LENGTH
    }
  }
}
