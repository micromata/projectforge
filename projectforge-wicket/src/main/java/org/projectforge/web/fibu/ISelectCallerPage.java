/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.web.fibu;

public interface ISelectCallerPage
{
  /**
   * Called by the SelectListPage (callee) after an item was selected.
   * @param property The property of the caller holding the item to select.
   * @param selectedValue The value of the selected object.
   */
  public void select(String property, Object selectedValue);

  /**
   * Called by the SelectPanel if the user clicked unselect.
   * @param property The property holding the item to unselect.
   */
  public void unselect(String property);

  /**
   * Called by the SelectListPage (callee) after cancellation. By default, the caller can ignore this event.
   * @param property The property of the caller holding the item to select.
   */
  public void cancelSelection(String property);
}
