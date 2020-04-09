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

package org.projectforge.jcr

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.jcr.Node
import javax.jcr.Session


private val log = KotlinLogging.logger {}

@Service
open class RepositoryBackupService {
    @Autowired
    internal lateinit var repositoryService: RepositoryService

    open fun backupSystemView(absPath: String, ostream: OutputStream, skipBinary: Boolean = false, noRecurse: Boolean = false) {
        return runInSession { session ->
            log.info { "Creating backup of system view of path '$absPath'..." }
            session.exportSystemView(absPath, ostream, skipBinary, noRecurse)
        }
    }

    open fun backupDocumentView(absPath: String, ostream: OutputStream, skipBinary: Boolean = false, noRecurse: Boolean = false) {
        return runInSession { session ->
            log.info { "Creating backup of document view of path '$absPath'..." }
            val node = repositoryService.getNode(session, absPath, null)
            println(NodeInfo(node, true))
            session.exportDocumentView(absPath, ostream, skipBinary, noRecurse)
        }
    }

    open fun backupAsZipArchive(absPath: String, zipOut: ZipOutputStream) {
        return runInSession { session ->
            log.info { "Creating backup of document view and binaries of path '$absPath'..." }
            val zipEntry = ZipEntry("repository.xml")
            zipOut.putNextEntry(zipEntry)
            session.exportDocumentView(absPath, zipOut, true, false)
            findAndZipBinaries(repositoryService.getNode(session, absPath, null), zipOut)
        }
    }

    private fun findAndZipBinaries(node: Node, zipOut: ZipOutputStream) {
        repositoryService.getFiles(node).forEach {
            val fileNode = repositoryService.findFile(node, it.id, null)
            val content =repositoryService.getFileContent(fileNode)
            if (content != null) {
                val zipEntry = ZipEntry(node.path + "/" + it.id)
                zipOut.putNextEntry(zipEntry)
                zipOut.write(content)
            }
        }
        node.nodes?.let {
            while (it.hasNext()) {
                findAndZipBinaries(it.nextNode(), zipOut)
            }
        }
    }

    open fun restore(absPath: String, istream: InputStream, securityConfirmation: String, uuidBehavior: Int) {
        if (securityConfirmation != RepositoryService.RESTORE_SECURITY_CONFIRMATION_IN_KNOW_WHAT_I_M_DOING_REPO_MAY_BE_DESTROYED) {
            throw IllegalArgumentException("You must use the correct security confirmation if you know what you're doing. The repo content may be lost after restoring!")
        }
        return runInSession { session ->
            log.info { "Restoring repository in path '$absPath'..." }
            session.workspace.importXML(absPath, istream, uuidBehavior)
        }
    }

    private fun <T> runInSession(method: (session: Session) -> T): T {
        val session: Session = repositoryService.login()
        try {
            return method(session)
        } finally {
            session.logout()
        }
    }
}
