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

package org.projectforge.business.fibu.kost;

import java.io.Serializable;

import org.projectforge.framework.persistence.api.BaseSearchFilter;


/**
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class KostFilter extends BaseSearchFilter implements Serializable
{
  private static final long serialVersionUID = -1401828654796571141L;

  public static final String FILTER_ALL = "all";

  public static final String FILTER_ACTIVE = "active";

  public static final String FILTER_NON_ACTIVE = "nonactive";

  public static final String FILTER_NOT_ENDED = "notEnded";

  public static final String FILTER_ENDED = "ended";

  protected String listType = FILTER_ALL;

  public static final String[] LIST = { FILTER_ALL, FILTER_ACTIVE, FILTER_NON_ACTIVE, FILTER_NOT_ENDED, FILTER_ENDED};

  public KostFilter()
  {
  }

  public KostFilter(final BaseSearchFilter filter)
  {
    super(filter);
  }

  public boolean isShowAll()
  {
    return FILTER_ALL.equals(listType);
  }

  public boolean isActive()
  {
    return FILTER_ACTIVE.equals(listType);
  }

  public boolean isNonActive()
  {
    return FILTER_NON_ACTIVE.equals(listType);
  }

  public boolean isNotEnded()
  {
    return FILTER_NOT_ENDED.equals(listType);
  }

  public boolean isEnded()
  {
    return FILTER_ENDED.equals(listType);
  }

  public String getListType()
  {
    return this.listType;
  }

  public void setListType(final String listType)
  {
    this.listType = listType;
  }
}
