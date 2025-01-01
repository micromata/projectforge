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

package org.projectforge.rest.core

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpSession
import kotlin.concurrent.timer

/**
 * For storing large objects in the user's session with expiring times. A timer will check every minute for large objects
 * for expiration and will set them to null size. This is useful, if large objects (such as image uploads) will resist
 * in the user's session and the session terminates after several hours.
 */
object ExpiringSessionAttributes {
  /** Store all session attributes for deleting content after expire time. Key SSESSION_ID and the name
   * of the ExpiringAttribute. */
  private val attributesMap = mutableMapOf<String, ExpiringAttribute>()

  init {
    timer("ExpiringSessionAttributesTime", period = 60000) {
      check()
    }
  }

  fun setAttribute(request: HttpServletRequest, name: String, value: Any, ttlMinutes: Int) {
    setAttribute(request.getSession(false), name, value, ttlMinutes)
  }

  fun setAttribute(session: HttpSession, name: String, value: Any, ttlMinutes: Int) {
    val attribute = ExpiringAttribute(value, ttlMinutes)
    session.setAttribute(name, attribute)
    synchronized(attributesMap) {
      attributesMap[getMapKey(session, name)] = attribute
    }
  }

  fun <T> getAttribute(request: HttpServletRequest, name: String, classOfT: Class<T>): T? {
    return getAttribute(request.getSession(false), name, classOfT)
  }

  fun <T> getAttribute(session: HttpSession, name: String, classOfT: Class<T>): T? {
    @Suppress("UNCHECKED_CAST")
    return getAttribute(session, name) as T?
  }

  fun getAttribute(request: HttpServletRequest, name: String): Any? {
    return getAttribute(request.getSession(false), name)
  }

  /**
   * If an ExpringAttribute is found, then it will be renewed (the timestamp will be updated).
   */
  fun getAttribute(session: HttpSession, name: String): Any? {
    checkSession(session)
    val value = session.getAttribute(name) ?: return null
    return if (value is ExpiringAttribute) {
      value.timestamp = System.currentTimeMillis()
      value.value
    }  else {
      value
    }
  }

  fun removeAttribute(request: HttpServletRequest, name: String) {
    return removeAttribute(request.getSession(false), name)
  }

  fun removeAttribute(session: HttpSession, name: String) {
    val value = getAttribute(session, name) ?: return
    if (value is ExpiringAttribute) {
      synchronized(attributesMap) {
        attributesMap.remove(getMapKey(session, name))
      }
    }
    session.removeAttribute(name)
  }

  /**
   * Tidy up expired session attributes. Will be called on each user's session access.
   */
  private fun checkSession(session: HttpSession) {
    val current = System.currentTimeMillis()
    session.attributeNames.iterator().forEach { name ->
      val value = session.getAttribute(name)
      if (value is ExpiringAttribute) {
        if (current - value.timestamp > value.ttlMillis) {
          synchronized(attributesMap) {
            attributesMap.remove(getMapKey(session, name))
          }
          session.removeAttribute(name)
        }
      }
    }
  }

  /**
   * Interval check: Set values of expired session attributes to null for saving memory.
   */
  private fun check() {
    val current = System.currentTimeMillis()
    synchronized(attributesMap) {
      val attributesToRemove = mutableListOf<String>()
      attributesMap.forEach {
        val attribute = it.value
        if (current - attribute.timestamp > attribute.ttlMillis) {
          attribute.value = null // Save memory
          attributesToRemove.add(it.key) // Don't remove here due to ConcurrentModificationException
        }
      }
      attributesToRemove.forEach {
        attributesMap.remove(it)
      }
    }
  }

  private fun getMapKey(session: HttpSession, name: String): String {
    return "${session.id}.$name"
  }

  private class ExpiringAttribute(var value: Any?, ttlMinutes: Int) {
    var timestamp: Long = System.currentTimeMillis()
    val ttlMillis = ttlMinutes * 60000
  }
}
