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

package org.projectforge.framework.persistence.api;

import org.projectforge.framework.persistence.history.FlatDisplayHistoryEntry;
import org.projectforge.framework.persistence.history.HistoryEntry;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

import java.util.List;

public class SearchResultData
{
  protected PFUserDO modifiedByUser;

  protected ExtendedBaseDO<Integer> dataObject;

  protected List<FlatDisplayHistoryEntry> propertyChanges;

  protected HistoryEntry historyEntry;

  public ExtendedBaseDO<Integer> getDataObject()
  {
    return dataObject;
  }

  /**
   * History entry of the object for the search result.
   */
  public HistoryEntry getHistoryEntry()
  {
    return historyEntry;
  }

  /**
   * All changes in history entry with resolved properties to display.
   *
   * @return
   */
  public List<FlatDisplayHistoryEntry> getPropertyChanges()
  {
    return propertyChanges;
  }

  /**
   * The user who has done the represented modification if available.
   *
   * @return
   */
  public PFUserDO getModifiedByUser()
  {
    return modifiedByUser;
  }
}
