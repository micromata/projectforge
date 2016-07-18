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

package org.projectforge._garbage;

import org.apache.commons.lang.StringUtils;

/**
 * Stores the expressions and settings for creating a hibernate criteria object. This template is useful for avoiding the need of a
 * hibernate session in the stripes action classes.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class MemoryQueryFilter
{
  // private static final Logger log = Logger.getLogger(MemoryQueryFilter.class);

  private String[] searchKeys;

  private String[] fields;

  /**
   * All substrings of the given searchString will be AND combined and be searched in the given fields. This query will done in memory after
   * the result set of the database returned.
   * @param searchString
   * @param fields
   * @return
   */
  public MemoryQueryFilter(String searchString, String[] fields)
  {
    this.searchKeys = StringUtils.split(searchString);
    this.fields = fields;
  }

  public String[] getFields()
  {
    return fields;
  }

  public String[] getSearchKeys()
  {
    return searchKeys;
  }
}
