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

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.AppVersion;
import org.projectforge.business.user.UserDao;
import org.projectforge.business.user.UserXmlPreferencesCache;
import org.projectforge.business.user.filter.UserFilter;
import org.projectforge.framework.configuration.ConfigurationParam;
import org.projectforge.framework.configuration.GlobalConfiguration;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.LoginPage;
import org.projectforge.web.LoginService;
import org.projectforge.web.session.MySession;
import org.projectforge.web.wicket.WicketUtils;

public class LoginMobilePage extends AbstractMobilePage
{
  private static final long serialVersionUID = 313568971144109236L;

  @SpringBean
  private LoginService loginService;

  @SpringBean
  private UserDao userDao;

  @SpringBean
  protected UserXmlPreferencesCache userXmlPreferencesCache;

  private final LoginMobileForm form;

  @Override
  protected void thisIsAnUnsecuredPage()
  {
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public LoginMobilePage(final PageParameters parameters)
  {
    super(parameters);
    final PFUserDO wicketSessionUser = ((MySession) getSession()).getUser();
    final PFUserDO sessionUser = UserFilter.getUser(WicketUtils.getHttpServletRequest(getRequest()));
    // Sometimes the wicket session user is given but the http session user is lost (re-login required).
    if (wicketSessionUser != null && sessionUser != null && wicketSessionUser.getId() == sessionUser.getId()) {
      final Integer userId = sessionUser.getId();
      final RecentMobilePageInfo pageInfo = (RecentMobilePageInfo) userXmlPreferencesCache.getEntry(userId,
          AbstractSecuredMobilePage.USER_PREF_RECENT_PAGE);
      if (pageInfo != null && pageInfo.getPageClass() != null) {
        throw new RestartResponseException((Class) pageInfo.getPageClass(), pageInfo.restorePageParameters());
      } else {
        throw new RestartResponseException(WicketUtils.getDefaultMobilePage());
      }
    }
    setNoBackButton();
    form = new LoginMobileForm(this);
    pageContainer.add(form);
    form.init();
    pageContainer.add(new FeedbackPanel("feedback").setOutputMarkupId(true));
    final String messageOfTheDay = GlobalConfiguration.getInstance()
        .getStringValue(ConfigurationParam.MESSAGE_OF_THE_DAY);
    if (StringUtils.isBlank(messageOfTheDay) == true) {
      pageContainer.add(new Label("messageOfTheDay", "[invisible]").setVisible(false));
    } else {
      pageContainer.add(new Label("messageOfTheDay", messageOfTheDay));
    }
    @SuppressWarnings("serial")
    final Link<Void> goButton = new Link<Void>("goFullWebVersion")
    {
      @Override
      public final void onClick()
      {
        setResponsePage(LoginPage.class, LoginPage.forceNonMobile());
      }
    };
    pageContainer.add(goButton);
  }

  void checkLogin()
  {
    loginService.internalCheckLogin(this, form.getUsername(), form.getPassword(), form.isStayLoggedIn(), WicketUtils.getDefaultMobilePage());
  }

  /**
   * @return Application title
   */
  @Override
  protected void addTopCenter()
  {
    headerContainer.add(new Label(AbstractMobilePage.TOP_CENTER_ID, AppVersion.APP_TITLE));
  }

  /**
   * Invisible
   */
  @Override
  protected void addTopLeftButton()
  {
    headerContainer.add(WicketUtils.getInvisibleComponent(TOP_LEFT_BUTTON_ID));
  }

  /**
   * Invisible
   */
  @Override
  protected void addTopRightButton()
  {
    headerContainer.add(WicketUtils.getInvisibleComponent(TOP_RIGHT_BUTTON_ID));
  }

  @Override
  protected String getTitle()
  {
    return getString("login.title");
  }
}
