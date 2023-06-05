/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateIfKey
import org.projectforge.rest.config.RestUtils

class UIButton
private constructor(
  val id: String,
  /** May be null for standard buttons. For standard buttons the title will be set dependent on the id. */
  var title: String? = null,
  val color: UIColor? = null,
  var outline: Boolean? = null,
  /**
   * There should be one default button in every form, used if the user hits return.
   */
  val default: Boolean? = null,
  /**
   * Tell the client of what to do after clicking this button.
   */
  var responseAction: ResponseAction? = null,
  /**
   * If given the frontend should display a confirmation dialog containing this message.
   */
  var tooltip: String? = null,
  var confirmMessage: String? = null,
  val disabled: Boolean? = false,
) : UIElement(UIElementType.BUTTON) {
  /**
   * Sets a redirect as responseAction.
   * @return this for chaining.
   * @see RestUtils.getRedirectToDefaultPageAction
   */
  fun redirectToDefaultPage(): UIButton {
    responseAction = RestUtils.getRedirectToDefaultPageAction()
    return this
  }

  /**
   * @param layout Needed to add yes/cancel translation.
   * @param confirmMessage Will be translated, if not starting with "'"
   * @return this for chaining.
   */
  fun withConfirmMessage(layout: UILayout, confirmMessage: String?): UIButton {
    this.confirmMessage = translateIfKey(confirmMessage)
    if (confirmMessage != null) {
      layout.addTranslations("yes", "cancel")
    }
    return this
  }

  companion object {
    /**
     * @param responseAction If not given, return to default page is used as response action.
     */
    fun createBackButton(
      responseAction: ResponseAction? = null,
      id: String = "back",
      title: String = id,
      default: Boolean? = null
    ): UIButton {
      val result = UIButton(
        id,
        title = translate(title),
        color = UIColor.SECONDARY,
        responseAction = responseAction,
        outline = true,
        default = default,
      )
      if (responseAction == null) {
        result.redirectToDefaultPage()
      }
      return result
    }

    fun createResetButton(responseAction: ResponseAction, default: Boolean? = null): UIButton {
      return UIButton(
        "reset",
        title = translate("reset"),
        color = UIColor.SECONDARY,
        outline = true,
        responseAction = responseAction,
        default = default,
      )
    }

    /**
     * @param title id is used as default. Will be translated, if not starting with "'"
     * @param tooltip null default. Will be translated, if not starting with "'"
     */
    fun createDangerButton(
      id: String,
      responseAction: ResponseAction,
      title: String = id,
      tooltip: String? = null
    ): UIButton {
      return UIButton(
        id,
        translateIfKey(title),
        UIColor.DANGER,
        responseAction = responseAction,
        outline = true,
        tooltip = translateIfKey(tooltip)
      )
    }

    /**
     * @param layout Needed to add yes/cancel translation.
     * @param title id is used as default. Will be translated, if not starting with "'"
     * @param tooltip null default. Will be translated, if not starting with "'"
     * @param confirmMessage "question.markAsDeletedQuestion" as default. Will be translated, if not starting with "'"
     */
    fun createDangerButton(
      layout: UILayout,
      id: String,
      responseAction: ResponseAction,
      title: String = id,
      tooltip: String? = null,
      confirmMessage: String? = null,
    ): UIButton {
      return createDangerButton(id, title = title, tooltip = tooltip, responseAction = responseAction)
        .withConfirmMessage(layout, confirmMessage)
    }


    /**
     * @param responseAction If not given, return to default page is used as response action.
     */
    fun createCancelButton(responseAction: ResponseAction? = null, title: String = translate("cancel")): UIButton {
      val result = UIButton(
        "cancel",
        title = title,
        color = UIColor.DANGER,
        responseAction = responseAction,
        outline = true,
      )
      if (responseAction == null) {
        result.redirectToDefaultPage()
      }
      return result
    }

    /**
     * @param title id is used as default. Will be translated, if not starting with "'"
     * @param tooltip null default. Will be translated, if not starting with "'"
     */
    fun createDefaultButton(
      id: String,
      responseAction: ResponseAction,
      title: String = id,
      tooltip: String? = null,
      default: Boolean? = true,
      confirmMessage: String? = null,
      ): UIButton {
      return UIButton(
        id,
        translateIfKey(title),
        UIColor.SUCCESS,
        responseAction = responseAction,
        outline = true,
        default = default,
        tooltip = translateIfKey(tooltip),
        confirmMessage = confirmMessage,
      )
    }

    fun createUpdateButton(responseAction: ResponseAction, default: Boolean? = true): UIButton {
      return createDefaultButton("update", responseAction = responseAction, default = default)
    }

    fun createAddButton(responseAction: ResponseAction, default: Boolean? = true): UIButton {
      return createDefaultButton("add", responseAction = responseAction, default = default)
    }

    fun createSaveButton(responseAction: ResponseAction, default: Boolean? = true): UIButton {
      return createDefaultButton("update", responseAction = responseAction, default = default)
    }

    fun createCreateButton(responseAction: ResponseAction, default: Boolean? = true): UIButton {
      return createDefaultButton("create", responseAction = responseAction, default = default)
    }

    fun createSearchButton(responseAction: ResponseAction, default: Boolean? = null): UIButton {
      return UIButton(
        "search",
        translate("search"),
        UIColor.PRIMARY,
        responseAction = responseAction,
        outline = true,
        default = default,
      )
    }

    fun createUndeleteButton(responseAction: ResponseAction): UIButton {
      return UIButton(
        "undelete",
        color = UIColor.PRIMARY,
        responseAction = responseAction,
        outline = true,
      )
    }

    /**
     * @param layout Needed to add yes/cancel translation.
     * @param confirmMessage "question.markAsDeletedQuestion" as default. Will be translated, if not starting with "'"
     */
    fun createMarkAsDeletedButton(
      layout: UILayout,
      responseAction: ResponseAction,
      confirmMessage: String? = "question.markAsDeletedQuestion",
    ): UIButton {
      return UIButton(
        "markAsDeleted",
        color = UIColor.WARNING,
        responseAction = responseAction,
        outline = true,
      ).withConfirmMessage(layout, confirmMessage)
    }

    /**
     * @param layout Needed to add yes/cancel translation.
     * @param confirmMessage "question.deleteQuestion" as default. Will be translated, if not starting with "'"
     */
    fun createDeleteButton(
      layout: UILayout,
      responseAction: ResponseAction,
      confirmMessage: String? = "question.deleteQuestion"
    ): UIButton {
      return UIButton(
        "deleteIt",
        color = UIColor.WARNING,
        responseAction = responseAction,
        outline = true,
      ).withConfirmMessage(layout, confirmMessage)
    }

    /**
     * @param layout Needed to add yes/cancel translation.
     * @param confirmMessage "question.forceDeleteQuestion" as default. Will be translated, if not starting with "'"
     */
    fun createForceDeleteButton(
      layout: UILayout,
      responseAction: ResponseAction,
      confirmMessage: String? = "question.forceDeleteQuestion",
    ): UIButton {
      return UIButton(
        "forceDelete",
        color = UIColor.WARNING,
        responseAction = responseAction,
        outline = true,
      ).withConfirmMessage(layout, confirmMessage)
    }


    /**
     * @param title id is used as default. Will be translated, if not starting with "'"
     * @param tooltip null default. Will be translated, if not starting with "'"
     */
    fun createPrimaryButton(
      id: String,
      responseAction: ResponseAction,
      title: String? = id,
      tooltip: String? = null,
    ): UIButton {
      return UIButton(
        id,
        title = translateIfKey(title),
        tooltip = translateIfKey(tooltip),
        color = UIColor.PRIMARY,
        responseAction = responseAction,
        outline = true,
      )
    }

    /**
     * @param title id is used as default. Will be translated, if not starting with "'"
     * @param tooltip null default. Will be translated, if not starting with "'"
     */
    fun createSecondaryButton(
      id: String,
      responseAction: ResponseAction,
      title: String? = id,
      tooltip: String? = null,
    ): UIButton {
      return UIButton(
        id,
        title = translateIfKey(title),
        tooltip = translateIfKey(tooltip),
        color = UIColor.SECONDARY,
        responseAction = responseAction,
        outline = true,
      )
    }

    /**
     * @param layout Needed to set translations yes/cancel for confirmation message.
     * @param title id is used as default. Will be translated, if not starting with "'"
     * @param tooltip null default. Will be translated, if not starting with "'"
     * @param confirmMessage Will be translated, if not null and not starting with "'"
     */
    fun createSecondaryButton(
      layout: UILayout,
      id: String = "download",
      responseAction: ResponseAction,
      title: String? = id,
      tooltip: String? = null,
      confirmMessage: String? = null,
    ): UIButton {
      return UIButton(
        id,
        title = translateIfKey(title),
        tooltip = translateIfKey(tooltip),
        color = UIColor.SECONDARY,
        responseAction = responseAction,
        outline = true,
      ).withConfirmMessage(layout, confirmMessage)
    }

    fun createCloneButton(responseAction: ResponseAction): UIButton {
      return createSecondaryButton("clone", responseAction)
    }

    /**
     * @param title id is used as default. Will be translated, if not starting with "'"
     * @param tooltip null default. Will be translated, if not starting with "'"
     */
    fun createDownloadButton(
      id: String = "download",
      responseAction: ResponseAction,
      title: String = id,
      tooltip: String? = null,
      default: Boolean? = null
    ): UIButton {
      return UIButton(
        id,
        title = translateIfKey(title),
        tooltip = translateIfKey(tooltip),
        color = UIColor.DARK,
        responseAction = responseAction,
        outline = true,
        default = default,
      )
    }

    /**
     * @param title id is used as default. Will be translated, if not starting with "'"
     * @param tooltip null default. Will be translated, if not starting with "'"
     */
    fun createLinkButton(
      id: String,
      responseAction: ResponseAction,
      title: String = id,
      tooltip: String? = null,
    ): UIButton {
      return UIButton(
        id,
        title = translateIfKey(title),
        tooltip = translateIfKey(tooltip),
        color = UIColor.LINK,
        responseAction = responseAction,
      )
    }

    /**
     * @param title id is used as default. Will be translated, if not starting with "'"
     * @param tooltip null default. Will be translated, if not starting with "'"
     */
    fun createExportButton(
      id: String,
      responseAction: ResponseAction,
      title: String = id,
      tooltip: String? = null,
    ): UIButton {
      return UIButton(
        id,
        title = translateIfKey(title),
        tooltip = translateIfKey(tooltip),
        color = UIColor.LINK,
        responseAction = responseAction,
        outline = true,
      )
    }
  }
}
