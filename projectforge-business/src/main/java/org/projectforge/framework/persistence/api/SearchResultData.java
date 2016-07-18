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

package org.projectforge.framework.persistence.api;

import java.util.List;

import org.projectforge.framework.persistence.history.DisplayHistoryEntry;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

import de.micromata.genome.db.jpa.history.api.HistoryEntry;

public class SearchResultData
{
  protected PFUserDO modifiedByUser;

  protected ExtendedBaseDO<Integer> dataObject;

  protected List<DisplayHistoryEntry> propertyChanges;

  protected HistoryEntry historyEntry;

  public ExtendedBaseDO<Integer> getDataObject()
  {
    return dataObject;
  }

  /**
   * History entriy of the object for the search result.
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
  public List<DisplayHistoryEntry> getPropertyChanges()
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
