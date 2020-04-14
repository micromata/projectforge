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

package org.projectforge.rest

import mu.KotlinLogging
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.jcr.Attachment
import org.projectforge.framework.jcr.AttachmentsService
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.core.RestException
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

/**
 * Modal dialog showing details of an attachment with the functionality to download, modify and delete it.
 */
@RestController
@RequestMapping("${Rest.URL}/attachment")
class AttachmentPageRest : AbstractDynamicPageRest() {
    @Autowired
    private lateinit var attachmentsService: AttachmentsService

    /**
     * The react path of this should look like: 'react/attachment/dynamic/42?category=contract...'
     * @param id: Id of data object with attachments.
     */
    @GetMapping("dynamic")
    fun getForm(@RequestParam("id", required = true) id: Int,
                @RequestParam("category", required = true) category: String,
                @RequestParam("fileId", required = true) fileId: String,
                @RequestParam("listId") listId: String?,
                request: HttpServletRequest): FormLayoutData? {
        log.info { "User tries to edit/view details of attachment: category='$category', id='$id', listId='$listId', fileId='$fileId', page='${this::class.java.name}'." }
        val pagesRest = PagesResolver.getPagesRest(category)
                ?: throw UnsupportedOperationException("PagesRest class for category '$category' not known (registerd).")
        pagesRest.checkJcrActivity(listId)
        pagesRest.baseDao.getById(id)
                ?: throw RestException("Entity with id $id not accessible for category '$category' or doesn't exist.", "User without access or id unknown.")
        val data = attachmentsService.getAttachmentInfo(pagesRest.jcrPath!!, id, fileId, listId)
                ?: throw RestException("Attachment '$fileId' for object with id $id not found for category '$category' and list '$listId'.", "Attachment not found.")
        val layout = UILayout("attachment")

        val lc = LayoutContext(Attachment::class.java)

        layout
                .add(lc, "name")
                .add(UITextArea("description", lc))
                .add(UIRow()
                        .add(UICol(UILength(md = 6))
                                .add(UIReadOnlyField("sizeHumanReadable", label = "attachment.fileSize")))
                        .add(UICol(UILength(md = 6))
                                .add(UIReadOnlyField("fileId", label = "attachment.fileId"))))
                .add(UIRow()
                        .add(UICol(UILength(md = 6))
                                .add(UIReadOnlyField("createdFormatted", label = "created"))
                                .add(UIReadOnlyField("createdByUser", label = "createdBy")))
                        .add(UICol(UILength(md = 6))
                                .add(UIReadOnlyField("lastUpdateFormatted", label = "modified"))
                                .add(UIReadOnlyField("lastUpdateByUser", label = "modifiedBy"))))

                .addAction(UIButton("delete",
                        translate("delete"),
                        UIColor.DANGER,
                        confirmMessage = translate("file.panel.deleteExistingFile.heading"),
                        responseAction = ResponseAction(RestResolver.getRestUrl(this::class.java), targetType = TargetType.POST),
                        default = true))
                .addAction(UIButton("update",
                        translate("update"),
                        UIColor.SUCCESS,
                        responseAction = ResponseAction(RestResolver.getRestUrl(this::class.java), targetType = TargetType.POST),
                        default = true)
                )
        LayoutUtils.process(layout)

        return FormLayoutData(data, layout, createServerData(request))
    }
}
