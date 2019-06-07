/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.persistence.history;

/**
 * The Class HistoryPropertiesEntry.
 *
 * @author Roger Kommer (r.kommer.extern@micromata.de)
 */
public class HistoryPropertiesEntry
{

  /**
   * The old value.
   */
  private Object oldValue;

  /**
   * The new value.
   */
  private Object newValue;

  /**
   * Instantiates a new history properties entry.
   */
  public HistoryPropertiesEntry()
  {

  }

  /**
   * Instantiates a new history properties entry.
   *
   * @param oldValue the old value
   * @param newValue the new value
   */
  public HistoryPropertiesEntry(Object oldValue, Object newValue)
  {

    this.oldValue = oldValue;
    this.newValue = newValue;
  }

  /**
   * Gets the old value.
   *
   * @return the old value
   */
  public Object getOldValue()
  {
    return oldValue;
  }

  /**
   * Sets the old value.
   *
   * @param oldValue the new old value
   */
  public void setOldValue(Object oldValue)
  {
    this.oldValue = oldValue;
  }

  /**
   * Gets the new value.
   *
   * @return the new value
   */
  public Object getNewValue()
  {
    return newValue;
  }

  /**
   * Sets the new value.
   *
   * @param newValue the new new value
   */
  public void setNewValue(Object newValue)
  {
    this.newValue = newValue;
  }

}
