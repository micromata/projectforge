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

package org.projectforge.framework.persistence.history

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.builder.ReflectionToStringBuilder
import org.projectforge.business.address.AddressbookDO
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.user.UserGroupCache
import org.projectforge.common.BeanHelper
import org.projectforge.common.i18n.I18nEnum
import org.projectforge.common.props.PropUtils
import org.projectforge.framework.DisplayNameCapable
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.DateHelper
import org.projectforge.framework.utils.NumberHelper.parseLong
import org.slf4j.LoggerFactory
import java.io.Serializable
import java.lang.reflect.Field
import java.sql.Date
import java.sql.Timestamp
import java.util.*
import java.util.function.Consumer
import jakarta.persistence.EntityManager

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
    val id = parseLong(userId) ?: return null
    return userGroupCache.getUser(id)
  }

  constructor(
    userGroupCache: UserGroupCache, entry: HistoryEntry<*>, diffEntry: DiffEntry,
    em: EntityManager
  ) : this(userGroupCache, entry) {
    diffEntry.newProp?.let {
      propertyType = it.type
    }
    diffEntry.oldProp?.let {
      propertyType = it.type
    }
    newValue = diffEntry.newValue
    oldValue = diffEntry.oldValue
    var processed = false

    val type = entry.entityName
    var clazz: Class<*>? = null
    try {
      clazz = Class.forName(type)
    } catch (ex: ClassNotFoundException) {
      log.warn("Class '$type' not found.")
    }
    if (clazz != null && !diffEntry.propertyName.isNullOrBlank()) {
      val field = BeanHelper.getDeclaredField(clazz, diffEntry.propertyName)
      if (field == null) {
        if (log.isDebugEnabled) {
          log.debug("No such field '${diffEntry.propertyName}.${diffEntry.propertyName}'.")
        }
      } else {
        field.isAccessible = true
        if (field.type.isEnum) {
          oldValue = getI18nEnumTranslation(field, diffEntry.oldValue)
          newValue = getI18nEnumTranslation(field, diffEntry.newValue)
          processed = true // Nothing to convert.
        }
      }
      propertyName = translateProperty(diffEntry, clazz)
    } else {
      propertyName = diffEntry.propertyName
    }
    if (!processed) {
      var oldObjectValue: Any?
      var newObjectValue: Any?
      try {
        oldObjectValue = getObjectValue(userGroupCache, em, diffEntry.oldProp)
      } catch (ex: Exception) {
        oldObjectValue = "???"
        log.warn(
          "Error while try to parse old object value '"
              + diffEntry.oldValue
              + "' of prop-type '"
              + diffEntry.javaClass.name
              + "': "
              + ex.message, ex
        )
      }
      try {
        newObjectValue = getObjectValue(userGroupCache, em, diffEntry.newProp)
      } catch (ex: Exception) {
        newObjectValue = "???"
        log.warn(
          "Error while try to parse new object value '"
              + diffEntry.newValue
              + "' of prop-type '"
              + diffEntry.javaClass.name
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
    }
  }

  private fun objectValueToDisplay(value: Any): String {
    return if (value is java.util.Date || value is Date || value is Timestamp) {
      formatDate(value)
    } else toShortNames(value)
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
      val value = prop.value
      if (!value.isNullOrBlank() && !value.contains(",")) {
        // Single user expected.
        val user = getUser(userGroupCache, prop.value ?: "###")
        if (user != null) {
          return user
        }
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
    /*val emd = PfEmgrFactory.get().metadataRepository.findEntityMetadata(prop.type)
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
        log.warn("Cannot parse pk: $prop")
      }
    }*/
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

  private fun toShortNames(value: Any): String {
    return if (value is Collection<*>) {
      value.map { input -> toShortName(input) }.sorted().joinToString()
    } else toShortName(value)
  }

  fun toShortName(obj: Any?): String {
    obj ?: return ""
    return if (obj is DisplayNameCapable) obj.displayName ?: "---" else obj.toString()
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
    timestamp = entry.modifiedAt!!
    val str = entry.modifiedBy
    if (StringUtils.isNotEmpty(str) && "anon" != str) { // Anonymous user, see PfEmgrFactory.java
      val userId = parseLong(entry.modifiedBy)
      if (userId != null) {
        user = userGroupCache.getUser(userId)
      }
    }
    // entry.getClassName();
    // entry.getComment();
    entryType = entry.entityOpType!!
    // entry.getEntityId();
  }

  private fun getI18nEnumTranslation(field: Field, value: String?): String? {
    if (value == null) {
      return ""
    }
    val i18nEnum = I18nEnum.create(field.type, value) as? I18nEnum ?: return value
    return translate(i18nEnum.i18nKey)
  }

  /**
   * Tries to find a PropertyInfo annotation for the property field referred in the given diffEntry.
   * If found, the property name will be returned translated, if not, the property will be returned unmodified.
   */
  private fun translateProperty(diffEntry: DiffEntry, clazz: Class<*>): String? {
    // Try to get the PropertyInfo containing the i18n key of the property for translation.
    var propertyName = PropUtils.get(clazz, diffEntry.propertyName)?.i18nKey
    if (propertyName != null) {
      // translate the i18n key:
      propertyName = translate(propertyName)
    } else {
      propertyName = diffEntry.propertyName
    }
    return propertyName
  }

}
