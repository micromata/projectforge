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

package org.projectforge.web.user;

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.business.login.Login;
import org.projectforge.business.teamcal.admin.TeamCalCache;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.user.UserDao;
import org.projectforge.business.user.UserXmlPreferencesDao;
import org.projectforge.business.user.filter.UserFilter;
import org.projectforge.business.user.service.UserService;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.session.MySession;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.AbstractSecuredBasePage;
import org.projectforge.web.wicket.MessagePage;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.components.ContentMenuEntryPanel;

public class MyAccountEditPage extends AbstractEditPage<PFUserDO, MyAccountEditForm, UserDao>
{
  private static final long serialVersionUID = 4636922408954211544L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MyAccountEditPage.class);

  @SpringBean
  private UserService userService;

  @SpringBean
  private EmployeeService employeeService;

  @SpringBean
  private TeamCalCache teamCalCache;

  @SpringBean
  private UserXmlPreferencesDao userXmlPreferencesDao;

  public MyAccountEditPage(final PageParameters parameters)
  {
    super(parameters, "user.myAccount");

    if (Login.getInstance().isPasswordChangeSupported(getUser())) {
      final BookmarkablePageLink<Void> changePwLink = new BookmarkablePageLink<>("link", ChangePasswordPage.class);
      final ContentMenuEntryPanel entry = new ContentMenuEntryPanel(getNewContentMenuChildId(), changePwLink, getString("menu.changePassword"));
      addContentMenuEntry(entry);
    }

    if (Login.getInstance().isWlanPasswordChangeSupported(getUser())) {
      final BookmarkablePageLink<Void> changeWlanPwLink = new BookmarkablePageLink<>("link", ChangeWlanPasswordPage.class);
      final ContentMenuEntryPanel entry = new ContentMenuEntryPanel(getNewContentMenuChildId(), changeWlanPwLink, getString("menu.changeWlanPassword"));
      addContentMenuEntry(entry);
    }

    final PFUserDO loggedInUser = userService.getById(ThreadLocalUserContext.getUserId());
    super.init(loggedInUser);
    this.showHistory = false;
  }

  @Override
  public AbstractSecuredBasePage onSaveOrUpdate()
  {
    return super.onSaveOrUpdate();
  }

  /**
   * @see org.projectforge.web.wicket.AbstractEditPage#afterSaveOrUpdate()
   */
  @Override
  public AbstractSecuredBasePage afterSaveOrUpdate()
  {
    Collection<TeamCalDO> teamCalRestWhiteList = form.getTeamCalRestWhiteList();
    Collection<TeamCalDO> teamCalRestBlackList = teamCalCache.getAllFullAccessCalendars();
    teamCalRestBlackList.removeAll(teamCalRestWhiteList);
    Integer[] blackListIds = teamCalRestBlackList.stream().map(cal -> cal.getId()).toArray(size -> new Integer[size]);
    userXmlPreferencesDao.saveOrUpdate(ThreadLocalUserContext.getUserId(), TeamCalDO.TEAMCALRESTBLACKLIST, blackListIds, true);

    userXmlPreferencesDao.saveOrUpdate(ThreadLocalUserContext.getUserId(), "disableSnowEffectPermant", form.getDisableSnowEffectPermant(), true);
    userXmlPreferencesCache.putEntry(ThreadLocalUserContext.getUserId(), "disableSnowEffectPermant", form.getDisableSnowEffectPermant(), true);

    final HttpServletRequest request = WicketUtils.getHttpServletRequest(getRequest());
    // Don't trust the form data, use logged in user from the data base instead.
    UserFilter.refreshUser(request);
    return super.afterSaveOrUpdate();
  }

  /**
   * @see org.projectforge.web.wicket.AbstractEditPage#updateAll()
   */
  @Override
  protected void update()
  {
    if (ThreadLocalUserContext.getUserId().equals(getData().getId()) == false) {
      throw new IllegalStateException("Oups, MyAccountEditPage is called with another than the logged in user!");
    }
    getData().setPersonalPhoneIdentifiers(userService.getNormalizedPersonalPhoneIdentifiers(getData()));
    userService.updateMyAccount(getData());
    final EmployeeDO employeeData = form.getEmployeeData();
    if (employeeData != null) {
      employeeService.updateAttribute(getData().getId(), employeeData.getIban(), "iban");
      employeeService.updateAttribute(getData().getId(), employeeData.getBic(), "bic");
      employeeService.updateAttribute(getData().getId(), employeeData.getAccountHolder(), "accountHolder");
      employeeService.updateAttribute(getData().getId(), employeeData.getStreet(), "street");
      employeeService.updateAttribute(getData().getId(), employeeData.getState(), "state");
      employeeService.updateAttribute(getData().getId(), employeeData.getCity(), "city");
      employeeService.updateAttribute(getData().getId(), employeeData.getZipCode(), "zipCode");
      employeeService.updateAttribute(getData().getId(), employeeData.getCountry(), "country");
      employeeService.updateAttribute(getData().getId(), employeeData.getBirthday(), "birthday");
    }
    ((MySession) getSession()).setLocale(getRequest());
    if (form.invalidateAllStayLoggedInSessions == true) {
      userService.renewStayLoggedInKey(getData().getId());
    }
    afterSaveOrUpdate();
    setResponsePage(new MessagePage("message.successfullChanged"));
  }

  /**
   * @throws UnsupportedOperationException
   * @see org.projectforge.web.wicket.AbstractEditPage#create()
   */
  @Override
  protected void create()
  {
    throw new UnsupportedOperationException("Oups, shouldn't be called.");
  }

  /**
   * @throws UnsupportedOperationException
   * @see org.projectforge.web.wicket.AbstractEditPage#delete()
   */
  @Override
  protected void delete()
  {
    throw new UnsupportedOperationException("Oups, shouldn't be called.");
  }

  /**
   * @throws UnsupportedOperationException
   * @see org.projectforge.web.wicket.AbstractEditPage#markAsDeleted()
   */
  @Override
  protected void markAsDeleted()
  {
    throw new UnsupportedOperationException("Oups, shouldn't be called.");
  }

  /**
   * @throws UnsupportedOperationException
   * @see org.projectforge.web.wicket.AbstractEditPage#undelete()
   */
  @Override
  protected void undelete()
  {
    throw new UnsupportedOperationException("Oups, shouldn't be called.");
  }

  @Override
  protected UserDao getBaseDao()
  {
    return userService.getUserDao();
  }

  @Override
  protected MyAccountEditForm newEditForm(final AbstractEditPage<?, ?, ?> parentPage, final PFUserDO data)
  {
    return new MyAccountEditForm(this, data);
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
