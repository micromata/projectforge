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

package org.projectforge.web.core;

import javax.servlet.jsp.PageContext;

import org.projectforge.common.i18n.Priority;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.business.utils.HtmlHelper;
import org.springframework.stereotype.Service;

@Service
public class PriorityFormatter
{
  public String getFormattedPriority(Priority priority)
  {
    if (priority == null) {
      return "";
    }
    StringBuffer buf = new StringBuffer();
    buf.append("<span");
    HtmlHelper.attribute(buf, "class", "priority_" + priority.getKey());
    buf.append(">");
    buf.append(ThreadLocalUserContext.getLocalizedString("priority." + priority.getKey()));
    buf.append("</span>");
    return buf.toString();
  }

  @Deprecated
  public String format(PageContext pageContext, Priority priority)
  {
    return getFormattedPriority(priority);
  }
}
