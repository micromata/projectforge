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

package org.projectforge.web;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.http.WebRequest;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.login.LoginResultStatus;
import org.projectforge.business.user.UserDao;
import org.projectforge.business.user.UserXmlPreferencesCache;
import org.projectforge.business.user.filter.UserFilter;
import org.projectforge.business.utils.HtmlHelper;
import org.projectforge.framework.configuration.ConfigurationParam;
import org.projectforge.framework.configuration.GlobalConfiguration;
import org.projectforge.framework.persistence.database.DatabaseUpdateService;
import org.projectforge.framework.persistence.database.InitDatabaseDao;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.admin.SetupPage;
import org.projectforge.web.admin.SystemUpdatePage;
import org.projectforge.web.mobile.LoginMobilePage;
import org.projectforge.web.session.MySession;
import org.projectforge.web.wicket.AbstractUnsecureBasePage;
import org.projectforge.web.wicket.WicketUtils;

public class LoginPage extends AbstractUnsecureBasePage
{
  private static final long serialVersionUID = 4457817484456315374L;

  public static final String REQUEST_PARAM_LOGOUT = "logout";

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(LoginPage.class);

  // Used by LoginMobilePage
  private static final String PARAMETER_KEY_FORCE_NON_MOBILE = "forceNonMobile";

  @SpringBean
  private DatabaseUpdateService myDatabaseUpdateService;

  @SpringBean
  private UserDao userDao;

  @SpringBean
  private LoginService loginService;

  @SpringBean
  private InitDatabaseDao initDatabaseDao;

  private WebMarkupContainer errorsContainer;

  private String errorMessage;

  private LoginForm form = null;

  /**
   * Add parameter to force non-mobile version. This avoids a redirect to the LoginMobilePage and is used by
   * LoginMobilePage.
   * 
   * @return PageParameters.
   */
  public static PageParameters forceNonMobile()
  {
    final PageParameters params = new PageParameters();
    params.add(PARAMETER_KEY_FORCE_NON_MOBILE, "true");
    return params;
  }

  public static void logout(final MySession mySession, final WebRequest request, final WebResponse response,
      final UserXmlPreferencesCache userXmlPreferencesCache, MenuBuilder menuBuilder)
  {
    final Cookie stayLoggedInCookie = UserFilter.getStayLoggedInCookie(WicketUtils.getHttpServletRequest(request));
    logout(mySession, stayLoggedInCookie, userXmlPreferencesCache, menuBuilder);
    if (stayLoggedInCookie != null) {
      response.addCookie(stayLoggedInCookie);
    }
  }

  public static void logout(final MySession mySession, final HttpServletRequest request,
      final HttpServletResponse response,
      final UserXmlPreferencesCache userXmlPreferencesCache, MenuBuilder menuBuilder)
  {
    final Cookie stayLoggedInCookie = UserFilter.getStayLoggedInCookie(request);
    logout(mySession, stayLoggedInCookie, userXmlPreferencesCache, menuBuilder);
    if (stayLoggedInCookie != null) {
      response.addCookie(stayLoggedInCookie);
    }
  }

  private static void logout(final MySession mySession, final Cookie stayLoggedInCookie,
      final UserXmlPreferencesCache userXmlPreferencesCache, MenuBuilder menuBuilder)
  {
    final PFUserDO user = mySession.getUser();
    if (user != null) {
      userXmlPreferencesCache.flushToDB(user.getId());
      userXmlPreferencesCache.clear(user.getId());
      if (menuBuilder != null) {
        menuBuilder.expireMenu(user.getId());
      }
    }
    mySession.logout();
    if (stayLoggedInCookie != null) {
      stayLoggedInCookie.setMaxAge(0);
      stayLoggedInCookie.setValue(null);
      stayLoggedInCookie.setPath("/");
    }
  }

  @Override
  protected void thisIsAnUnsecuredPage()
  {
  }

  @SuppressWarnings("serial")
  public LoginPage(final PageParameters parameters)
  {
    super(parameters);
    if (myDatabaseUpdateService.databaseTablesWithEntriesExists() == true) {
      log.info("Data-base is empty: redirect to SetupPage...");
      throw new RestartResponseException(SetupPage.class);
    }
    if (getMySession().isMobileUserAgent() == true
        && UserFilter.isUpdateRequiredFirst() == false
        && "true".equals(WicketUtils.getAsString(parameters, PARAMETER_KEY_FORCE_NON_MOBILE)) == false) {
      throw new RestartResponseException(LoginMobilePage.class);
    }
    final PFUserDO wicketSessionUser = getMySession().getUser();
    final PFUserDO sessionUser = UserFilter.getUser(WicketUtils.getHttpServletRequest(getRequest()));
    // Sometimes the wicket session user is given but the http session user is lost (re-login required).
    if (wicketSessionUser != null && sessionUser != null && wicketSessionUser.getId() == sessionUser.getId()) {
      if (UserFilter.isUpdateRequiredFirst() == true) {
        log.info("Admin login for maintenance (data-base update) successful for user '"
            + wicketSessionUser.getUsername() + "'.");

        throw new RestartResponseException(SystemUpdatePage.class);
      }
      throw new RestartResponseException(WicketUtils.getDefaultPage());
    }
    //    if (initDatabaseDao.isEmpty() == true) {
    //      log.info("Data-base is empty: redirect to SetupPage...");
    //      throw new RestartResponseException(SetupPage.class);
    //    }
    form = new LoginForm(this);
    body.add(AttributeModifier.replace("class", "loginpage"));
    body.add(form);
    form.init();
    // Use the following message to check a ProjectForge installation with your monitoring tool (such as Nagios):
    final String message = WebConfiguration.isUpAndRunning() ? "ProjectForge is alive."
        : "ProjectForge is not full available (perhaps in maintenance mode or in start-up phase).";
    body.add(new Label("statusComment", "<!-- " + HtmlHelper.escapeHtml(message, false) + " -->")
        .setEscapeModelStrings(false)
        .setRenderBodyOnly(true));
    final WebMarkupContainer administratorLoginNeeded = new WebMarkupContainer("administratorLoginNeeded");
    body.add(administratorLoginNeeded);
    if (UserFilter.isUpdateRequiredFirst() == false) {
      administratorLoginNeeded.setVisible(false);
    }
    {
      final String messageOfTheDay = GlobalConfiguration.getInstance()
          .getStringValue(ConfigurationParam.MESSAGE_OF_THE_DAY);
      final WebMarkupContainer container = new WebMarkupContainer("messageOfTheDay");
      body.add(container.setVisible(StringUtils.isNotBlank(messageOfTheDay)));
      final Label messageOfTheDayLabel = new Label("msg", messageOfTheDay);
      container.add(messageOfTheDayLabel.setEscapeModelStrings(false));
    }
    errorsContainer = new WebMarkupContainer("errors");
    body.add(errorsContainer.setVisible(false));
    errorsContainer.add(new Label("msg", new Model<String>()
    {
      @Override
      public String getObject()
      {
        return StringUtils.defaultString(errorMessage);
      }
    }));
  }

  public LoginResultStatus checkLogin()
  {
    return loginService.internalCheckLogin(this, form.getUsername(), form.getPassword(), form.isStayLoggedIn(),
        WicketUtils.getDefaultPage());
  }

  void addError(final String msg)
  {
    errorMessage = msg;
    errorsContainer.setVisible(true);
  }

  @Override
  protected String getTitle()
  {
    return getString("login.title");
  }
}
