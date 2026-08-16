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

package org.projectforge.menu

import mu.KotlinLogging
import org.projectforge.business.user.service.UserPrefService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.utils.RecentQueue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * The menu entries a user opened last, most recent first: the history the quick access search of
 * projectforge-next offers before the user has typed anything.
 *
 * Kept here and not in the browser, because all three frontends contribute: the entry a user opened in
 * Wicket should be offered by the search in projectforge-next as well, and a second browser should know
 * about it too. Each frontend reports a click on a menu entry once, centrally (MenuRest.reportMenuUsage,
 * NavAbstractPanel for Wicket, the navigation components of projectforge-webapp and projectforge-next).
 *
 * Only ids are stored, never titles or urls: the menu is the truth about both, and it is built per user
 * and access checked, so an entry the user may no longer open simply stops resolving in
 * [buildRecentMenu] instead of lingering here as a dead row.
 */
@Service
open class RecentMenuEntriesService {
    @Autowired
    private lateinit var userPrefService: UserPrefService

    /**
     * Reports an entry the user just opened.
     *
     * @param reportedKey [MenuItem.key] as the client rendered it - the only identity all three
     * frontends have (the Wicket menu keeps nothing else, see WicketMenuBuilder). Anything that is no
     * menu entry key is ignored, so a client may report what it has without knowing the rules.
     */
    open fun append(reportedKey: String?) {
        val id = normalizeKey(reportedKey)
        if (id == null) {
            log.debug { "Ignoring reported menu key '$reportedKey': no menu entry id." }
            return
        }
        val userId = ThreadLocalUserContext.loggedInUserId ?: return
        getQueue(userId).append(id)
    }

    /**
     * The ids of the entries the user opened last, most recent first. May contain ids the user can't open
     * anymore; [buildRecentMenu] is where that is decided.
     */
    open fun getRecentIds(): List<String> {
        val userId = ThreadLocalUserContext.loggedInUserId ?: return emptyList()
        return getQueue(userId).recentList ?: emptyList()
    }

    /**
     * The recent entries as a flat menu, most recent first, at most [MAX_RECENT_VISIBLE] of them.
     *
     * Resolved against the menus given - the ones the user is being served right now - rather than
     * against the [org.projectforge.menu.builder.MenuCreator] tree: those are already access checked and
     * their titles are already translated and their badges already counted, so an entry the user lost the
     * right to disappears here for free. It also picks the registration the user actually sees for an
     * entry registered in more than one category (ORDER_LIST hangs below PROJECT_MANAGEMENT and below
     * FIBU, with mutually exclusive access), which a lookup by id could not.
     *
     * @param menus in the order their category should win for an entry appearing in several of them: the
     * main menu first, so the user gets the entry as they know it and not as a favourite.
     */
    open fun buildRecentMenu(vararg menus: Menu): Menu {
        val byId = mutableMapOf<String, MenuItem>()
        menus.forEach { menu ->
            menu.getAllDescendants().forEach { item ->
                val id = item.id ?: return@forEach
                // A leaf only: a category heading is nothing to navigate to. RESTCALL entries (the logout)
                // are no page either - they are rejected on the way in, this is the second net.
                if (!item.isLeaf() || item.type == MenuItemTargetType.RESTCALL) return@forEach
                byId.putIfAbsent(id, item)
            }
        }
        val recentMenu = Menu()
        for (id in getRecentIds()) {
            val item = byId[id] ?: continue
            recentMenu.add(item)
            if (recentMenu.menuItems.size >= MAX_RECENT_VISIBLE) break
        }
        // No postProcess(): the items are the ones of an already processed menu, and it would accumulate
        // their badge counters into this menu's badge a second time.
        return recentMenu
    }

    private fun getQueue(userId: Long): RecentQueue<String> {
        var queue: RecentQueue<String>? = null
        try {
            @Suppress("UNCHECKED_CAST")
            queue = userPrefService.getEntry(PREF_AREA, PREF_NAME, RecentQueue::class.java, userId)
                    as? RecentQueue<String>
        } catch (ex: Exception) {
            // A pref written by an incompatible version: the history is worth no more than a log entry.
            log.error("Unexpected exception while getting the recent menu entries of user #$userId: ${ex.message}.", ex)
        }
        if (queue == null) {
            queue = RecentQueue(MAX_RECENT_STORED)
            // Persistent, unlike the queues of TimesheetRecentService: those are rebuilt from the
            // timesheets after a restart, this one has no other source.
            userPrefService.putEntry(PREF_AREA, PREF_NAME, queue, true, userId)
        } else {
            // Lets a change of the constant take effect on a queue written by an earlier version.
            queue.setMaxSize(MAX_RECENT_STORED)
        }
        return queue
    }

    companion object {
        /**
         * More than [MAX_RECENT_VISIBLE] are stored, so entries the user lost the right to don't crowd the
         * visible rows out until they have been clicked past.
         */
        internal const val MAX_RECENT_STORED = 20

        /** How many entries the quick access search offers before the user has typed anything. */
        internal const val MAX_RECENT_VISIBLE = 5

        /** Guards the pref against a client posting something long into it. No menu id comes close. */
        private const val MAX_KEY_LENGTH = 60

        internal val PREF_AREA = RecentMenuEntriesService::class.java.name
        internal const val PREF_NAME = "recent.menuEntries"

        /**
         * Keys that are no menu entry, although they are shaped like one: the parent carrying the user's
         * full name in the account menu (MenuRest), a group the user named while customizing their
         * favourites (MenuCustomizerRest), and the logout, which is a REST call and not a page.
         */
        private val REJECTED_IDS = setOf("MY_MENU", "username", "LOGOUT")
        private const val CUSTOM_GROUP_PREFIX = "CUSTOM_GROUP_"
        private val ID_REGEX = Regex("[A-Za-z0-9_]+")

        /**
         * The [org.projectforge.menu.builder.MenuItemDef.id] a reported [MenuItem.key] stands for, or null
         * if it stands for none.
         *
         * The key is qualified by its category in the main menu (`COMMON.ADDRESS_LIST`, see
         * MenuItemDef.createMenu) and bare in the favourites and the account menu (`ADDRESS_LIST`), which
         * is why the part behind the last period is the id - the same normalization
         * FavoritesMenuReaderWriter does when reading a stored favourite.
         *
         * Pure on purpose: this is the part worth testing without a Spring context.
         */
        internal fun normalizeKey(reportedKey: String?): String? {
            val key = reportedKey?.trim()
            if (key.isNullOrEmpty() || key.length > MAX_KEY_LENGTH) return null
            val id = key.substringAfterLast('.')
            // Rejects a favourite folder the user renamed ("menu-3") and a custom group ("custom-group-17")
            // along with anything else that is no id.
            if (!ID_REGEX.matches(id)) return null
            if (id in REJECTED_IDS || id.startsWith(CUSTOM_GROUP_PREFIX)) return null
            return id
        }
    }
}
