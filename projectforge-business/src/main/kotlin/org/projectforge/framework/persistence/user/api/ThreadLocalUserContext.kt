/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.persistence.user.api

import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.asContextElement
import mu.KotlinLogging
import org.joda.time.DateTimeZone
import org.projectforge.business.configuration.ConfigurationServiceAccessor
import org.projectforge.business.user.UserLocale
import org.projectforge.business.user.UserTimeZone
import org.projectforge.framework.i18n.I18nHelper
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.text.Collator
import java.time.DayOfWeek
import java.time.ZoneId
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * ThreadLocal context.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object ThreadLocalUserContext {
    private val threadLocalUserContext = ThreadLocal<UserContext?>()
    private val threadLocalLocale = ThreadLocal<Locale?>()

    val userContextAsContextElement: ThreadContextElement<UserContext?>
        get() = threadLocalUserContext.asContextElement(threadLocalUserContext.get())

    /*
    suspend fun getCurrentUser(): UserContext? {
        return if (kotlin.coroutines.coroutineContext == kotlin.coroutines.EmptyCoroutineContext) {
            // Not in a Coroutine: access to ThreadLocal
            threadLocalUserContext.get()
        } else {
            // In a Coroutine: access to CoroutineContext
            kotlin.coroutines.coroutineContext[UserContextElement]?.getUserContext()
        }
    }*/

    val localeAsContextElement: ThreadContextElement<Locale?>
        get() = threadLocalLocale.asContextElement(threadLocalLocale.get())

    /**
     * @return The user of ThreadLocal if exists.
     */
    @JvmStatic
    val loggedInUser: PFUserDO?
        get() = userContext?.user

    /**
     * The logged-in user. If no logged-in user exists, an exception is thrown.
     */
    @JvmStatic
    val requiredLoggedInUser: PFUserDO
        get() = userContext!!.user!!

    @JvmStatic
    var userContext: UserContext?
        get() = threadLocalUserContext.get()
        set(userContext) {
            val oldUser = loggedInUser
            var newUser = userContext?.user
            if (log.isDebugEnabled) {
                log.debug(
                    if (newUser != null) newUser.userDisplayName else if (oldUser != null) oldUser.userDisplayName else "null"
                )
            }
            threadLocalUserContext.set(userContext)
            threadLocalLocale.set(null)
            if (log.isDebugEnabled) {
                newUser = loggedInUser
                log.debug(if (newUser != null) newUser.userDisplayName else "null")
            }
        }

    @JvmStatic
    fun clear() {
        threadLocalUserContext.set(null)
        threadLocalLocale.set(null)
    }

    /**
     * If given user is null, [.clear] is called. Creates a new UserContext object containing the given user.
     *
     * @param user
     * @return UserContext registered or null, if no user given.
     */
    @JvmStatic
    fun setUser(user: PFUserDO?): UserContext? {
        if (user == null) {
            clear()
            return null
        }
        val userContext = UserContext(user)
        ThreadLocalUserContext.userContext = userContext
        return userContext
    }

    /**
     * @return The user id of the ThreadLocal user if exists.
     * @see .getUser
     */
    @JvmStatic
    val loggedInUserId: Long?
        get() = loggedInUser?.id

    /**
     * @return The user id of the ThreadLocal user if exists.
     * @see .getUser
     */
    @JvmStatic
    val requiredLoggedInUserId: Long
        get() = requiredLoggedInUser.id!!


    /**
     * For non logged-in users and public pages, the locale could be set.
     * @return The locale of ThreadLocalLocale or null, if not given.
     */
    fun internalGetThreadLocalLocale(): Locale? {
        return threadLocalLocale.get()
    }
    /**
     * @return The locale of the user if exists, otherwise default locale.
     * @see .getUser
     * @see PFUserDO.getLocale
     */
    /**
     * Only for anonymous usage (needed by translations). Will throw an exception, if an user is already attached.
     *
     * @param locale
     */
    @JvmStatic
    var locale: Locale
        get() = getLocale(null)
        set(locale) {
            check(loggedInUser == null) { "Can't register locale if an user is already registered. setLocale(Locale) should only used for public/anonymous services." }
            threadLocalLocale.set(locale)
        }

    /**
     * @return The locale as String for external systems: e. g. 'de-DE' instead of 'de_DE'.
     * @see .getLocale
     */
    val localeAsString: String
        get() = getLocale(null).toString().replace('_', '-')

    /**
     * If context user's locale is null and the given defaultLocale is not null, then the context user's client locale
     * will be set to given defaultLocale.
     *
     * @param defaultLocale will be used, if the context user or his user locale does not exist. If given, it's the client's
     * locale (browser locale) in common.
     * @return The locale of the user if exists, otherwise the given default locale or if null the system's default
     * locale.
     * @see .getUser
     * @see PFUserDO.getLocale
     */
    @JvmStatic
    fun getLocale(defaultLocale: Locale?): Locale {
        val user = loggedInUser
        return UserLocale.determineUserLocale(user, defaultLocale)
    }

    /**
     * @return The timeZone of the user if exists, otherwise default timezone of the Configuration
     * @see .getUser
     * @see PFUserDO.getTimeZone
     * @see Configuration.getDefaultTimeZone
     */
    @JvmStatic
    val timeZone: TimeZone
        get() = UserTimeZone.determineUserTimeZone()

    /**
     * @return The timeZone of the user if exists, otherwise default timezone of the Configuration
     * @see .getUser
     * @see PFUserDO.getTimeZone
     * @see Configuration.getDefaultTimeZone
     */
    val zoneId: ZoneId
        get() = timeZone.toZoneId()

    @JvmStatic
    @get:Deprecated("")
    val dateTimeZone: DateTimeZone
        get() {
            val timeZone = timeZone
            return DateTimeZone.forTimeZone(timeZone)
        }

    /**
     * The first day of the week, configured at the given user, if not configured [org.projectforge.business.configuration.ConfigurationService.getDefaultFirstDayOfWeek] is
     * used.
     */
    @JvmStatic
    val firstDayOfWeek: DayOfWeek?
        get() {
            val user = loggedInUser
            if (user != null) {
                val firstDayOfWeek = user.firstDayOfWeek
                if (firstDayOfWeek != null) {
                    return firstDayOfWeek
                }
            }
            return ConfigurationServiceAccessor.get().defaultFirstDayOfWeek
        }

    /**
     * @return 1 - Monday, ..., 7 - Sunday
     */
    @JvmStatic
    val firstDayOfWeekValue: Int
        get() = firstDayOfWeek!!.value

    @JvmStatic
    fun getLocalizedMessage(messageKey: String?, vararg params: Any?): String {
        return I18nHelper.getLocalizedMessage(loggedInUser, messageKey, *params)
    }

    @JvmStatic
    fun getLocalizedString(key: String?): String {
        return I18nHelper.getLocalizedMessage(loggedInUser, key)
    }

    /**
     * Use this instead of String[String.compareTo], because it uses the user's locale for comparison.
     *
     * @param a
     * @param b
     * @return
     */
    fun localeCompare(a: String?, b: String?): Int {
        return localeComparator.compare(a, b)
    }

    /**
     * Use this instead of String[String.compareTo], because it uses the user's locale for comparison.
     *
     * @return
     */
    val localeComparator: Comparator<Any?>
        get() = Collator.getInstance(locale)
}
