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

package org.projectforge.web.fibu;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.projectforge.business.fibu.ProjektFilter;


/**
 */
@XStreamAlias("ProjektFilter")
public class ProjektListFilter extends ProjektFilter
{
  private static final long serialVersionUID = 4576278400076797415L;

  @Override
  public ProjektListFilter reset()
  {
    super.reset();
    setListType(ProjektFilter.FILTER_NOT_ENDED);
    this.setSearchString("");
    return this;
  }
}
