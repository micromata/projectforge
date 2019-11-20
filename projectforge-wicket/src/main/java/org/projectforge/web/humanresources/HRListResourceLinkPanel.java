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

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.projectforge.business.humanresources.HRViewDao;
import org.projectforge.business.humanresources.HRViewData;
import org.projectforge.business.user.UserFormatter;
import org.projectforge.business.utils.HtmlHelper;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.web.wicket.WebConstants;

import java.util.Date;
import java.util.List;

/**
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 *
 */
public class HRListResourceLinkPanel extends Panel
{

  private static final long serialVersionUID = -718881597957595460L;

  private final RepeatingView userRepeater;

  private final HRListPage hrListPage;

  private final HRViewDao hrViewDao;

  private final UserFormatter userFormatter;

  public HRListResourceLinkPanel(final String id, final HRListPage hrListPage, final HRViewDao hrViewDao, final UserFormatter userFormatter)
  {
    super(id);
    this.hrListPage = hrListPage;
    this.hrViewDao = hrViewDao;
    this.userFormatter = userFormatter;
    userRepeater = new RepeatingView("userRepeater");
    add(userRepeater);
  }

  public void refresh(final HRViewData hrViewData, final Date startTime)
  {
    userRepeater.removeAll();
    final List<PFUserDO> unplannedUsers = hrViewDao.getUnplannedResources(hrViewData);
    for (final PFUserDO user : unplannedUsers) {
      if (user.getHrPlanning() == false || user.hasSystemAccess() == false) {
        continue;
      }
      final WebMarkupContainer container = new WebMarkupContainer(userRepeater.newChildId());
      userRepeater.add(container);
      @SuppressWarnings("serial")
      final Link<Object> link = new Link<Object>("resourceLink") {
        @Override
        public void onClick()
        {
          final DateHolder date = new DateHolder(startTime);
          final Long millis = date.getSQLDate().getTime();
          final PageParameters pageParams = new PageParameters();
          pageParams.add(WebConstants.PARAMETER_USER_ID, String.valueOf(user.getId()));
          pageParams.add(WebConstants.PARAMETER_DATE, millis.toString());
          final HRPlanningEditPage page = new HRPlanningEditPage(pageParams);
          page.setReturnToPage(hrListPage);
          setResponsePage(page);
        }
      };
      container.add(link);
      link.add(new Label("user", HtmlHelper.escapeXml(userFormatter.formatUser(user)) + "<br/>").setEscapeModelStrings(false));
    }
  }
}
