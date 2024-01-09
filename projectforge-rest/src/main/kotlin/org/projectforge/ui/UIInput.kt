/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

import com.fasterxml.jackson.annotation.JsonValue
import org.projectforge.framework.i18n.InternalErrorException
import org.projectforge.rest.core.AbstractPagesRest
import org.springframework.util.ClassUtils

data class UIInput(
  override var id: String,
  @Transient
  override val layoutContext: LayoutContext? = null,
  var maxLength: Int? = null,
  var required: Boolean? = null,
  var focus: Boolean? = null,
  val color: UIColor? = null,
  var dataType: UIDataType = UIDataType.STRING,
  override var label: String? = null,
  override var additionalLabel: String? = null,
  override var tooltip: String? = null,
  @Transient
  override val ignoreAdditionalLabel: Boolean = false,
  @Transient
  override val ignoreTooltip: Boolean = false,
  /**
   * AutoComplete Types for HTML Input fields.
   */
  val autoComplete: AutoCompleteType? = null
) : UIElement(UIElementType.INPUT), UILabelledElement, IUIId {
  var autoCompletionUrl: String? = null

  /**
   * Optional url params given by the client. This map contains as keys the url parameters for the GET request and
   * as values the variables to post (variables of the client's data model).
   */
  var autoCompletionUrlParams: Map<String, String>? = null

  /**
   * AutoComplete Types for HTML Input fields.
   *
   * https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#autofilling-form-controls%3A-the-autocomplete-attribute
   */
  enum class AutoCompleteType(@JsonValue val htmlName: String) {
    USERNAME("username"),
    CURRENT_PASSWORD("current-password"),
    NEW_PASSWORD("new-password"),
    OFF("off"),
  }

  /**
   * Please note: Only enabled properties in [BaseDao] are available due to security reasons.
   * @return this for chaining.
   * @see BaseDao.isAutocompletionPropertyEnabled
   */
  fun enableAutoCompletion(services: AbstractPagesRest<*, *, *>): UIInput {
    if (!services.isAutocompletionPropertyEnabled(id)) {
      throw InternalErrorException(
        "Development error: You must enable autocompletion properties explicit in '${
          ClassUtils.getUserClass(
            services.baseDao
          ).simpleName
        }.isAutocompletionPropertyEnabled(String)' for property '$id' for security resasons first."
      )
    }
    autoCompletionUrl = "${services.getRestPath()}/${AutoCompletion.AUTOCOMPLETE_TEXT}?property=$id&search=:search"
    return this
  }

  /**
   * @return this for chaining.
   */
  fun setAutoCompletion(url: String, autoCompletionUrlParams: Map<String, String>?): UIInput {
    this.autoCompletionUrl = url
    this.autoCompletionUrlParams = autoCompletionUrlParams
    return this
  }
}
