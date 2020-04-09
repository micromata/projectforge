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
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.jcr.Node
import javax.jcr.Session


private val log = KotlinLogging.logger {}

@Service
open class RepoBackupService {
    @Autowired
    internal lateinit var repoService: RepoService

    open fun backupSystemView(absPath: String, ostream: OutputStream, skipBinary: Boolean = false, noRecurse: Boolean = false) {
        return runInSession { session ->
            log.info { "Creating backup of system view of path '$absPath'..." }
            session.exportSystemView(absPath, ostream, skipBinary, noRecurse)
        }
    }

    open fun backupDocumentView(absPath: String, ostream: OutputStream, skipBinary: Boolean = false, noRecurse: Boolean = false) {
        return runInSession { session ->
            log.info { "Creating backup of document view of path '$absPath'..." }
            val node = repoService.getNode(session, absPath, null)
            println(NodeInfo(node, true))
            session.exportDocumentView(absPath, ostream, skipBinary, noRecurse)
        }
    }

    open fun backupAsZipArchive(absPath: String, archiveName: String, zipOut: ZipOutputStream) {
        val archivNameWithoutExtension = if (archiveName.contains('.')) {
            archiveName.substring(0, archiveName.indexOf('.'))
        } else {
            archiveName
        }
        return runInSession { session ->
            log.info { "Creating backup of document view and binaries of path '$absPath' as '$archiveName'..." }
            zipOut.putNextEntry(createZipEntry(archivNameWithoutExtension, "repository.xml"))
            session.exportDocumentView(absPath, zipOut, true, false)
            writeToZip(repoService.getNode(session, absPath, null), archivNameWithoutExtension, zipOut)
        }
    }

    private fun writeToZip(node: Node, archiveName: String, zipOut: ZipOutputStream) {
        val fileList = repoService.getFiles(node)
        if (!fileList.isNullOrEmpty()) {
            fileList.forEach {
                val fileNode = repoService.findFile(node, it.id, null)
                val content = repoService.getFileContent(fileNode)
                if (content != null) {
                    val fileName = PFJcrUtils.createSafeFilename(it)
                    zipOut.putNextEntry(createZipEntry(archiveName, "${node.path}", fileName))
                    zipOut.write(content)
                }
            }
            zipOut.putNextEntry(createZipEntry(archiveName, node.path, "files.txt"))
            val fileListAsString = fileList.joinToString(separator = "\n") { "${PFJcrUtils.createSafeFilename(it)} ${PFJcrUtils.formatBytes(it.size)} ${it.fileName}" }
            zipOut.write(fileListAsString.toByteArray(StandardCharsets.UTF_8))
        }
        val nodeInfo = NodeInfo(node, false)
        zipOut.putNextEntry(createZipEntry(archiveName, node.path, "node.json"))
        zipOut.write(PFJcrUtils.toJson(nodeInfo).toByteArray(StandardCharsets.UTF_8))
        node.nodes?.let {
            while (it.hasNext()) {
                writeToZip(it.nextNode(), archiveName, zipOut)
            }
        }
    }

    private fun createZipEntry(archiveName: String, vararg path: String?): ZipEntry {
        return ZipEntry("$archiveName/${path.joinToString(separator = "/") { it ?: "" }}")
    }

    open fun restore(absPath: String, istream: InputStream, securityConfirmation: String, uuidBehavior: Int) {
        if (securityConfirmation != RepoService.RESTORE_SECURITY_CONFIRMATION_IN_KNOW_WHAT_I_M_DOING_REPO_MAY_BE_DESTROYED) {
            throw IllegalArgumentException("You must use the correct security confirmation if you know what you're doing. The repo content may be lost after restoring!")
        }
        return runInSession { session ->
            log.info { "Restoring repository in path '$absPath'..." }
            session.workspace.importXML(absPath, istream, uuidBehavior)
        }
    }

    private fun <T> runInSession(method: (session: Session) -> T): T {
        val session: Session = repoService.login()
        try {
            return method(session)
        } finally {
            session.logout()
        }
    }
}
