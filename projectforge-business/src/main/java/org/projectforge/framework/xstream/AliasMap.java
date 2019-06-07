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

package org.projectforge.framework.xstream;

import java.util.HashMap;
import java.util.Map;

public class AliasMap
{
  private Map<Class< ? >, String> aliasMap1 = new HashMap<Class< ? >, String>();

  private Map<String, Class< ? >> aliasMap2 = new HashMap<String, Class< ? >>();

  public String getAliasForClass(final Class< ? > type)
  {
    return aliasMap1.get(type);
  }
  
  public boolean containsAlias(final String alias) {
    return aliasMap2.containsKey(alias);
  }

  public Class< ? > getClassForAlias(final String alias)
  {
    return aliasMap2.get(alias);
  }

  public void put(final Class< ? > clazz, final String alias)
  {
    aliasMap1.put(clazz, alias);
    aliasMap2.put(alias, clazz);
  }
}
