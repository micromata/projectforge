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

import java.util.LinkedList;
import java.util.List;

import org.projectforge.continuousdb.DatabaseResultRow;
import org.projectforge.continuousdb.DatabaseResultRowEntry;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class DatabaseResultRowImpl implements DatabaseResultRow
{
  private List<DatabaseResultRowEntry> entries = new LinkedList<DatabaseResultRowEntry>();

  @Override
  public List<DatabaseResultRowEntry> getEntries()
  {
    return entries;
  }

  @Override
  public DatabaseResultRowEntry getEntry(int index)
  {
    return entries.get(index);
  }

  @Override
  public DatabaseResultRowEntry getEntry(String name)
  {
    String lower = name.toLowerCase();
    for (DatabaseResultRowEntry entry : entries) {
      if (entry.getName() == null) {
        continue;
      }
      if (lower.equals(entry.getName().toLowerCase()) == true) {
        return entry;
      }
    }
    return null;
  }

  @Override
  public void add(DatabaseResultRowEntry entry)
  {
    entries.add(entry);
  }

}
