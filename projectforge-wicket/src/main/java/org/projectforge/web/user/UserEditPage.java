/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.ldap.PFUserDOConverter;
import org.projectforge.business.login.Login;
import org.projectforge.business.user.GroupDao;
import org.projectforge.business.user.UserDao;
import org.projectforge.business.user.UserRightDao;
import org.projectforge.business.user.UserRightVO;
import org.projectforge.business.user.service.UserService;
import org.projectforge.framework.configuration.Configuration;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.AbstractSecuredBasePage;
import org.projectforge.web.wicket.EditPage;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@EditPage(defaultReturnPage = UserListPage.class)
public class UserEditPage extends AbstractEditPage<PFUserDO, UserEditForm, UserDao>
{
  private static final long serialVersionUID = 4636922408954211544L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserEditPage.class);

  @SpringBean
  private GroupDao groupDao;

  @SpringBean
  private UserService userService;

  @SpringBean
  private UserRightDao userRightDao;

  public UserEditPage(final PageParameters parameters)
  {
    super(parameters, "user");
    super.init();
    myInit();
  }

  private void myInit()
  {
    if (isNew() == true) {
      getData().setTimeZone(Configuration.getInstance().getDefaultTimeZone());
    }
  }

  @Override
  public AbstractSecuredBasePage onSaveOrUpdate()
  {
    final PFUserDO passwordUser = form.getPasswordUser();
    if (passwordUser != null) {
      getData().setPassword(passwordUser.getPassword());
      getData().setPasswordSalt(passwordUser.getPasswordSalt());
      userService.onPasswordChange(getData(), false);
    }
    getData().setPersonalPhoneIdentifiers(userService.getNormalizedPersonalPhoneIdentifiers(getData()));
    if (form.ldapUserValues.isValuesEmpty() == false) {
      final String xml = PFUserDOConverter.getLdapValuesAsXml(form.ldapUserValues);
      getData().setLdapValues(xml);
    }

    if (StringUtils.isNotEmpty(form.getWlanPassword())) {
      userService.onWlanPasswordChange(getData(), false); // persist new time, history is created by caller
    }

    return super.onSaveOrUpdate();
  }

  @Override
  public AbstractSecuredBasePage afterSaveOrUpdate()
  {
    long startAll = System.currentTimeMillis();
    log.info("Start afterSaveOrUpdate() in UserEditPage");
    log.info("Start assign groups");
    long start = System.currentTimeMillis();
    groupDao.assignGroups(getData(), form.assignGroupsListHelper.getItemsToAssign(),
        form.assignGroupsListHelper.getItemsToUnassign(), false);
    long end = System.currentTimeMillis();
    log.info("Finish assign groups. Took: " + (end - start) / 1000 + " sec.");
    if (form.rightsData != null) {
      log.info("Start updating user rights");
      start = System.currentTimeMillis();
      final List<UserRightVO> list = form.rightsData.getRights();
      userRightDao.updateUserRights(getData(), list, false);
      end = System.currentTimeMillis();
      log.info("Finish updating user rights. Took: " + (end - start) / 1000 + " sec.");
    }
    if (form.getPasswordUser() != null) {
      log.info("Start password change");
      start = System.currentTimeMillis();
      Login.getInstance().passwordChanged(getData(), form.getPassword().toCharArray());
      end = System.currentTimeMillis();
      log.info("Finish password change. Took: " + (end - start) / 1000 + " sec.");
    }

    if (StringUtils.isNotEmpty(form.getWlanPassword())) {
      log.info("Start WLAN password change");
      start = System.currentTimeMillis();
      Login.getInstance().wlanPasswordChanged(getData(), form.getWlanPassword().toCharArray());
      end = System.currentTimeMillis();
      log.info("Finish WLAN password change. Took: " + (end - start) / 1000 + " sec.");
    }

    //Only one time reload user group cache
    log.info("Start force reload user group cache.");
    start = System.currentTimeMillis();
    end = System.currentTimeMillis();
    log.info("Finish force reload user group cache. Took: " + (end - start) / 1000 + " sec.");
    // Force to reload Menu directly (if the admin user modified himself), otherwise menu is
    // reloaded after next page call.
    end = System.currentTimeMillis();
    log.info("Finish afterSaveOrUpdate() in UserEditPage. Took: " + (end - startAll) / 1000 + " sec.");
    return new UserListPage(new PageParameters());
  }

  @Override
  protected UserDao getBaseDao()
  {
    return userService.getUserDao();
  }

  @Override
  protected UserEditForm newEditForm(final AbstractEditPage<?, ?, ?> parentPage, final PFUserDO data)
  {
    return new UserEditForm(this, data);
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
