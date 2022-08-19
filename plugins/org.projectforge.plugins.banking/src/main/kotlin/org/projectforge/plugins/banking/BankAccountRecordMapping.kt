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

package org.projectforge.rest.dto

import org.hibernate.search.util.AnalyzerUtils.log
import org.projectforge.framework.json.JsonUtils
import org.projectforge.plugins.banking.BankAccountRecord
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.declaredMemberProperties

class BankAccountRecordMapping {
  /**
   * Key is the data field of BankAccountRecordDO and value is a list of mapping aliases (regexp are supported).
   */
  var mappingMap = mutableMapOf<String, List<String>>()

  private val regexMap = mutableMapOf<String, Regex?>()

  fun getField(header: String): KMutableProperty1<BankAccountRecord, Any>? {
    mappingMap.forEach { mapping ->
      if (mapping.value.any { getRegex(it)?.matches(header) == true }) {
        @Suppress("UNCHECKED_CAST")
        return BankAccountRecord::class.declaredMemberProperties.find { it is KMutableProperty1<*, *> && it.name == mapping.key }
            as KMutableProperty1<BankAccountRecord, Any>
      }
    }
    return null
  }

  fun toJson(): String? {
    if (mappingMap.isEmpty()) {
      return null
    }
    return JsonUtils.toJson(this)
  }

  private fun getRegex(userString: String): Regex? {
    synchronized(regexMap) {
      if (regexMap.containsKey(userString)) {
        return regexMap[userString]
      }
      val regex = createRegex(userString)
      regexMap[userString] = regex
      return regex
    }
  }

  companion object {
    fun createFromJson(json: String?): BankAccountRecordMapping {
      if (json == null) {
        return BankAccountRecordMapping()
      }
      try {
        val mapping = JsonUtils.fromJson(json, BankAccountRecordMapping::class.java)
        return mapping ?: BankAccountRecordMapping()
      } catch (ex: Exception) {
        log.warn("Can't read bank account mapping from json: '$json'.")
        return BankAccountRecordMapping()
      }
    }

    internal fun matches(headerString: String, regex: Regex): Boolean {
      return regex.matches(headerString.trim())
    }

    internal fun createRegex(userString: String): Regex {
      return createRegexString(userString).toRegex(RegexOption.IGNORE_CASE)
    }

    internal fun createRegexString(userString: String): String {
      val sb = StringBuilder()
      userString.forEach { ch ->
        if ("<([{\\^-=\$!|]})+.>".contains(ch)) { // Don't escape * and ?
          sb.append('\\')
        }
        sb.append(ch)
      }
      return sb.toString().replace("*", ".*").replace("?", ".")
    }
  }
}
