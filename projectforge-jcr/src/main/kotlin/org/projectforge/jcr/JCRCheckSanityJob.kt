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
import org.projectforge.common.FormatterUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import javax.jcr.Node

private val log = KotlinLogging.logger {}

@Component
open class JCRCheckSanityJob {
  @Autowired
  internal lateinit var repoService: RepoService

  class CheckResult(
    val errors: List<String>,
    val warnings: List<String>,
    val numberOfVisitedFiles: Int,
    val numberOfVisitedNodes: Int
  ) {
    fun toText(): String {
      val sb = StringBuilder()
      sb.appendLine("Errors").appendLine("------")
      errors.forEach { sb.appendLine("  *** $it") }
      sb.appendLine("Warnings").appendLine("--------")
      warnings.forEach { sb.appendLine("  $it") }
      sb.appendLine()
      sb.appendLine("Number of visited nodes: ${FormatterUtils.format(numberOfVisitedNodes)}")
      sb.appendLine("Number of visited files: ${FormatterUtils.format(numberOfVisitedFiles)}")
      return sb.toString()
    }
  }

  // For testing: @Scheduled(fixedDelay = 3600 * 1000, initialDelay = 10 * 1000)
  // projectforge.jcr.cron.backup=0 30 0 * * *
  @Scheduled(cron = "\${projectforge.jcr.cron.sanityCheck}")
  open fun execute(): CheckResult {
    log.info("JCR sanity check job started.")
    val errors = mutableListOf<String>()
    val warnings = mutableListOf<String>()
    val walker = object : RepoTreeWalker(repoService) {
      override fun visitFile(fileNode: Node, fileObject: FileObject) {
        fileObject.checksum.let { repoChecksum ->
          if (repoChecksum != null && repoChecksum.length > 10) {
            log.info { "Checking checksum of file '${fileObject.fileName}' (${FormatterUtils.formatBytes(fileObject.size)})..." }
            val checksum =
              repoService.getFileInputStream(fileNode, fileObject, true, useEncryptedFile = true)
                .use { istream -> RepoService.checksum(istream) }
            if (!validateChecksum(checksum, repoChecksum)) {
              val msg =
                "Checksum of file '${fileObject.fileName}' from repository '${normalizeChecksum(checksum)}' differs from repository value '${
                  normalizeChecksum(
                    repoChecksum
                  )
                }'! ['${fileNode.path}']"
              errors.add(msg)
              log.error { msg }
            }
          } else {
            val msg =
              "Checksum of file '${fileObject.fileName}' from repository not given (skipping checksum check). ['${fileNode.path}']"
            warnings.add(msg)
            log.info { msg }
          }
        }
        if (fileObject.fileExtension == "zip") {
          // Check mode of zip files (encryption).
          if (fileObject.zipMode == null) {
            val newZipMode = repoService.getFileInputStream(fileNode, fileObject, true, useEncryptedFile = true)
              .use { istream -> ZipUtils.determineZipMode(istream) }
            if (newZipMode != null) {
              fileNode.setProperty(RepoService.PROPERTY_ZIP_MODE, newZipMode.name)
            }
          }
        }
        fileObject.size.let { repoSize ->
          if (repoSize == null) {
            val msg =
              "Size of file '${fileObject.fileName}' from repository not given (skipping file size check). ['${fileNode.path}']"
            warnings.add(msg)
            log.info { msg }
          } else {
            val fileSize = repoService.getFileSize(fileNode, fileObject, true)
            if (fileSize != repoSize) {
              val msg =
                "Size of file from repository '${fileNode.path}': '${fileObject.fileName}'=${
                  FormatterUtils.format(
                    fileSize
                  )
                } differs from reposity value ${FormatterUtils.format(repoSize)}!"
              errors.add(msg)
              log.error { msg }
            }
          }
        }
      }
    }
    walker.walk()
    log.info { "JCR sanity check job finished. ${walker.numberOfVisitedFiles} Files checked with ${warnings.size} warnings and ${errors.size} errors." }
    return CheckResult(errors, warnings, walker.numberOfVisitedFiles, walker.numberOfVisitedNodes)
  }

  private fun validateChecksum(checksum1: String, checksum2: String): Boolean {
    val c1 = normalizeChecksum(checksum1)
    val c2 = normalizeChecksum(checksum2)
    return c1 == c2
  }

  private fun normalizeChecksum(checksum: String): String {
    return subString(subString(checksum, '='), ' ')
  }

  private fun subString(checksum: String, ch: Char): String {
    val idx = checksum.indexOf(ch)
    return if (idx > 0 && checksum.length > idx) {
      checksum.substring(idx + 1)
    } else {
      checksum
    }
  }
}
