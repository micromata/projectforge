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

package org.projectforge.excel;

import java.util.HashMap;
import java.util.Map;

public class PropertyMapping
{
  protected Map<String, Object> map = new HashMap<String, Object>();

  public void add(String propertyName, Object obj)
  {
    // Convert boolean results to a number 1 or blank
    if (obj instanceof Boolean) {
      if ((Boolean) obj == true) {
        obj = 1;
      } else {
        obj = null;
      }
    }
    map.put(propertyName, obj);
  }

  public void add(Enum< ? > e, Object obj)
  {
    add(e.name(), obj);
  }

  public Map<String, Object> getMapping()
  {
    return map;
  }
}
