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

package org.projectforge.web.teamcal.admin;

import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.teamcal.admin.TeamCalDao;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.teamcal.admin.right.TeamCalRight;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.teamcal.event.TeamEventListPage;
import org.projectforge.web.teamcal.event.importics.TeamCalImportPage;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.AbstractSecuredBasePage;
import org.projectforge.web.wicket.EditPage;
import org.projectforge.web.wicket.components.ContentMenuEntryPanel;

/**
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 * 
 */
@EditPage(defaultReturnPage = TeamCalListPage.class)
public class TeamCalEditPage extends AbstractEditPage<TeamCalDO, TeamCalEditForm, TeamCalDao>
    implements ISelectCallerPage
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TeamCalEditPage.class);

  private static final long serialVersionUID = -3352981782657771662L;

  @SpringBean
  private TeamCalDao teamCalDao;

  /**
   * @param parameters
   * @param i18nPrefix
   */
  public TeamCalEditPage(final PageParameters parameters)
  {
    super(parameters, "plugins.teamcal");
    init();
    addTopMenuPanel();
  }

  @SuppressWarnings("serial")
  private void addTopMenuPanel()
  {
    if (isNew() == false) {
      final Integer id = form.getData().getId();
      ContentMenuEntryPanel menu = new ContentMenuEntryPanel(getNewContentMenuChildId(),
          new Link<Void>(ContentMenuEntryPanel.LINK_ID)
          {
            @Override
            public void onClick()
            {
              final TeamEventListPage teamEventListPage = new TeamEventListPage(
                  new PageParameters().add(TeamEventListPage.PARAM_CALENDARS,
                      String.valueOf(id)));
              setResponsePage(teamEventListPage);
            };
          }, getString("plugins.teamcal.events"));
      addContentMenuEntry(menu);
      final TeamCalRight right = new TeamCalRight(accessChecker);
      if (isNew() == true
          || right.hasFullAccess(getData(), getUserId()) == true && getData().isExternalSubscription() == false) {
        menu = new ContentMenuEntryPanel(getNewContentMenuChildId(), new Link<Void>(ContentMenuEntryPanel.LINK_ID)
        {
          @Override
          public void onClick()
          {
            final PageParameters parameters = new PageParameters().add(TeamCalImportPage.PARAM_KEY_TEAM_CAL_ID,
                getData().getId());
            final TeamCalImportPage importPage = new TeamCalImportPage(parameters);
            importPage.setReturnToPage(TeamCalEditPage.this);
            setResponsePage(importPage);
          };
        }, getString("import")).setTooltip(getString("plugins.teamcal.import.ics.tooltip"));
        addContentMenuEntry(menu);
      }
    }
  }

  @Override
  public AbstractSecuredBasePage onSaveOrUpdate()
  {
    teamCalDao.setFullAccessUsers(getData(), form.fullAccessUsersListHelper.getAssignedItems());
    teamCalDao.setReadonlyAccessUsers(getData(), form.readonlyAccessUsersListHelper.getAssignedItems());
    teamCalDao.setMinimalAccessUsers(getData(), form.minimalAccessUsersListHelper.getAssignedItems());
    teamCalDao.setFullAccessGroups(getData(), form.fullAccessGroupsListHelper.getAssignedItems());
    teamCalDao.setReadonlyAccessGroups(getData(), form.readonlyAccessGroupsListHelper.getAssignedItems());
    teamCalDao.setMinimalAccessGroups(getData(), form.minimalAccessGroupsListHelper.getAssignedItems());
    return super.onSaveOrUpdate();
  }

  /**
   * @see org.projectforge.web.fibu.ISelectCallerPage#select(java.lang.String, java.lang.Integer)
   */
  public void select(final String property, final Object selectedValue)
  {
    log.error("Property '" + property + "' not supported for selection.");
  }

  /**
   * @see org.projectforge.web.fibu.ISelectCallerPage#unselect(java.lang.String)
   */
  public void unselect(final String property)
  {
    log.error("Property '" + property + "' not supported for unselection.");
  }

  /**
   * @see org.projectforge.web.fibu.ISelectCallerPage#cancelSelection(java.lang.String)
   */
  public void cancelSelection(final String property)
  {
    log.error("Property '" + property + "' not supported for cancelling.");
  }

  /**
   * @see org.projectforge.web.wicket.AbstractEditPage#getBaseDao()
   */
  @Override
  protected TeamCalDao getBaseDao()
  {
    return teamCalDao;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractEditPage#getLogger()
   */
  @Override
  protected Logger getLogger()
  {
    return log;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractEditPage#newEditForm(org.projectforge.web.wicket.AbstractEditPage,
   *      org.projectforge.core.AbstractBaseDO)
   */
  @Override
  protected TeamCalEditForm newEditForm(final AbstractEditPage<?, ?, ?> parentPage, final TeamCalDO data)
  {
    return new TeamCalEditForm(this, data);
  }
}
