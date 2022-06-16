/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

import kotlin.reflect.KProperty

class UIReadOnlyField(
  var id: String? = null,
  @Transient
  override val layoutContext: LayoutContext? = null,
  var dataType: UIDataType = UIDataType.STRING,
  override var label: String? = null,
  override var additionalLabel: String? = null,
  override var tooltip: String? = null,
  @Transient
  override val ignoreAdditionalLabel: Boolean = false,
  @Transient
  override val ignoreTooltip: Boolean = false,
  /**
   * For copying values.
   */
  val canCopy: Boolean? = null,
  /**
   * Hide password fields.
   */
  val coverUp: Boolean? = null,
  /** For a fixed value. Id is ignored then. */
  val value: String? = null,
  property: KProperty<*>? = null,
) : UIElement(UIElementType.READONLY_FIELD), UILabelledElement {
  init {
    if (property != null && id == null) {
      id = property.name
    }
  }
}
