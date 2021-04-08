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
class JCRCheckSanityJob {
  @Autowired
  private lateinit var repoService: RepoService

  // projectforge.jcr.cron.backup=2 30 0 * * *
  @Scheduled(cron = "\${projectforge.jcr.cron.sanityCheck}")
  fun execute() {
    log.info("JCR sanity check job started.")
    val errors = mutableListOf<String>()
    val warnings = mutableListOf<String>()
    val walker = object : RepoTreeWalker(repoService) {
      override fun visitFile(fileNode: Node, fileObject: FileObject) {
        fileObject.checksum.let { repoChecksum ->
          if (repoChecksum != null && repoChecksum.length > 10) {
            val checksum =
              repoService.getFileInputStream(fileNode, fileObject, true)
                .use { istream -> RepoService.checksum(istream) }
            if (!validateChecksum(checksum, repoChecksum)) {
              val msg =
                "Checksum of file '${fileObject.fileName}' from repository '${normalizeChecksum(checksum)}' differs from repository value '${normalizeChecksum(repoChecksum)}'! ['${fileNode.path}']"
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
