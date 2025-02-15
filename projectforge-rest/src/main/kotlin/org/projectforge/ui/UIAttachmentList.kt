/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.ui

import org.projectforge.common.FormatterUtils
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.jcr.AttachmentsService

/**
 * List of attachments including upload, download and remove functionality.
 * See DynamicAttachmentList.jsx for usage.
 */
@Suppress("unused")
class UIAttachmentList(
  /**
   * Simply use [org.projectforge.rest.core.AbstractPagesRest.category] if using in pages rest. Otherwise
   * the category/jcrPath is required here.
   */
  var category: String,
  /**
   * The id of the object (pk) where the attachments are belonging to.
   * Null for new objects, meaning, that now upload is available and an hint is shown.
   */
  val id: Long?,
  /**
   * id to identify attachments list by server, especially if multiple lists of attachments are
   * used for one page.
   * Default is 'attachments'.
   */
  val listId: String = AttachmentsService.DEFAULT_NODE,
  /**
   * The maximum size of the uploadable files will be also handled by the client before uploading.
   */
  val maxSizeInKB: Int,
  /**
   * If true, only download of attachments is allowed.
   */
  val readOnly: Boolean = false,
  /**
   * Used by data transfer tool for using public react pages.
   */
  val serviceBaseUrl: String? = null,
  /**
   * Used by data transfer tool for using public rest api.
   */
  val restBaseUrl: String? = null,
  /**
   * Used by data transfer tool for using public rest api and react pages (for serving access token and password by the client).
   */
  val downloadOnRowClick: Boolean? = null,
  /**
   * If true, the upload function isn't available.
   */
  val uploadDisabled: Boolean? = null,
  /**
   * If true, the expiry info of the attachments will be displayed (if given in [org.projectforge.framework.jcr.Attachment.info] as string value with key 'expiryInfo'.
   */
  val showExpiryInfo: Boolean? = null,
  /**
   * If true, the createdBy- and lastUpdateBy-user is displayed (default). Should be false for public usage (e. g.
   * external DataTransfer usage.)
   */
  showUserInfo: Boolean = true,
) :
  UIElement(type = UIElementType.ATTACHMENT_LIST) {
  /**
   * For displaying the attachments as AG Grid.
   */
  var agGrid: UIAgGrid? = null

  init {
    UIAgGrid("attachments").let {
      this.agGrid = it
      it.add(
        UIAgGridColumnDef(
          "name",
          translate("attachment.fileName"),
          sortable = true,
          width = 300,
          cellRenderer = "filename",
          filter = true,
          wrapText = true,
        )
      )
        .add(
          UIAgGridColumnDef(
            "action",
            "",
            cellRenderer = "action",
            width = 50,
            filter = true,
          )
        )
        .add(
          UIAgGridColumnDef(
            "size",
            translate("attachment.size"),
            sortable = true,
            valueFormatter = "data.sizeHumanReadable",
            width = 80,
          )
        )
        .add(
          UIAgGridColumnDef(
            "description",
            translate("description"),
            sortable = true,
            width = UIAgGridColumnDef.DESCRIPTION_WIDTH,
            filter = true,
            wrapText = true,
          )
        )
      if (showExpiryInfo == true) {
        it.add(
          UIAgGridColumnDef(
            "info.expiryInfo",
            translate("attachment.expires"),
            width = 120,
          )
        )
      }
      it.add(
        UIAgGridColumnDef(
          "created",
          translate("created"),
          sortable = true,
          valueFormatter = "data.createdFormatted",
          width = UIAgGridColumnDef.TIMESTAMP_WIDTH,
        )
      )
      if (showUserInfo) {
        it.add(
          UIAgGridColumnDef(
            "createdByUser",
            translate("createdBy"),
            sortable = true,
            width = UIAgGridColumnDef.USER_WIDTH,
            filter = true,
          )
        )
      }
      it.add(
        UIAgGridColumnDef(
          "lastUpdate",
          translate("modified"),
          sortable = true,
          valueFormatter = "data.lastUpdateTimeAgo",
          width = UIAgGridColumnDef.DATE_WIDTH
        ),
      )
      if (showUserInfo) {
        it.add(
          UIAgGridColumnDef(
            "lastUpdateByUser",
            translate("modifiedBy"),
            sortable = true,
            width = UIAgGridColumnDef.USER_WIDTH,
            filter = true,
          )
        )
      }
    }
  }

  fun addTranslations(layout: UILayout) {
    if (layout.translations.containsKey("attachment.fileName")) {
      // Translations already added.
      return
    }
    layout.addTranslations(
      "attachment.expires",
      "attachment.fileName",
      "attachment.onlyAvailableAfterSave",
      "attachment.size",
      "attachment.upload.title",
      "cancel", "yes", // For delete confirmation dialogue
      "created",
      "createdBy",
      "delete",
      "description",
      "file.upload.deleteSelected",
      "file.upload.deleteSelected.confirm",
      "file.upload.downloadSelected",
      "file.upload.error.fileAlreadyExists",
      "file.upload.error.toManyFiles",
      "modified",
      "modifiedBy",
      "multiselection.aggrid.selection.info.message",
      "multiselection.aggrid.selection.info.title",
      "reload",
    )
    layout.addTranslation(
      "file.upload.error.maxSizeOfExceeded",
      translateMsg("file.upload.error.maxSizeOfExceeded", FormatterUtils.formatBytes(maxSizeInKB * 1024))
    )
  }
}
