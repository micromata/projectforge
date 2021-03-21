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

package org.projectforge.plugins.datatransfer.restPublic

import mu.KotlinLogging
import org.projectforge.rest.config.Rest
import org.springframework.core.io.InputStreamResource
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

private val log = KotlinLogging.logger {}

/**
 * For external anonymous usage via token/password.
 */
@RestController
@RequestMapping("${Rest.PUBLIC_URL}/datatransfer")
class DataTransferPublicServicesRest {
  @GetMapping("download/{category}/{id}")
  fun download(
    @PathVariable("category", required = true) category: String,
    @PathVariable("id", required = true) id: Int,
    @RequestParam("fileId", required = true) fileId: String,
    @RequestParam("listId") listId: String?,
    @RequestParam("accessString") accessString: Any?
  )
      : ResponseEntity<InputStreamResource>? {
    check(category == "datatransfer")
    throw NotImplementedError()
    /*log.info { "User tries to download attachment: ${paramsToString(category, id, fileId, listId)}." }
    val pagesRest = getPagesRest(category, listId)

    val result =
      attachmentsService.getAttachmentInputStream(pagesRest.jcrPath!!, id, fileId, pagesRest.attachmentsAccessChecker)
        ?: throw TechnicalException(
          "File to download not accessible for user or not found: ${
            paramsToString(
              category,
              id,
              fileId,
              listId
            )
          }."
        )

    val filename = result.first.fileName ?: "file"
    val inputStream = result.second
    return ResponseEntity.ok()
      .contentType(MediaType.parseMediaType("application/octet-stream"))
      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${filename.replace('"', '_')}\"")
      .body(InputStreamResource(inputStream))*/
  }
}
