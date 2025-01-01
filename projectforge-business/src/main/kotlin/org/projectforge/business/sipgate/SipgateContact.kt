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

package org.projectforge.business.sipgate

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import org.projectforge.framework.ToStringUtil.Companion.toJsonString
import org.projectforge.framework.utils.NumberHelper

/**
 * @author K. Reinhard (k.reinhard@micromata.de)
 */
class SipgateContact {
  enum class Scope { PRIVATE, SHARED, INTERNAL }

  enum class EmailType { HOME, WORK, OTHER }

  class Address {
    var poBox: String? = null
    var extendedAddress: String? = null
    var streetAddress: String? = null
    var region: String? = null
    var locality: String? = null
    var postalCode: String? = null
    var country: String? = null
  }

  var id: String? = null
  var name: String? = null
  var family: String? = null
  var given: String? = null
  var picture: String? = null
  var emails: MutableList<Email>? = null
  var numbers: MutableList<Number>? = null
  var addresses: MutableList<Address>? = null

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
    get() = numbers?.firstOrNull { it.isWorkType() }?.number
    set(value) {
      setNumber(value, Number.WORK_ARRAY)
    }

  var home: String?
    @JsonIgnore
    get() = numbers?.firstOrNull { it.isHomeType() }?.number
    set(value) {
      setNumber(value, Number.HOME_ARRAY)
    }

  var faxWork: String?
    @JsonIgnore
    get() = numbers?.firstOrNull { it.isFaxWorkType() }?.number
    set(value) {
      setNumber(value, Number.FAX_WORK_ARRAY)
    }

  var faxHome: String?
    @JsonIgnore
    get() = numbers?.firstOrNull { it.isFaxHomeType() }?.number
    set(value) {
      setNumber(value, Number.FAX_HOME_ARRAY)
    }

  var cell: String?
    @JsonIgnore
    get() = numbers?.firstOrNull { it.isCellType() }?.number
    set(value) {
      setNumber(value, Number.CELL_ARRAY)
    }

  var cellHome: String?
    @JsonIgnore
    get() = numbers?.firstOrNull { it.isCellHomeType() }?.number
    set(value) {
      setNumber(value, Number.CELL_HOME_ARRAY)
    }

  var pager: String?
    @JsonIgnore
    get() = numbers?.firstOrNull { it.isPagerType() }?.number
    set(value) {
      setNumber(value, Number.PAGER_ARRAY)
    }

  /**
   * Linked to private cell number
   */
  var other: String?
    @JsonIgnore
    get() = numbers?.firstOrNull { it.isOtherType() }?.number
    set(value) {
      setNumber(value, Number.OTHER_ARRAY)
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

  /**
   * Remove duplicates and empty numbers.
   */
  fun fixNumbers() {
    // Remove empty numbers.
    numbers?.removeIf { it.number.isNullOrBlank() }
    // Remove equal numbers of same type.
    numbers = numbers?.distinctBy { "${NumberHelper.extractPhonenumber(it.number)}:${it.type?.joinToString()}" }
      ?.toMutableList()
    // Remove numbers of type other, if any number with specified type has same number:
    numbers?.removeIf { current ->
      current.isOtherType() && numbers?.any {
        it != current && NumberHelper.extractPhonenumber(
          it.number
        ) == NumberHelper.extractPhonenumber(current.number)
      } == true
    }
  }

  private fun setEmail(emailAddress: String?, type: EmailType) {
    if (emails == null) {
      emails = mutableListOf()
    }
    var mail = emails?.firstOrNull { it.type == type }
    if (mail == null) {
      mail = Email(type = type)
      emails?.add(mail)
    }
    mail.email = emailAddress
  }

  private fun setNumber(number: String?, type: Array<String>) {
    if (numbers == null) {
      numbers = mutableListOf()
    }
    var num = numbers?.firstOrNull { Number.compare(it.type, type) }
    if (num == null) {
      num = Number()
      num.type = type
      numbers?.add(num)
    }
    num.number = number
    fixNumbers()
  }

  /**
   * as json.
   */
  override fun toString(): String {
    return toJsonString(this)
  }

  class Email(
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

  class Number(
    var number: String? = null,
  ) {
    var type: Array<String>? = null

    fun setHomeType(): Number {
      type = HOME_ARRAY
      return this
    }

    @JsonIgnore
    fun isHomeType(): Boolean {
      return compare(type, HOME_ARRAY)
    }

    fun setWorkType(): Number {
      type = WORK_ARRAY
      return this
    }

    @JsonIgnore
    fun isWorkType(): Boolean {
      return compare(type, WORK_ARRAY)
    }

    fun setCellType(): Number {
      type = CELL_ARRAY
      return this
    }

    @JsonIgnore
    fun isCellType(): Boolean {
      return compare(type, CELL_ARRAY)
    }

    fun setCellHomeType(): Number {
      type = CELL_HOME_ARRAY
      return this
    }

    @JsonIgnore
    fun isCellHomeType(): Boolean {
      return compare(type, CELL_HOME_ARRAY)
    }

    fun setFaxHomeType(): Number {
      type = FAX_HOME_ARRAY
      return this
    }


    @JsonIgnore
    fun isFaxHomeType(): Boolean {
      return compare(type, FAX_HOME_ARRAY)
    }

    fun setFaxWorkType(): Number {
      type = FAX_WORK_ARRAY
      return this
    }

    @JsonIgnore
    fun isFaxWorkType(): Boolean {
      return compare(type, FAX_WORK_ARRAY)
    }

    fun setPagerType(): Number {
      type = PAGER_ARRAY
      return this
    }

    @JsonIgnore
    fun isPagerType(): Boolean {
      return compare(type, PAGER_ARRAY)
    }

    fun setOtherType(): Number {
      type = OTHER_ARRAY
      return this
    }

    @JsonIgnore
    fun isOtherType(): Boolean {
      return compare(type, OTHER_ARRAY)
    }

    companion object {
      fun compare(array1: Array<String>?, array2: Array<String>?): Boolean {
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
      internal var CELL_HOME_ARRAY = arrayOf("cell", "home")
      internal val FAX_HOME_ARRAY = arrayOf("fax", "home")
      internal val FAX_WORK_ARRAY = arrayOf("fax", "work")
      internal val PAGER_ARRAY = arrayOf("pager")
      internal val OTHER_ARRAY = arrayOf("other")
    }
  }
}
