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

package org.projectforge.web.humanresources;

import org.projectforge.business.humanresources.HRPlanningFilter;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.framework.time.DatePrecision;

/**
 * 
 * @author Mario Groß (m.gross@micromata.de)
 *
 */
public class HRPlanningListFilter extends HRPlanningFilter
{
  private static final long serialVersionUID = 7278847635662477100L;

  @Override
  public HRPlanningListFilter reset()
  {
    super.reset();
    setUserId(ThreadLocalUserContext.getUserId());
    final DateHolder date = new DateHolder(DatePrecision.DAY);
    date.setBeginOfWeek();
    setStartTime(date.getTimestamp());
    date.setEndOfWeek();
    setStopTime(date.getTimestamp());
    return this;
  }
}
