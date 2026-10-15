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

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.user.UserPrefCache
import org.projectforge.business.user.service.UserPrefService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.utils.RecentQueue
import org.projectforge.menu.builder.MenuCreator
import org.projectforge.menu.builder.MenuCreatorContext
import org.projectforge.menu.builder.MenuItemDefId
import org.springframework.beans.factory.annotation.Autowired

class RecentMenuEntriesServiceTest : AbstractTestBase() {
    @Autowired
    private lateinit var recentMenuEntriesService: RecentMenuEntriesService

    @Autowired
    private lateinit var menuCreator: MenuCreator

    @Autowired
    private lateinit var userPrefService: UserPrefService

    @Autowired
    private lateinit var userPrefCache: UserPrefCache

    @Test
    fun normalizeKeyTest() {
        // The favourites and the account menu send the bare id, the main menu qualifies it by its category:
        assertEquals("ADDRESS_LIST", RecentMenuEntriesService.normalizeKey("ADDRESS_LIST"))
        assertEquals("ADDRESS_LIST", RecentMenuEntriesService.normalizeKey("COMMON.ADDRESS_LIST"))
        assertEquals("ADDRESS_LIST", RecentMenuEntriesService.normalizeKey("  COMMON.ADDRESS_LIST  "))
        assertEquals("C", RecentMenuEntriesService.normalizeKey("A.B.C"), "The part behind the last period.")
        // The keys of a favourite folder the user renamed, of a group they created while customizing their
        // menu, of the item carrying their name, and of the logout - none of them is a page:
        assertNull(RecentMenuEntriesService.normalizeKey("menu-3"))
        assertNull(RecentMenuEntriesService.normalizeKey("custom-group-17"))
        assertNull(RecentMenuEntriesService.normalizeKey("CUSTOM_GROUP_17"))
        assertNull(RecentMenuEntriesService.normalizeKey("MY_MENU"))
        assertNull(RecentMenuEntriesService.normalizeKey("username"))
        assertNull(RecentMenuEntriesService.normalizeKey("LOGOUT"))
        assertNull(RecentMenuEntriesService.normalizeKey(null))
        assertNull(RecentMenuEntriesService.normalizeKey(""))
        assertNull(RecentMenuEntriesService.normalizeKey("   "))
        assertNull(RecentMenuEntriesService.normalizeKey("COMMON."), "Nothing behind the period.")
        assertNull(RecentMenuEntriesService.normalizeKey("<script>alert(1)</script>"))
        assertNull(RecentMenuEntriesService.normalizeKey("A".repeat(100)))
    }

    @Test
    fun appendTest() {
        logon(TEST_USER)
        clearHistory()
        recentMenuEntriesService.append("COMMON.ADDRESS_LIST")
        recentMenuEntriesService.append("FIBU.ORDER_LIST")
        assertEquals(listOf("ORDER_LIST", "ADDRESS_LIST"), recentMenuEntriesService.getRecentIds())

        // Used again: to the front, not a second time.
        recentMenuEntriesService.append("ADDRESS_LIST")
        assertEquals(listOf("ADDRESS_LIST", "ORDER_LIST"), recentMenuEntriesService.getRecentIds())

        // Nothing a menu entry key could be:
        recentMenuEntriesService.append("menu-3")
        recentMenuEntriesService.append(null)
        assertEquals(listOf("ADDRESS_LIST", "ORDER_LIST"), recentMenuEntriesService.getRecentIds())

        // More than the queue holds: the oldest fall off the tail.
        repeat(RecentMenuEntriesService.MAX_RECENT_STORED + 5) { recentMenuEntriesService.append("ENTRY_$it") }
        val ids = recentMenuEntriesService.getRecentIds()
        assertEquals(RecentMenuEntriesService.MAX_RECENT_STORED, ids.size)
        assertEquals("ENTRY_${RecentMenuEntriesService.MAX_RECENT_STORED + 4}", ids.first())
        assertFalse(ids.contains("ADDRESS_LIST"), "Pushed out by the newer entries.")
    }

    /**
     * The one thing that could fail silently in production: that a [org.projectforge.framework.utils.RecentQueue]
     * of strings survives the json serialization of UserPrefDao.
     */
    @Test
    fun persistenceTest() {
        val userId = logon(TEST_USER2).id!!
        clearHistory()
        recentMenuEntriesService.append("COMMON.ADDRESS_LIST")
        recentMenuEntriesService.append("COMMON.TASK_TREE")
        // Written and dropped from the cache for this user only, while they are logged in: flushToDB checks
        // the access and writes nothing after a logoff, and setExpired would flush the whole cache, whose
        // entries of other users may be older than the last recreateDataBase of another test class.
        userPrefCache.flushToDB(userId)
        // The next access has to read the pref back from the database.
        assertNull(userPrefCache.getUserPreferencesData(userId), "Flushed users are dropped from the cache.")
        assertEquals(listOf("TASK_TREE", "ADDRESS_LIST"), recentMenuEntriesService.getRecentIds())
    }

    @Test
    fun buildRecentMenuTest() {
        logon(TEST_USER)
        clearHistory()
        val mainMenu = menuCreator.build(MenuCreatorContext(ThreadLocalUserContext.loggedInUser!!))
        // Taken from the menu itself: which entries an account may see is its rights' business, and this
        // test is about the resolution, not about the rights.
        val leaves = mainMenu.getAllDescendants().filter { it.isLeaf() && it.id != null }
        assertTrue(leaves.size > 2, "The test user should see a menu at all.")
        val first = leaves[0].id!!
        val second = leaves[1].id!!

        recentMenuEntriesService.append(first)
        recentMenuEntriesService.append(second)
        var recentMenu = recentMenuEntriesService.buildRecentMenu(mainMenu)
        assertEquals(listOf(second, first), recentMenu.menuItems.map { it.id }, "Most recently used first.")
        recentMenu.menuItems.forEach {
            assertNotNull(it.title, "Resolved out of the built menu, so the title is translated.")
            assertNotNull(it.url)
        }

        // An id of no entry of the given menu: dropped instead of lingering as a dead row. This is also
        // what happens to an entry the user lost the right to see.
        recentMenuEntriesService.append("NO_SUCH_MENU_ENTRY")
        recentMenu = recentMenuEntriesService.buildRecentMenu(mainMenu)
        assertEquals(listOf(second, first), recentMenu.menuItems.map { it.id })

        // The account menu is built by hand in MenuRest, and FEEDBACK sits in no MenuCreator category, so
        // it resolves only because the menus the user is served are what is searched.
        val myAccountMenu = Menu()
        myAccountMenu.add(MenuItem(MenuItemDefId.FEEDBACK))
        recentMenuEntriesService.append(MenuItemDefId.FEEDBACK.id)
        assertNull(menuCreator.findById(MenuItemDefId.FEEDBACK.id), "Precondition of the assertion below.")
        recentMenu = recentMenuEntriesService.buildRecentMenu(mainMenu, myAccountMenu)
        assertEquals(MenuItemDefId.FEEDBACK.id, recentMenu.menuItems.first().id)

        // The logout is a REST call, not a page: never offered, even if a client reports it.
        val logoutMenu = Menu()
        logoutMenu.add(MenuItem(MenuItemDefId.LOGOUT, type = MenuItemTargetType.RESTCALL))
        recentMenuEntriesService.append(MenuItemDefId.LOGOUT.id)
        assertFalse(recentMenuEntriesService.getRecentIds().contains(MenuItemDefId.LOGOUT.id))
        assertFalse(
            recentMenuEntriesService.buildRecentMenu(mainMenu, logoutMenu).menuItems.any {
                it.id == MenuItemDefId.LOGOUT.id
            }
        )

        // Only as many as the quick access search shows, however many are stored.
        leaves.forEach { recentMenuEntriesService.append(it.id) }
        assertTrue(recentMenuEntriesService.getRecentIds().size > RecentMenuEntriesService.MAX_RECENT_VISIBLE)
        assertEquals(
            RecentMenuEntriesService.MAX_RECENT_VISIBLE,
            recentMenuEntriesService.buildRecentMenu(mainMenu).menuItems.size
        )
    }

    /**
     * The cache is a bean shared by all tests of this class, so each starts from an empty history.
     * Overwritten rather than removed: UserPrefCache.remove is not implemented.
     */
    private fun clearHistory() {
        userPrefService.putEntry(
            RecentMenuEntriesService.PREF_AREA,
            RecentMenuEntriesService.PREF_NAME,
            RecentQueue<String>(RecentMenuEntriesService.MAX_RECENT_STORED),
            true,
        )
    }
}
