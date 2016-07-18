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

package org.projectforge.continuousdb.jdbc;

import org.projectforge.continuousdb.DatabaseResultRowEntry;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class DatabaseResultRowEntryImpl implements DatabaseResultRowEntry
{
  private int type;

  private Object value;

  private String name;
  
  public DatabaseResultRowEntryImpl(int type, String name, Object value)
  {
    this.type = type;
    this.name = name;
    this.value = value;
  }

  @Override
  public int getType()
  {
    return type;
  }

  @Override
  public Object getValue()
  {
    return value;
  }

  @Override
  public String getName()
  {
    return name;
  }
}
