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

package org.projectforge.web.mobile;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.request.http.WebRequest;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.api.UserContext;
import org.projectforge.web.LoginService;
import org.projectforge.web.Menu;
import org.projectforge.web.MenuBuilder;
import org.projectforge.web.MenuEntry;
import org.projectforge.web.session.MySession;
import org.projectforge.web.user.UserPreferencesHelper;
import org.projectforge.web.wicket.WicketUtils;

public class MenuMobilePage extends AbstractSecuredMobilePage
{
  private static final long serialVersionUID = 6709192621718648771L;

  // Indicates that the menu mobile page should be shown directly instead of restoring last page after stay-logged-in.
  private static final String PARAM_HOME_KEY = "home";

  @SpringBean
  private MenuBuilder menuBuilder;

  @SpringBean
  private LoginService loginService;

  /**
   * Returns a link to this the menu mobile page. It should be shown directly instead of restoring last page after
   * stay-logged-in .
   */
  public static JQueryButtonPanel getHomeLink(final Component parent, final String id)
  {
    final PageParameters params = new PageParameters();
    params.add(PARAM_HOME_KEY, true);
    return new JQueryButtonPanel(id, JQueryButtonType.HOME, MenuMobilePage.class, params,
        parent.getString("mobile.home")).setNoText();
  }

  public MenuMobilePage()
  {
    this(new PageParameters());
  }

  @SuppressWarnings("serial")
  public MenuMobilePage(final PageParameters parameters)
  {
    super(parameters);
    final UserContext userContext = getUserContext();
    if (userContext.isStayLoggedIn() == true) {
      userContext.setStayLoggedIn(false);
      if (WicketUtils.contains(parameters, PARAM_HOME_KEY) == false) {
        final RecentMobilePageInfo pageInfo = (RecentMobilePageInfo) UserPreferencesHelper
            .getEntry(AbstractSecuredMobilePage.USER_PREF_RECENT_PAGE);
        if (pageInfo != null && pageInfo.getPageClass() != null) {
          throw new RestartResponseException((Class<? extends Page>) pageInfo.getPageClass(),
              pageInfo.restorePageParameters());
        }
      }
    }
    setNoBackButton();
    final ListViewPanel listViewPanel = new ListViewPanel("menu");
    pageContainer.add(listViewPanel);
    listViewPanel.add(new ListViewItemPanel(listViewPanel.newChildId(), getString("menu.main.title")).setListDivider());
    final Menu menu = menuBuilder.getMobileMenu(ThreadLocalUserContext.getUser());
    if (menu.getMenuEntries() != null) {
      for (final MenuEntry menuEntry : menu.getMenuEntries()) {
        if (menuEntry.isVisible() == true) {
          listViewPanel
              .add(new ListViewItemPanel(listViewPanel.newChildId(), menuEntry.getMobilePageClass(), getString(menuEntry
                  .getI18nKey())));
        }
      }
    }
    listViewPanel.add(
        new ListViewItemPanel(listViewPanel.newChildId(), new BookmarkablePageLink<String>(ListViewItemPanel.LINK_ID,
            WicketUtils.getDefaultPage()), getString("menu.mobile.fullWebVersion")).setAsExternalLink());

    listViewPanel.add(new ListViewItemPanel(listViewPanel.newChildId(), new Link<String>(ListViewItemPanel.LINK_ID)
    {
      @Override
      public void onClick()
      {
        loginService.logout((MySession) getSession(), (WebRequest) getRequest(), (WebResponse) getResponse(), userXmlPreferencesCache, menuBuilder);
        setResponsePage(LoginMobilePage.class);
      }

    }, getString("menu.logout"))
    {
    });
    if (getMySession().isIOSDevice() == true) {
      pageContainer.add(new Label("iOSHint", getString("mobile.iOS.startScreenInfo")));
    } else {
      pageContainer.add(new Label("iOSHint", getString("mobile.others.startScreenInfo")));
    }
  }

  @Override
  protected String getTitle()
  {
    return getString("menu.main.title");
  }
}
