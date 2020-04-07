/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.repository

import mu.KotlinLogging
import org.apache.jackrabbit.commons.JcrUtils
import org.springframework.stereotype.Service
import javax.jcr.*

private val log = KotlinLogging.logger {}

@Service
open class RepositoryService {
    /**
     * @param path Path, nodes are separated by '/', e. g. "world/germany".
     * @param subPath optional sub node of node specified by path.
     */
    @JvmOverloads
    open fun ensureNode(path: String, subPath: String? = null): String {
        var resultPath: String? = null
        runInSession { session ->
            val root: Node = session.rootNode
            val fullPath = if (subPath.isNullOrBlank()) {
                path
            } else {
                "$path/$subPath"
            }
            var current: Node = root
            fullPath.split("/").forEach {
                current = ensureNode(current, it)
            }
            resultPath = current.path
            session.save()
        }
        return resultPath!!
    }

    private fun ensureNode(parentNode: Node, relPath: String): Node {
        if (!parentNode.hasNode(relPath)) {
            log.info { "Creating node ${parentNode.path}/$relPath." }
            return parentNode.addNode(relPath)
        }
        return parentNode.getNode(relPath)
    }

    open fun store(path: String) {
        runInSession { session ->
            val node: Node = if (path.startsWith("/")) {
                session.getNode(path)
            } else {
                session.rootNode.getNode(path)
            }
            node.setProperty("message", "Hello, World!")
            session.save()
        }
    }

    open fun retrieve(path: String) {
        runInSession { session ->
            val node: Node = session.rootNode.getNode(path)
            System.out.println(node.path)
            System.out.println(node.getProperty("message").string)
        }
    }

    fun test() {
        val repository: Repository = JcrUtils.getRepository()
        val session: Session = repository.login(GuestCredentials())
        try {
            val user: String = session.getUserID()
            val name: String = repository.getDescriptor(Repository.REP_NAME_DESC)
            println("Logged in as $user to a $name repository.")
        } finally {
            session.logout()
        }
    }

    private fun runInSession(method: (session: Session) -> Unit) {
        val session: Session = repository.login(credentials)
        try {
            method(session)
        } finally {
            session.logout()
        }
    }

    private val repository: Repository
    private val credentials: Credentials

    init {
        log.info { "Initializing Jcr repository." }
        repository = JcrUtils.getRepository()
        credentials = SimpleCredentials("admin", "admin".toCharArray())
    }
}
