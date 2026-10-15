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

package org.projectforge.jcr

import io.github.oshai.kotlinlogging.KotlinLogging
import javax.jcr.Node
import javax.jcr.Session
import javax.jcr.SimpleCredentials
import javax.jcr.ValueFactory


private val log = KotlinLogging.logger {}

/**
 * Handles close and clean-up. Thin wrapper for [Session]
 */
open class SessionWrapper(repoStore: OakStorage) {
    val session: Session = login(repoStore)

    /**
     * JAAS loads Oak's LoginModule via the thread context classloader. On threads not created by the servlet
     * container (e.g. coroutine workers of jobs) this is the plain app classloader of the Spring Boot fat jar,
     * which doesn't see BOOT-INF/lib ("No LoginModule found for ...LoginModuleImpl").
     */
    private fun login(repoStore: OakStorage): Session {
        val thread = Thread.currentThread()
        val origClassLoader = thread.contextClassLoader
        thread.contextClassLoader = SessionWrapper::class.java.classLoader
        try {
            return repoStore.repository.login(credentials)
        } finally {
            thread.contextClassLoader = origClassLoader
        }
    }

    val valueFactory: ValueFactory
        get() = session.valueFactory

    val rootNode: Node
        get() = session.rootNode

    fun getNode(absPath: String): Node {
        return session.getNode(absPath)
    }

    fun nodeExists(absPath: String): Boolean {
        return session.nodeExists(absPath)
    }

    fun save() {
        session.save()
    }

    fun logout() {
        session.save()
        session.logout()
    }

    companion object {
        private val credentials = SimpleCredentials("admin", "admin".toCharArray())
    }
}
