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

/**
 * @param fetchUpdateUrl If given, the status of the progress bar will be fetched every 2 seconds. The rest service
 * should update the data object or the variables part (if progress info is set as variable).
 * @param fetchUpdateInterval Update interval in ms if [fetchUpdateUrl] is given. Default is 1000 (1s).
 */
data class UIProgress(
  override var id: String,
  val title: String? = null,
  var color: UIColor? = null,
  var value: Int? = null,
  var info: String? = null,
  var infoColor: UIColor? = null,
  var cancelConfirmMessage: String? = null,
  var animated: Boolean? = null,
) : UIElement(UIElementType.PROGRESS), IUIId
