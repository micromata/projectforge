/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.persistence.history

import de.micromata.genome.db.jpa.history.api.DiffEntry
import de.micromata.genome.db.jpa.history.api.HistProp
import de.micromata.genome.db.jpa.history.api.HistoryEntry
import de.micromata.genome.db.jpa.history.entities.EntityOpType
import org.apache.commons.collections.CollectionUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.builder.ReflectionToStringBuilder
import org.jfree.util.Log
import org.projectforge.business.address.AddressbookDO
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.DisplayNameCapable
import org.projectforge.framework.persistence.jpa.PfEmgrFactory
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.DateHelper
import org.projectforge.framework.utils.NumberHelper.parseInteger
import org.slf4j.LoggerFactory
import java.io.Serializable
import java.sql.Date
import java.sql.Timestamp
import java.util.*
import java.util.function.Consumer
import javax.persistence.EntityManager

/**
 * For storing the hibernate history entries in flat format.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de), Roger Kommer, Florian Blumenstein
 */
open class DisplayHistoryEntry(userGroupCache: UserGroupCache, entry: HistoryEntry<*>) : Serializable {
  var user: PFUserDO? = null

  /**
   * @return the entryType
   */
  val entryType: EntityOpType
  /**
   * @return the propertyName
   */
  /**
   * Use-full for prepending id of children (e. g. entries in a collection displayed in the history table of the parent
   * object). Example: AuftragDO -> AuftragsPositionDO.
   *
   * @param propertyName
   */
  var propertyName: String? = null

  /**
   * @return the propertyType
   */
  var propertyType: String? = null
    private set
  /**
   * @return the oldValue
   */
  /**
   * @param oldValue the oldValue to set
   * @return this for chaining.
   */
  var oldValue: String? = null
  /**
   * @return the newValue
   */
  /**
   * @param newValue the newValue to set
   * @return this for chaining.
   */
  var newValue: String? = null
  val timestamp: java.util.Date
  private fun getUser(userGroupCache: UserGroupCache, userId: String): PFUserDO? {
    if (StringUtils.isBlank(userId)) {
      return null
    }
    val id = parseInteger(userId) ?: return null
    return userGroupCache.getUser(id)
  }

  constructor(
    userGroupCache: UserGroupCache, entry: HistoryEntry<*>, prop: DiffEntry,
    em: EntityManager
  ) : this(userGroupCache, entry) {
    if (prop.newProp != null) {
      propertyType = prop.newProp.type
    }
    if (prop.oldProp != null) {
      propertyType = prop.oldProp.type
    }
    newValue = prop.newValue
    oldValue = prop.oldValue
    var oldObjectValue: Any? = null
    var newObjectValue: Any? = null
    try {
      oldObjectValue = getObjectValue(userGroupCache, em, prop.oldProp)
    } catch (ex: Exception) {
      oldObjectValue = "???"
      log.warn(
        "Error while try to parse old object value '"
            + prop.oldValue
            + "' of prop-type '"
            + prop.javaClass.name
            + "': "
            + ex.message, ex
      )
    }
    try {
      newObjectValue = getObjectValue(userGroupCache, em, prop.newProp)
    } catch (ex: Exception) {
      newObjectValue = "???"
      log.warn(
        "Error while try to parse new object value '"
            + prop.newValue
            + "' of prop-type '"
            + prop.javaClass.name
            + "': "
            + ex.message, ex
      )
    }
    if (oldObjectValue != null) {
      oldValue = objectValueToDisplay(oldObjectValue)
    }
    if (newObjectValue != null) {
      newValue = objectValueToDisplay(newObjectValue)
    }
    propertyName = prop.propertyName
  }

  private fun objectValueToDisplay(value: Any): String {
    return if (value is java.util.Date || value is Date || value is Timestamp) {
      formatDate(value)
    } else toShortNameOfList(value).toString()
  }

  protected open fun getObjectValue(userGroupCache: UserGroupCache, em: EntityManager, prop: HistProp?): Any? {
    if (prop == null) {
      return null
    }
    if (StringUtils.isBlank(prop.value)) {
      return prop.value
    }
    val type = prop.type
    if (String::class.java.name == type) {
      return prop.value
    }
    if (PFUserDO::class.java.name == type) {
      val user = getUser(userGroupCache, prop.value)
      if (user != null) {
        return user
      }
    }
    if (EmployeeDO::class.java.name == type || AddressbookDO::class.java.name == type) {
      val sb = StringBuffer()
      getDBObjects(em, prop).forEach(Consumer { dbObject: Any? ->
        if (dbObject is EmployeeDO) {
          sb.append(dbObject.user!!.getFullname() + ";")
        }
        if (dbObject is AddressbookDO) {
          sb.append(dbObject.title + ";")
        }
      })
      sb.deleteCharAt(sb.length - 1)
      return sb.toString()
    }
    return getDBObjects(em, prop)
  }

  private fun getDBObjects(em: EntityManager, prop: HistProp): List<Any> {
    val ret: MutableList<Any> = ArrayList()
    val emd = PfEmgrFactory.get().metadataRepository.findEntityMetadata(prop.type)
    if (emd == null) {
      ret.add(prop.value)
      return ret
    }
    val sa = StringUtils.split(prop.value, ", ")
    if (sa == null || sa.size == 0) {
      return emptyList()
    }
    for (pks in sa) {
      try {
        val pk = pks.toInt()
        val ent = em.find(emd.javaType, pk)
        if (ent != null) {
          ret.add(ent)
        }
      } catch (ex: NumberFormatException) {
        Log.warn("Cannot parse pk: $prop")
      }
    }
    return ret
  }

  private fun formatDate(objectValue: Any?): String {
    if (objectValue == null) {
      return ""
    }
    if (objectValue is Date) {
      return DateHelper.formatIsoDate(objectValue as java.util.Date?)
    } else if (objectValue is java.util.Date) {
      return DateHelper.formatIsoTimestamp(objectValue as java.util.Date?)
    }
    return objectValue.toString()
  }

  private fun toShortNameOfList(value: Any): Any {
    return if (value is Collection<*>) {
      CollectionUtils.collect(value) { input -> toShortName(input) }
    } else toShortName(value)
  }

  fun toShortName(`object`: Any): String {
    return if (`object` is DisplayNameCapable) `object`.displayName ?: "---" else `object`.toString()
  }

  /**
   * Returns string containing all fields (except the password, via ReflectionToStringBuilder).
   *
   * @return
   */
  override fun toString(): String {
    return ReflectionToStringBuilder(this).toString()
  }

  companion object {
    private val log = LoggerFactory.getLogger(DisplayHistoryEntry::class.java)
    private const val serialVersionUID = 3900345445639438747L
  }

  init {
    timestamp = entry.modifiedAt
    val str = entry.userName
    if (StringUtils.isNotEmpty(str) && "anon" != str) { // Anonymous user, see PfEmgrFactory.java
      val userId = parseInteger(entry.userName)
      if (userId != null) {
        user = userGroupCache.getUser(userId)
      }
    }
    // entry.getClassName();
    // entry.getComment();
    entryType = entry.entityOpType
    // entry.getEntityId();
  }
}
