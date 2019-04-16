package org.projectforge.rest.core

import javax.servlet.http.HttpSession
import kotlin.concurrent.timer

/**
 * For storing large objects in the user's session with expiring times. A timer will check every minute for large objects
 * for expiration and will set them to null size. This is useful, if large objects (such as image uploads) will resist
 * in the user's session and the session terminates after several hours.
 */
object ExpiringSessionAttributes {
    /** Store all session attributes for deleting content after expire time. */
    private val attributesMap = mutableMapOf<Long, ExpiringAttribute>()
    private var counter = 0L

    init {
        timer("ExpiringSessionAttributesTime", period = 60000) {
            check()
        }
    }

    fun setAttribute(session: HttpSession, name: String, value: Any, ttlMinutes: Int) {
        val attribute = ExpiringAttribute(System.currentTimeMillis(), value, ttlMinutes)
        session.setAttribute(name, attribute)
        attributesMap.put(attribute.index, attribute)
    }

    fun getAttribute(session: HttpSession, name: String): Any? {
        checkSession(session)
        val value = session.getAttribute(name)
        if (value == null)
            return null
        if (value is ExpiringAttribute)
            return value.value
        return value
    }

    fun removeAttribute(session: HttpSession, name: String) {
        val value = getAttribute(session, name)
        if (value == null)
            return
        if (value is ExpiringAttribute) {
            attributesMap.remove(value.index)
        }
        session.removeAttribute(name)
    }

    private fun checkSession(session: HttpSession) {
        val current = System.currentTimeMillis()
        session.attributeNames.iterator().forEach {
            val value = session.getAttribute(it)
            if (value is ExpiringAttribute) {
                if (current - value.timestamp > value.ttlMillis) {
                    attributesMap.remove(value.index)
                    session.removeAttribute(it)
                }
            }
        }
    }

    private fun check() {
        val current = System.currentTimeMillis()
        attributesMap.forEach {
            val attribute = it.value
            if (current - attribute.timestamp > attribute.ttlMillis) {
                attribute.value = null // Save memory
                attributesMap.remove(it.key)
            }
        }
    }

    private class ExpiringAttribute(val timestamp: Long, var value: Any?, ttlMinutes: Int) {
        val ttlMillis = ttlMinutes * 60000
        val index = ++counter
    }
}