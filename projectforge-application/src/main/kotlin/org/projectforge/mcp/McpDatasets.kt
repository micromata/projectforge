/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.mcp

import org.projectforge.common.props.PropUtils
import org.projectforge.framework.jcr.AttachmentsService
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.SortOrder
import org.projectforge.framework.persistence.api.SortProperty
import org.projectforge.rest.AddressEntityRest
import org.projectforge.rest.TimesheetEntityRest
import org.projectforge.rest.core.AbstractEntityRest
import org.projectforge.rest.fibu.CustomerPagesRest
import org.projectforge.rest.fibu.OutgoingInvoiceEntityRest
import org.projectforge.rest.fibu.ProjectEntityRest
import org.projectforge.rest.task.TaskEntityRest
import org.projectforge.ui.LayoutContext
import org.projectforge.ui.filter.LayoutListFilterUtils
import org.projectforge.ui.filter.UIFilterElement
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.http.MediaTypeFactory
import org.springframework.stereotype.Component

/**
 * The data the MCP tools read, through the rest classes of the list and edit pages: the same filter fields, the
 * same access checks (in the DAOs and in the rest classes, e.g. the invoice detail rule) and the same DTOs as the UI.
 * Call it with the logged-in user registered for the thread.
 */
@Component
@ConditionalOnProperty(name = ["projectforge.mcp.enabled"], havingValue = "true")
class McpDatasets(
    projectRest: ProjectEntityRest,
    customerRest: CustomerPagesRest,
    taskRest: TaskEntityRest,
    timesheetRest: TimesheetEntityRest,
    addressRest: AddressEntityRest,
    invoiceRest: OutgoingInvoiceEntityRest,
    private val attachmentsService: AttachmentsService,
) {
    private class Dataset(val rest: AbstractEntityRest<ExtendedBaseDO<Long>, Any, BaseDao<ExtendedBaseDO<Long>>>, val defaultSort: SortProperty)

    private val datasets = linkedMapOf(
        "projects" to Dataset(erased(projectRest), SortProperty("id", SortOrder.DESCENDING)),
        "customers" to Dataset(erased(customerRest), SortProperty("id", SortOrder.DESCENDING)),
        "tasks" to Dataset(erased(taskRest), SortProperty("id", SortOrder.DESCENDING)),
        "timesheets" to Dataset(erased(timesheetRest), SortProperty("startTime", SortOrder.DESCENDING)),
        "addresses" to Dataset(erased(addressRest), SortProperty("id", SortOrder.DESCENDING)),
        "invoices" to Dataset(erased(invoiceRest), SortProperty("datum", SortOrder.DESCENDING)),
    )

    val names: Set<String> get() = datasets.keys

    private fun dataset(entity: String) = datasets[entity] ?: throw IllegalArgumentException("Unknown entity")

    /** The filter fields the list page of [entity] offers, without the [hidden] ones. */
    fun filterElements(entity: String): List<UIFilterElement> {
        val rest = dataset(entity).rest
        return LayoutListFilterUtils.createNamedSearchFilterContainer(rest, LayoutContext(rest.baseDao.doClass)).content
            .filterIsInstance<UIFilterElement>().filterNot { hidden(entity, it.id) }
    }

    /** Whether [field] is a property of the entity's own table, which the database can sort by. */
    fun sortable(entity: String, field: String): Boolean =
        !field.contains('.') && !hidden(entity, field) && PropUtils.getField(dataset(entity).rest.baseDao.doClass, field, true) != null

    fun defaultSort(entity: String): SortProperty = dataset(entity).defaultSort

    /** The list rows for [filter], access checked like the list page. Doesn't change the user's stored filter. */
    fun search(entity: String, filter: MagicFilter): List<Any> {
        val rest = dataset(entity).rest
        return rest.getResultList(filter).map { rest.createListRow(it) }
    }

    /** The entry as the edit page gets it (all fields, access checked), or null if not found. */
    fun get(entity: String, id: Long): Any? = dataset(entity).rest.getItem(id).takeIf { it.statusCode.is2xxSuccessful }?.body

    /** Whether records of [entity] may have attached files. */
    fun hasAttachments(entity: String): Boolean = dataset(entity).rest.jcrPath != null

    /**
     * The file [fileId] attached to the record [id], if the user may read the record as on its edit page ([get]) and
     * download the file as in the UI. Counted as a download of the user, as there. Null if not found; the content is
     * left out (null) if the file is encrypted or larger than [maxSize] bytes.
     */
    fun attachment(entity: String, id: Long, fileId: String, maxSize: Long): AttachmentFile? {
        val rest = dataset(entity).rest
        val jcrPath = rest.jcrPath ?: return null
        get(entity, id) ?: return null
        val info = attachmentsService.getAttachmentInfo(jcrPath, id, fileId, rest.attachmentsAccessChecker) ?: return null
        val name = info.name ?: "file"
        val mimeType = MediaTypeFactory.getMediaType(name).orElse(MediaType.APPLICATION_OCTET_STREAM).toString()
        if (info.encrypted || (info.size ?: 0) > maxSize) return AttachmentFile(name, mimeType, info.size, info.encrypted, null)
        val (_, inputStream) = attachmentsService.getAttachmentInputStream(jcrPath, id, fileId, rest.attachmentsAccessChecker,
            baseDao = rest.baseDao) ?: return null
        val content = inputStream.use { it.readNBytes((maxSize + 1).toInt()) }
        return AttachmentFile(name, mimeType, content.size.toLong(), false, content.takeIf { it.size <= maxSize })
    }

    class AttachmentFile(val name: String, val mimeType: String, val size: Long?, val encrypted: Boolean, val content: ByteArray?)

    companion object {
        /**
         * Fields agents never get, in results and as filters. The private data of addresses stays in ProjectForge,
         * because an agent's answers may end up in services outside of it. Binary data and keys only cost tokens,
         * the other attachment fields repeat what the list `attachments` (reduced to [ATTACHMENT_FIELDS]) says.
         */
        fun hidden(entity: String, field: String): Boolean =
            field in HIDDEN_FIELDS || (entity == "addresses" && (field.startsWith("private") || field == "birthday"))

        /** What an agent learns of an attached file. */
        val ATTACHMENT_FIELDS = listOf("fileId", "name", "size", "description", "created", "createdByUser",
            "lastUpdate", "lastUpdateByUser", "encrypted")

        private val HIDDEN_FIELDS = setOf("attachmentsSize", "attachmentsNames", "attachmentsIds", "attachmentsLastUserAction",
            "imageData", "imageDataPreview", "imageDataUrl", "hasUploadedImage", "imageDeleted", "hasImage", "publicKey", "fingerprint", "uiStatusAsXml", "writeAccess", "deleteAccess")

        @Suppress("UNCHECKED_CAST")
        private fun erased(rest: AbstractEntityRest<*, *, *>) =
            rest as AbstractEntityRest<ExtendedBaseDO<Long>, Any, BaseDao<ExtendedBaseDO<Long>>>
    }
}
