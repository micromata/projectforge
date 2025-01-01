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

package org.projectforge.web.core;

import org.projectforge.framework.persistence.api.SearchFilter;
import org.projectforge.framework.xmlstream.XmlObject;

@XmlObject(alias = "searchPageFilter")
public class SearchPageFilter extends SearchFilter
{
  public static final String ALL = "ALL";

  private static final long serialVersionUID = 2056162179686892853L;

  private Integer lastDays;

  private String area = ALL;

  public Integer getLastDays()
  {
    return lastDays;
  }

  public void setLastDays(final Integer lastDays)
  {
    this.lastDays = lastDays;
  }

  public String getArea()
  {
    return area;
  }

  public void setArea(final String area)
  {
    this.area = area;
  }

  public void setMaxRows(final Integer maxRows)
  {
    super.setMaxRows(maxRows != null ? maxRows : 3);
  }
}
