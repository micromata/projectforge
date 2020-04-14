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
import org.projectforge.Const
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.jcr.Attachment
import org.projectforge.framework.jcr.AttachmentsService
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.*
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
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

    class AttachmentPageData(var category: String,
                             var id: Int,
                             var fileId: String,
                             var listId: String? = null) {
        lateinit var attachment: Attachment
    }

    @PostMapping
    fun save(request: HttpServletRequest, @RequestBody postData: PostData<AttachmentPageData>)
            : ResponseEntity<ResponseAction>? {
        validateCsrfToken(request, postData)?.let { return it }
        val data = postData.data
        val attachment = data.attachment
        val pagesRest = getPagesRest(data.category, data.listId)
        log.warn("*************** Do check access by pagesRest *************")
        getAttachment(pagesRest, data) // Check attachment availability
        val obj = getDataObject(pagesRest, data.id) // Check data object availability.

        attachmentsService.changeFileInfo(pagesRest.jcrPath!!, data.fileId, pagesRest.baseDao, obj, attachment.name, attachment.description, data.listId)
        //attachmentsService.getAttachmentInfo(pages)
        return ResponseEntity(ResponseAction("/${Const.REACT_APP_PATH}calendar"), HttpStatus.OK)
    }

    @PostMapping("delete")
    fun delete(request: HttpServletRequest, @RequestBody postData: PostData<AttachmentPageData>)
            : ResponseEntity<ResponseAction>? {
        validateCsrfToken(request, postData)?.let { return it }
        log.warn("*************** Do check access by pagesRest *************")
        return ResponseEntity(ResponseAction("/${Const.REACT_APP_PATH}calendar"), HttpStatus.OK)
    }

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
        log.warn("*************** Do check access by pagesRest *************")
        val pagesRest = getPagesRest(category, listId)
        getDataObject(pagesRest, id) // Check data object availability.
        val data = AttachmentPageData(category = category, id = id, fileId = fileId, listId = listId)
        data.attachment = getAttachment(pagesRest, data)
        val layout = UILayout("attachment")

        val lc = LayoutContext(Attachment::class.java)

        layout
                .add(lc, "attachment.name")
                .add(UITextArea("attachment.description", lc))
                .add(UIRow()
                        .add(UICol(UILength(md = 6))
                                .add(UIReadOnlyField("attachment.sizeHumanReadable", label = "attachment.fileSize")))
                        .add(UICol(UILength(md = 6))
                                .add(UIReadOnlyField("attachment.fileId", label = "attachment.fileId"))))
                .add(UIRow()
                        .add(UICol(UILength(md = 6))
                                .add(UIReadOnlyField("attachment.createdFormatted", label = "created"))
                                .add(UIReadOnlyField("attachment.createdByUser", label = "createdBy")))
                        .add(UICol(UILength(md = 6))
                                .add(UIReadOnlyField("attachment.lastUpdateFormatted", label = "modified"))
                                .add(UIReadOnlyField("attachment.lastUpdateByUser", label = "modifiedBy"))))

                .addAction(UIButton("download",
                        translate("download"),
                        UIColor.LINK,
                        responseAction = ResponseAction(RestResolver.getRestUrl(this::class.java, "download"), targetType = TargetType.POST),
                        default = true))
                .addAction(UIButton("delete",
                        translate("delete"),
                        UIColor.DANGER,
                        confirmMessage = translate("file.panel.deleteExistingFile.heading"),
                        responseAction = ResponseAction(RestResolver.getRestUrl(this::class.java, "delete"), targetType = TargetType.POST),
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

    private fun getPagesRest(category: String, listId: String?): AbstractPagesRest<out ExtendedBaseDO<Int>, *, out BaseDao<*>> {
        val pagesRest = PagesResolver.getPagesRest(category)
                ?: throw UnsupportedOperationException("PagesRest class for category '$category' not known (registered).")
        pagesRest.checkJcrActivity(listId)
        return pagesRest
    }

    private fun getAttachment(pagesRest: AbstractPagesRest<*, *, *>, data: AttachmentPageData): Attachment {
        return attachmentsService.getAttachmentInfo(pagesRest.jcrPath!!, data.id, data.fileId, data.listId)
                ?: throw RestException("Attachment '$data.fileId' for object with id $data.id not found for category '$data.category' and list '$data.listId'.", "Attachment not found.")
    }

    private fun getDataObject(pagesRest: AbstractPagesRest<*, *, *>, id: Int): ExtendedBaseDO<Int> {
        return pagesRest.baseDao.getById(id)
                ?: throw RestException("Entity with id $id not accessible for category '$pagesRest.category' or doesn't exist.", "User without access or id unknown.")

    }
}
