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

package org.projectforge.web.meb;

import org.projectforge.business.fibu.kost.KostFilter;

import com.thoughtworks.xstream.annotations.XStreamAlias;


/**
 */
@XStreamAlias("Kost1Filter")
public class MebListFilter extends KostFilter
{
  private static final long serialVersionUID = 4887724462816700155L;

  @Override
  public MebListFilter reset()
  {
    super.reset();
    setListType(KostFilter.FILTER_NOT_ENDED);
    this.searchString = "";
    return this;
  }
}
