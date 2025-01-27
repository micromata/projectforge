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

package org.projectforge.framework.persistence.database

import mu.KotlinLogging
import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.json.JsonUtils
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class DatabaseTester {
    private var lastStateAsJson: String = ""

    private lateinit var lastState: Map<Long, PFUserDO>

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    fun test() {
        lastState = userGroupCache.internalGetCopyOfUserMap()
        lastStateAsJson = JsonUtils.toJson(lastState)
        for (t in 0..10) {
            startThread(t)
        }
    }

    private fun startThread(threadNumber: Int) {
        val thread: Thread = object : Thread() {
            override fun run() {
                for (i in 0..1000) {
                    run(threadNumber, i)
                }
            }
        }
        thread.start()
    }

    private fun run(threadNumber: Int, iteration: Int) {
        log.info("Thread #$threadNumber.$iteration")
        userGroupCache.setExpired()
        val newState = userGroupCache.internalGetCopyOfUserMap()
        val newStateAsJson = JsonUtils.toJson(newState)
        if (newState.size != lastState.size) {
            throw IllegalStateException("Size changed: ${newState.size} != ${lastState.size}")
        }
        newState.forEach { id, user ->
            val lastUser = lastState[id] ?: throw IllegalStateException("User with id $id not found in last state")
            if (lastUser.toString() != user.toString()) {
                throw IllegalStateException("User with id $id changed: $lastUser != $user")
            }
        }
        lastState = newState
        lastStateAsJson = newStateAsJson
    }

}
