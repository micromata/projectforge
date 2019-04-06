package org.projectforge.rest.core

import org.projectforge.business.user.UserXmlPreferencesCache
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.utils.CloneHelper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.Serializable
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpSession

@Component
class RestUserPreferencesService {
    private val log = org.slf4j.LoggerFactory.getLogger(RestUserPreferencesService::class.java)

    @Autowired
    private lateinit var userXmlPreferencesCache: UserXmlPreferencesCache

    fun putEntry(request: HttpServletRequest, key: String, value: Any?, persistent: Boolean) {
        putEntry(request.session, key, value, persistent)
    }

    /**
     * Stores the given value for the current user.
     *
     * @param session    Only for demo users, the value will be stored to session, not to [UserXmlPreferencesCache].
     * @param key
     * @param value
     * @param persistent If true, the object will be persisted in the database.
     * @see UserXmlPreferencesCache.putEntry
     */
    fun putEntry(session: HttpSession, key: String, value: Any?, persistent: Boolean) {
        val user = ThreadLocalUserContext.getUser()
        if (user == null || value == null) {
            // Should only occur, if user is not logged in.
            return
        }
        if (AccessChecker.isDemoUser(user) == true && value is Serializable) {
            // Store user pref for demo user only in user's session.
            session.setAttribute(key, value as Serializable?)
            return
        }
        try {
            userXmlPreferencesCache.putEntry(user.id, key, value, persistent)
        } catch (ex: Exception) {
            log.error("Should only occur in maintenance mode: " + ex.message, ex)
        }

    }

    fun getEntry(request: HttpServletRequest, key: String): Any? {
        return getEntry(request.session, key)
    }

    /**
     * Gets the stored user preference entry.
     *
     * @param session Only for demo users, the value will be stored to session, not to [UserXmlPreferencesCache].
     * @param key
     * @return Return a persistent object with this key, if existing, or if not a volatile object with this key, if
     * existing, otherwise null;
     * @see UserXmlPreferencesCache.getEntry
     */
    fun getEntry(session: HttpSession, key: String): Any? {
        val user = ThreadLocalUserContext.getUser()
                ?: // Should only occur, if user is not logged in.
                return null
        val userId = user.id
        if (AccessChecker.isDemoUser(user) == true) {
            // Store user pref for demo user only in user's session.
            var value: Any? = session.getAttribute(key)
            if (value != null) {
                return value
            }
            value = userXmlPreferencesCache.getEntry(userId, key)
            if (value == null || value is Serializable == false) {
                return null
            }
            value = CloneHelper.cloneBySerialization<Any>(value)
            session.setAttribute(key, value as Serializable?)
            return value
        }
        try {
            return userXmlPreferencesCache.getEntry(userId, key)
        } catch (ex: Exception) {
            log.error("Should only occur in maintenance mode: " + ex.message, ex)
            return null
        }

    }

    fun <T : Class<*>> getEntry(request: HttpServletRequest, expectedType: T, key: String): T? {
        return getEntry(request.session, expectedType, key)
    }

    /**
     * Gets the stored user preference entry.
     *
     * @param session      Only for demo users, the value will be stored to session, not to [UserXmlPreferencesCache].
     * @param key
     * @param expectedType Checks the type of the user pref entry (if found) and returns only this object if the object is
     * from the expected type, otherwise null is returned.
     * @return Return a persistent object with this key, if existing, or if not a volatile object with this key, if
     * existing, otherwise null;
     * @see UserXmlPreferencesCache.getEntry
     */
    fun <T : Class<*>> getEntry(session: HttpSession, expectedType: T, key: String): T? {
        val entry = getEntry(session, expectedType, key) ?: return null
        if (expectedType.isAssignableFrom(entry.javaClass) == true) {
            return entry
        }
        // Probably a new software release results in an incompability of old and new object format.
        log.info("Could not get user preference entry: (old) type "
                + entry.javaClass.name
                + " is not assignable to (new) required type "
                + expectedType.name
                + " (OK, probably new software release).")
        return null
    }

    /**
     * Removes the entry under the given key.
     *
     * @param session Only for demo users, the value will be stored to session, not to [UserXmlPreferencesCache].
     * @param key
     * @return The removed entry if found.
     */
    fun removeEntry(session: HttpSession, key: String): Any? {
        val user = ThreadLocalUserContext.getUser()
                ?: // Should only occur, if user is not logged in.
                return null
        if (AccessChecker.isDemoUser(user) == true) {
            session.removeAttribute(key)
        }
        return userXmlPreferencesCache.removeEntry(user.id, key)
    }
}
