/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.framework.persistence.utils.ReflectionToString;

import java.util.Date;


public class ReindexSettings
{
  private Date fromDate;

  private Integer lastNEntries;

  private String entityName;

  public ReindexSettings()
  {
  }

  public ReindexSettings(final Date fromDate, final Integer lastNEntries)
  {
    this(fromDate, lastNEntries, null);
  }

  /**
   * @param entityName Full class name of the entity the re-index was started for, or null for all of them. Only
   *          used for tables holding the rows of several entities (the change history), see
   *          ReindexerStrategy.entityNameProperty: re-indexing the book list shouldn't touch the history of
   *          every other entity.
   */
  public ReindexSettings(final Date fromDate, final Integer lastNEntries, final String entityName)
  {
    this.fromDate = fromDate;
    this.lastNEntries = lastNEntries;
    this.entityName = entityName;
  }

  public Date getFromDate()
  {
    return fromDate;
  }

  public Integer getLastNEntries()
  {
    return lastNEntries;
  }

  public String getEntityName()
  {
    return entityName;
  }


  @Override
  public String toString()
  {
    final ReflectionToString tos = new ReflectionToString(this);
    return tos.toString();
  }
}
