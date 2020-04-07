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
import javax.jcr.Node
import javax.jcr.Repository
import javax.jcr.Session
import javax.jcr.SimpleCredentials

private val log = KotlinLogging.logger {}

@Service
open class RepositoryService {
    /**
     * @param parentNodePath Path, nodes are separated by '/', e. g. "world/germany". The nodes of this path must already exist.
     * For creating top level nodes, set parentNode to null, empty string or "/".
     * @param relPath optional sub node of node specified by path.
     */
    open fun ensureNode(parentNodePath: String?, relPath: String): String {
        return runInSession<String> { session ->
            val node = getNode(session, parentNodePath, relPath, true)
            val path = node.path
            session.save()
            path
        }
    }

    @JvmOverloads
    open fun storeProperty(parentNodePath: String?, relPath: String, name: String, value: String, ensureRelNode: Boolean = true) {
        runInSession { session ->
            val node = getNode(session, parentNodePath, relPath, ensureRelNode)
            node.setProperty(name, value)
            session.save()
        }
    }

    open fun retrieveProperty(parentNodePath: String?, relPath: String, name: String): String? {
        return runInSession { session ->
            getNodeOrNull(session, parentNodePath, relPath, false)?.getProperty(name)?.string
        }
    }

    private fun getNode(session: Session, parentNodePath: String?, relPath: String, ensureRelNode: Boolean = true): Node {
        return getNodeOrNull(session, parentNodePath, relPath, ensureRelNode)
                ?: throw IllegalArgumentException("Can't find node ${getFullPath(parentNodePath, relPath)}.")
    }

    private fun getNodeOrNull(session: Session, parentNodePath: String?, relPath: String, ensureRelNode: Boolean = true): Node? {
        val parentNode = if (parentNodePath.isNullOrBlank() || parentNodePath == "/") {
            session.rootNode
        } else if (isAbsolute(parentNodePath)) {
            session.getNode(parentNodePath)
        } else {
            session.rootNode.getNode(parentNodePath)
        }
        return if (ensureRelNode) {
            ensureNode(parentNode, relPath)
        } else if (parentNode.hasNode(relPath)) {
            parentNode.getNode(relPath)
        } else {
            null
        }
    }

    private fun ensureNode(parentNode: Node, relPath: String): Node {
        var current: Node = parentNode
        relPath.split("/").forEach {
            current = if (current.hasNode(it)) {
                current.getNode(it)
            } else {
                log.info { "Creating node ${getFullPath(parentNode, it)}." }
                current.addNode(it)
            }
        }
        return current
    }

    /**
     * return true if given path starts with '/'-
     */
    private fun isAbsolute(path: String): Boolean {
        return path.startsWith("/")
    }

    private fun getFullPath(parentNode: Node, relPath: String): String {
        val parentPath = parentNode.path
        return getFullPath(parentPath, relPath)
    }

    private fun getFullPath(parentPath: String?, relPath: String): String {
        parentPath ?: return relPath
        return if (parentPath.endsWith("/")) "$parentPath$relPath" else "$parentPath/$relPath"
    }

    private fun <T> runInSession(method: (session: Session) -> T): T {
        val session: Session = repository.login(credentials)
        try {
            return method(session)
        } finally {
            session.logout()
        }
    }

    private lateinit var repository: Repository

    private val credentials = SimpleCredentials("admin", "admin".toCharArray())

    fun init(parameters: Map<String, String>) {
        log.info { "Initializing Jcr repository: ${parameters.entries.joinToString { "${it.key}='${it.value}'" }}" }
        repository = JcrUtils.getRepository(parameters)
    }
}
