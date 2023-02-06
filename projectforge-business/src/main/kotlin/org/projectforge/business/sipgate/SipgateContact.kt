/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.sipgate

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import org.projectforge.framework.ToStringUtil.Companion.toJsonString

/**
 * @author K. Reinhard (k.reinhard@micromata.de)
 */
class SipgateContact {
  enum class Scope { PRIVATE, SHARED, INTERNAL }

  enum class EmailType { HOME, WORK, OTHER }

  var id: String? = null
  var name: String? = null
  var family: String? = null
  var given: String? = null
  var picture: String? = null
  var emails: MutableList<SipgateEmail>? = null
  var numbers: MutableList<SipgateNumber>? = null
  var addresses: MutableList<SipgateAddress>? = null

  var organizationArray: Array<Array<String>>?
    @JsonProperty("organization")
    get() {
      if (organization == null && division == null) {
        return null
      }
      val list = mutableListOf(organization ?: "")
      division?.let { list.add(it) }
      return arrayOf(list.toTypedArray())
    }
    set(value) {
      organization = value?.firstOrNull()?.firstOrNull()
      division = value?.firstOrNull()?.getOrNull(1)
    }

  @JsonIgnore
  var organization: String? = null

  @JsonIgnore
  var division: String? = null

  var scope: Scope? = null

  var work: String?
    @JsonIgnore
    get() = numbers?.firstOrNull { it.isWork() }?.number
    set(value) {
      setNumber(value, SipgateNumber.WORK_ARRAY)
    }

  var home: String?
    @JsonIgnore
    get() = numbers?.firstOrNull { it.isHome() }?.number
    set(value) {
      setNumber(value, SipgateNumber.HOME_ARRAY)
    }

  var faxWork: String?
    @JsonIgnore
    get() = numbers?.firstOrNull { it.isFaxWork() }?.number
    set(value) {
      setNumber(value, SipgateNumber.FAX_WORK_ARRAY)
    }

  var faxHome: String?
    @JsonIgnore
    get() = numbers?.firstOrNull { it.isFaxHome() }?.number
    set(value) {
      setNumber(value, SipgateNumber.FAX_HOME_ARRAY)
    }

  var cell: String?
    @JsonIgnore
    get() = numbers?.firstOrNull { it.isCell() }?.number
    set(value) {
      setNumber(value, SipgateNumber.CELL_ARRAY)
    }

  var pager: String?
    @JsonIgnore
    get() = numbers?.firstOrNull { it.isPager() }?.number
    set(value) {
      setNumber(value, SipgateNumber.PAGER_ARRAY)
    }

  /**
   * Linked to private cell number
   */
  var other: String?
    @JsonIgnore
    get() = numbers?.firstOrNull { it.isOther() }?.number
    set(value) {
      setNumber(value, SipgateNumber.OTHER_ARRAY)
    }

  var email: String?
    @JsonIgnore
    get() = emails?.firstOrNull { it.type == EmailType.WORK }?.email
    set(value) {
      setEmail(value, EmailType.WORK)
    }

  var privateEmail: String?
    @JsonIgnore
    get() = emails?.firstOrNull { it.type == EmailType.HOME }?.email
    set(value) {
      setEmail(value, EmailType.HOME)
    }

  private fun setEmail(emailAddress: String?, type: EmailType) {
    if (emails == null) {
      emails = mutableListOf()
    }
    var mail = emails?.firstOrNull { it.type == type }
    if (mail == null) {
      mail = SipgateEmail(type = type)
      emails?.add(mail)
    }
    mail.email = emailAddress
  }

  private fun setNumber(number: String?, type: Array<String>) {
    if (numbers == null) {
      numbers = mutableListOf()
    }
    var num = numbers?.firstOrNull { SipgateNumber.compare(it.type, type) }
    if (num == null) {
      num = SipgateNumber()
      num.type = type
      numbers?.add(num)
    }
    num.number = number
  }

  /**
   * as json.
   */
  override fun toString(): String {
    return toJsonString(this)
  }
}

class SipgateAddress {
  var poBox: String? = null
  var extendedAddress: String? = null
  var streetAddress: String? = null
  var region: String? = null
  var locality: String? = null
  var postalCode: String? = null
  var country: String? = null
}

class SipgateEmail(
  var email: String? = null,
  @JsonIgnore
  var type: SipgateContact.EmailType? = null,
) {
  @get:JsonProperty("type")
  var typeArray: Array<String>?
    set(value) {
      type = when (value?.firstOrNull()) {
        "home" -> SipgateContact.EmailType.HOME
        "work" -> SipgateContact.EmailType.WORK
        "other" -> SipgateContact.EmailType.OTHER
        else -> null
      }
    }
    get() {
      val value = type?.name?.lowercase() ?: return null
      return arrayOf(value)
    }
}

class SipgateNumber(
  var number: String? = null,
) {
  var type: Array<String>? = null

  fun setHome(): SipgateNumber {
    type = HOME_ARRAY
    return this
  }

  fun isHome(): Boolean {
    return compare(type, HOME_ARRAY)
  }

  fun setWork(): SipgateNumber {
    type = WORK_ARRAY
    return this
  }

  fun isWork(): Boolean {
    return compare(type, WORK_ARRAY)
  }

  fun setCell(): SipgateNumber {
    type = CELL_ARRAY
    return this
  }

  fun isCell(): Boolean {
    return compare(type, CELL_ARRAY)
  }

  fun setFaxHome(): SipgateNumber {
    type = FAX_HOME_ARRAY
    return this
  }

  fun isFaxHome(): Boolean {
    return compare(type, FAX_HOME_ARRAY)
  }

  fun setFaxWork(): SipgateNumber {
    type = FAX_WORK_ARRAY
    return this
  }

  fun isFaxWork(): Boolean {
    return compare(type, FAX_WORK_ARRAY)
  }

  fun setPager(): SipgateNumber {
    type = PAGER_ARRAY
    return this
  }

  fun isPager(): Boolean {
    return compare(type, PAGER_ARRAY)
  }

  fun setOther(): SipgateNumber {
    type = OTHER_ARRAY
    return this
  }

  fun isOther(): Boolean {
    return compare(type, OTHER_ARRAY)
  }

  companion object {
    internal fun compare(array1: Array<String>?, array2: Array<String>?): Boolean {
      if (array1 == null) {
        return array2 == null
      }
      array2 ?: return false
      if (array1.size != array2.size) {
        return false
      }
      array1.forEach {
        if (!array2.contains(it)) {
          return false
        }
      }
      return true
    }

    internal val HOME_ARRAY = arrayOf("home")
    internal val WORK_ARRAY = arrayOf("work")
    internal val CELL_ARRAY = arrayOf("cell")
    internal val FAX_HOME_ARRAY = arrayOf("fax", "home")
    internal val FAX_WORK_ARRAY = arrayOf("fax", "work")
    internal val PAGER_ARRAY = arrayOf("pager")
    internal val OTHER_ARRAY = arrayOf("other")
  }
}
