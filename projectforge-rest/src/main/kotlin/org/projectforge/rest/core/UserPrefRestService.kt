/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.business.user.service.UserPrefService
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.utils.CloneHelper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.Serializable
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpSession

/**
 * Uses [UserPrefService].
 */
@Component
class UserPrefRestService {
    private val log = org.slf4j.LoggerFactory.getLogger(UserPrefRestService::class.java)

    @Autowired
    private lateinit var userPrefService: UserPrefService

    fun putEntry(request: HttpServletRequest, area: String, name: String, value: Any?, persistent: Boolean) {
        putEntry(request.session, area, name, value, persistent)
    }

    /**
     * Stores the given value for the current user.
     *
     * @param session    Only for demo users, the value will be stored to session, not to [UserPrefService].
     * @param persistent If true, the object will be persisted in the database.
     */
    fun putEntry(session: HttpSession, area: String, name: String, value: Any?, persistent: Boolean) {
        val user = ThreadLocalUserContext.getUser()
        if (user == null || value == null) {
            // Should only occur, if user is not logged in.
            return
        }
        if (AccessChecker.isDemoUser(user) && value is Serializable) {
            // Store user pref for demo user only in user's session.
            session.setAttribute(getSessionAttributename(area, name), value as Serializable?)
            return
        }
        try {
            userPrefService.putEntry(area, name, value, persistent)
        } catch (ex: Exception) {
            log.error("Should only occur in maintenance mode: " + ex.message, ex)
        }

    }

    fun getEntry(request: HttpServletRequest, area: String, name: String): Any? {
        return getEntry(request.session, area, name)
    }

    /**
     * Gets the stored user preference entry.
     *
     * @param session Only for demo users, the value will be stored to session, not to [UserPrefService].
     * @param area
     * @param name
     * @return Return a persistent object with this name, if existing, or if not a volatile object with this name, if
     * existing, otherwise null;
     */
    fun getEntry(session: HttpSession, area: String, name: String): Any? {
        val user = ThreadLocalUserContext.getUser()
                ?: // Should only occur, if user is not logged in.
                return null
        if (AccessChecker.isDemoUser(user)) {
            // Store user pref for demo user only in user's session.
            var value: Any? = session.getAttribute(getSessionAttributename(area, name))
            if (value != null) {
                return value
            }
            value = userPrefService.getEntry(area, name)
            if (value == null || value !is Serializable) {
                return null
            }
            value = CloneHelper.cloneBySerialization<Any>(value)
            session.setAttribute(getSessionAttributename(area, name), value as Serializable?)
            return value
        }
        try {
            return userPrefService.getEntry(area, name)
        } catch (ex: Exception) {
            log.error("Should only occur in maintenance mode: " + ex.message, ex)
            return null
        }

    }

    fun <T : Class<*>> getEntry(request: HttpServletRequest, expectedType: Class<T>, area: String, name: String): T? {
        return getEntry(request.session, expectedType, area, name)
    }

    /**
     * Gets the stored user preference entry.
     *
     * @param session      Only for demo users, the value will be stored to session, not to [UserPrefService].
     * @param name
     * @param expectedType Checks the type of the user pref entry (if found) and returns only this object if the object is
     * from the expected type, otherwise null is returned.
     * @return Return a persistent object with this name, if existing, or if not a volatile object with this name, if
     * existing, otherwise null;
     */
    fun <T : Class<*>> getEntry(session: HttpSession, expectedType: Class<T>, area: String, name: String): T? {
        val entry = getEntry(session, area, name) ?: return null
        if (expectedType.isAssignableFrom(entry.javaClass)) {
            @Suppress("UNCHECKED_CAST")
            return entry as T
        }
        // Probably a new software release results in an incompability of old and new object format.
        log.info("Could not get user preference entry: (old) type "
                + entry.javaClass.name
                + " is not assignable tox (new) required type "
                + expectedType.name
                + " (OK, probably new software release).")
        return null
    }

    /**
     * Removes the entry under the given name.
     *
     * @param session Only for demo users, the value will be stored to session, not to [UserPrefService].
     * @param area
     * @param name
     * @return The removed entry if found.
     */
    fun removeEntry(session: HttpSession, area: String, name: String) {
        val user = ThreadLocalUserContext.getUser()
                ?: // Should only occur, if user is not logged in.
                return
        if (AccessChecker.isDemoUser(user)) {
            // Remove user pref for demo user only from user's session.
            session.removeAttribute(getSessionAttributename(area, name))
            return
        }
        userPrefService.removeEntry(area, name)
    }

    private fun getSessionAttributename(area: String, name: String): String {
        return "$area.$name"
    }
}
