/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

import mu.KotlinLogging
import javax.jcr.Node
import javax.jcr.Session
import javax.jcr.SimpleCredentials
import javax.jcr.ValueFactory


private val log = KotlinLogging.logger {}

/**
 * Handles close and clean-up. Thin wrapper for [Session]
 */
open class SessionWrapper(private val repoService: RepoService) {
    val session: Session = repoService.repository.login(credentials)

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
        repoService.fileStore?.flush()
        session.save()
    }

    companion object {
        private val credentials = SimpleCredentials("admin", "admin".toCharArray())
    }
}
