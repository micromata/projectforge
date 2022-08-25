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

/**
 * @param fetchUpdateUrl If given, the status of the progress bar will be fetched every 2 seconds. The rest service
 * should update the data object or the variables part (if progress info is set as variable).
 * @param fetchUpdateInterval Update interval in ms if [fetchUpdateUrl] is given. Default is 2000 (2s).
 */
data class UIProgress(
  override var id: String,
  var collapseable: Boolean? = null,
  var fetchUpdateUrl: String? = null,
  var fetchUpdateInterval: Long? = null,
) : UIElement(UIElementType.PROGRESS), IUIId {
  class Data(
    var value: Int?,
    var title: String? = null,
    var info: String? = null,
    var color: UIColor? = null,
    var animated: Boolean? = false,
  )
}
