/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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
import org.apache.commons.io.FilenameUtils
import org.projectforge.common.FormatterUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.jcr.Binary
import javax.jcr.Node


private val log = KotlinLogging.logger {}

@Service
open class RepoBackupService {
  @Value("\${projectforge.jcr.backupDir}")
  private val jcrBackupDir: String? = null

  @Autowired
  internal lateinit var repoService: RepoService

  @Autowired
  internal lateinit var jcrCheckSanityJob: JCRCheckSanityJob


  internal val listOfIgnoredNodePaths = mutableListOf<String>()

  /**
   * These node pathes will be ignored by backup job. Data transfer files are ignored (plugin datatransfer).
   */
  fun registerNodePathToIgnore(nodePath: String) {
    log.info { "Adding path '$nodePath' as path to ignore for backup service." }
    listOfIgnoredNodePaths.add(nodePath)
  }

  /**
   * Where to put the nigthly backups of the jcr as zip files?
   */
  var backupDirectory: File? = null
    private set

  /**
   * Must initially be called for setting backup directory.
   * @param defaultBackupDir is used as default backup dir if [jcrBackupDir] is not given or doesn't exists in the filesystem.
   */
  fun initBackupDir(defaultBackupDir: File) {
    var file: File? = null
    if (!jcrBackupDir.isNullOrBlank()) {
      file = File(jcrBackupDir)
      if (!file.exists() && !file.isDirectory) {
        log.error { "Can't use '$jcrBackupDir' as JCR backup directory, it isn't an existing directory." }
        file = null
      }
    }
    if (file == null) {
      file = defaultBackupDir
    }
    this.backupDirectory = file
    log.info { "Using '${file.absolutePath}' as JCR backup directory." }
  }

  /**
   * @param absPath If not given, [RepoService.mainNodeName] is used.
   */
  open fun backupAsZipArchive(
    archiveName: String,
    zipOut: ZipOutputStream,
    absPath: String = "/${repoService.mainNodeName}"
  ) {
    val archivNameWithoutExtension = if (archiveName.contains('.')) {
      archiveName.substring(0, archiveName.indexOf('.'))
    } else {
      archiveName
    }
    return repoService.runInSession { session ->
      log.info { "Creating backup of document view and binaries of path '$absPath' as '$archiveName'..." }

      // Write README.TXT
      zipOut.putNextEntry(createZipEntry(archivNameWithoutExtension, "README.TXT"))
      val readme = this::class.java.getResource(BACKUP_README).readText()
      zipOut.write(readme.toByteArray(StandardCharsets.UTF_8))

      val walker = object : RepoTreeWalker(repoService, absPath) {
        override fun visit(node: Node, isRootNode: Boolean) {
          if (isRootNode) {
            // Using repository.json if repository.xml doesn't work.
            zipOut.putNextEntry(createZipEntry(archivNameWithoutExtension, "repository.json"))
            zipOut.write(
              PFJcrUtils.toJson(NodeInfo(node, recursive = true, listOfIgnoredNodePaths = listOfIgnoredNodePaths))
                .toByteArray(StandardCharsets.UTF_8)
            )
          }

          if (PFJcrUtils.matchAnyPath(node, listOfIgnoredNodePaths)) {
            // Ignore node.
            log.warn { "Ignore path=${node.path} as configured." }
            return
          }
          val fileList = repoService.getFileInfos(node)
          if (!fileList.isNullOrEmpty()) {
            zipOut.putNextEntry(createZipEntry(archivNameWithoutExtension, node.path, "files.json"))
            zipOut.write(PFJcrUtils.toJson(FileObjectList(fileList)).toByteArray(StandardCharsets.UTF_8))
            zipOut.putNextEntry(createZipEntry(archivNameWithoutExtension, node.path, "files.txt"))
            val fileListAsString =
              fileList.joinToString(separator = "\n") {
                "${PFJcrUtils.createSafeFilename(it)} ${
                  FormatterUtils.formatBytes(
                    it.size
                  )
                } ${it.fileName}"
              }
            zipOut.write(fileListAsString.toByteArray(StandardCharsets.UTF_8))
          }
          val nodeInfo = NodeInfo(node, false)
          zipOut.putNextEntry(createZipEntry(archivNameWithoutExtension, node.path, "node.json"))
          zipOut.write(PFJcrUtils.toJson(nodeInfo).toByteArray(StandardCharsets.UTF_8))
        }

        override fun visitFile(fileNode: Node, fileObject: FileObject) {
          val content = repoService.getFileContent(fileNode, fileObject)
          if (content != null) {
            val fileName = PFJcrUtils.createSafeFilename(fileObject)
            zipOut.putNextEntry(createZipEntry(archivNameWithoutExtension, fileNode.path, fileName))
            zipOut.write(content)
          }
        }
      }
      walker.walk()
      log.info {
        "Backup of document view and binaries of path '$absPath' as '$archiveName' done: number of nodes=${
          FormatterUtils.format(
            walker.numberOfVisitedNodes
          )
        }, number of files=${FormatterUtils.format(walker.numberOfVisitedFiles)}."
      }
    }
  }

  /**
   * @param absPath If not given, [RepoService.mainNodeName] is used (only used for creation of repository.xml).
   */
  @JvmOverloads
  open fun restoreBackupFromZipArchive(
    zipIn: ZipInputStream,
    securityConfirmation: String,
    absPath: String = "/${repoService.mainNodeName}"
  ): JCRCheckSanityJob.CheckResult {
    if (securityConfirmation != RESTORE_SECURITY_CONFIRMATION__I_KNOW_WHAT_I_M_DOING__REPO_MAY_BE_DESTROYED) {
      throw IllegalArgumentException("You must use the correct security confirmation if you know what you're doing. The repo content may be lost after restoring!")
    }
    repoService.runInSession { session ->
      log.info { "Restoring backup of document view and binaries of path '$absPath'..." }
      var nodesRestored = false
      var zipEntry = zipIn.nextEntry
      while (zipEntry != null) {
        if (zipEntry.isDirectory) {
          zipEntry = zipIn.nextEntry
          continue
        }
        val fileName = FilenameUtils.getName(zipEntry.name)
        if (!nodesRestored) {
          if (fileName == "repository.json") {
            log.info { "Restoring nodes from '${zipEntry.name}'..." }
            val json = zipIn.readBytes().toString(StandardCharsets.UTF_8)
            val topNode = PFJcrUtils.fromJson(json, NodeInfo::class.java)
            restoreNode(session.rootNode, topNode)
            session.save()
            nodesRestored = true
            zipEntry = zipIn.nextEntry
            continue
          }
        }
        val filesPath = getFilesPath(zipEntry.name)
        if (!filesPath.isNullOrBlank() && !IGNORE_FILES.contains(fileName)) {
          if (log.isDebugEnabled) {
            log.debug { "Restoring file content (binary) '${zipEntry.name}', $fileName..." }
          }
          val filesNode = repoService.getNodeOrNull(session, filesPath)
          if (filesNode == null) {
            log.error { "Can't determine node '$filesNode'. Can't restore binary '${zipEntry.name}'." }
            zipEntry = zipIn.nextEntry
            continue
          }
          val fileNode = repoService.findFile(filesNode, FilenameUtils.getBaseName(zipEntry.name))
          if (fileNode == null) {
            log.error { "Can't determine node '$fileNode'. Can't restore binary '${zipEntry.name}'." }
            zipEntry = zipIn.nextEntry
            continue
          }
          if (!nodesRestored) {
            throw IllegalArgumentException("Sorry, can't restore binaries. repository.xml must be read first (placed before restoring binaries in zip file)!")
          }
          val fileObject = FileObject(fileNode)
          log.info { "Restoring file '${zipEntry.name}': $fileObject" }
          val content = zipIn.readBytes()
          val inputStream = ByteArrayInputStream(content)
          val bin: Binary = session.valueFactory.createBinary(inputStream)
          fileNode.setProperty(RepoService.PROPERTY_FILECONTENT, session.valueFactory.createValue(bin))
          session.save()
        }
        zipEntry = zipIn.nextEntry
      }
      zipIn.closeEntry()
    }
    return jcrCheckSanityJob.execute()
  }

  private fun restoreNode(parentNode: Node, nodeInfo: NodeInfo) {
    val node = repoService.ensureNode(parentNode, nodeInfo.name)
    nodeInfo.properties?.forEach {
      it.addToNode(node)
    }
    nodeInfo.children?.forEach {
      restoreNode(node, it)
    }
  }

  private fun getFilesPath(fileName: String): String? {
    if (!fileName.contains(RepoService.NODENAME_FILES)) {
      return null
    }
    var archiveName = fileName.substring(fileName.indexOf('/'))
    if (archiveName.startsWith("//")) {
      archiveName = archiveName.substring(1)
    }
    archiveName = archiveName.substring(0, archiveName.indexOf(RepoService.NODENAME_FILES) - 1)
    return "$archiveName/${RepoService.NODENAME_FILES}"
  }

  private fun createZipEntry(archiveName: String, vararg path: String?): ZipEntry {
    return ZipEntry("$archiveName/${path.joinToString(separator = "/") { it ?: "" }}")
  }

  companion object {
    const val RESTORE_SECURITY_CONFIRMATION__I_KNOW_WHAT_I_M_DOING__REPO_MAY_BE_DESTROYED =
      "Yes, I want to restore the repo and know what I'm doing. The repo may be lost."

    internal const val BACKUP_README = "/backupReadme.txt"

    private val IGNORE_FILES = arrayOf("README.txt", "node.json", "files.txt", "files.json")

    val backupFilename: String
      get() {
        val nowAsIsoString =
          ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").withZone(ZoneOffset.UTC))
        return "$backupFilenamePrefix$nowAsIsoString.zip"
      }

    val backupFilenamePrefix = "projectforge-jcr-backup-"
  }
}
