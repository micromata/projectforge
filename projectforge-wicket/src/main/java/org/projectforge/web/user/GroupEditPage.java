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

package org.projectforge.web.user;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.projectforge.business.ldap.GroupDOConverter;
import org.projectforge.business.user.GroupDao;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.AbstractSecuredBasePage;
import org.projectforge.web.wicket.EditPage;
import org.projectforge.web.wicket.WicketUtils;
import org.slf4j.Logger;

@EditPage(defaultReturnPage = GroupListPage.class)
public class GroupEditPage extends AbstractEditPage<GroupDO, GroupEditForm, GroupDao> implements ISelectCallerPage {
  /**
   * Parameter for pre-defining group name (e. g. used by a wizard for creating new groups).
   */
  public static final String PARAM_GROUP_NAME = "groupName";

  private static final long serialVersionUID = 4636922408954211544L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GroupEditPage.class);

  private final String selectProperty;

  public GroupEditPage(final PageParameters parameters)
  {
    this(parameters, null);
  }

  public GroupEditPage(final PageParameters parameters, final String selectProperty)
  {
    super(parameters, "group");
    if (!UserGroupCache.getInstance().isUserMemberOfAdminGroup()) {
      throw new AccessException("You are not allowed to access this page.");
    }
    this.selectProperty = selectProperty;
    super.init();
    final String groupName = WicketUtils.getAsString(parameters, PARAM_GROUP_NAME);
    if (StringUtils.isNotEmpty(groupName) == true) {
      getData().setName(groupName);
    }
  }

  @Override
  public AbstractSecuredBasePage onSaveOrUpdate()
  {
    WicketSupport.get(GroupDao.class).setAssignedUsers(getData(), form.assignUsersListHelper.getAssignedItems());
    //groupDao.setNestedGroups(getData(), form.nestedGroupsListHelper.getAssignedItems());

    if (form.ldapGroupValues != null && form.ldapGroupValues.isValuesEmpty() == false) {
      final String xml = WicketSupport.get(GroupDOConverter.class).getLdapValuesAsXml(form.ldapGroupValues);
      getData().setLdapValues(xml);
    }
    return super.onSaveOrUpdate();
  }

  @Override
  public AbstractSecuredBasePage afterSaveOrUpdate()
  {
    if (selectProperty != null) {
      ((ISelectCallerPage) returnToPage).select(selectProperty, getData().getId());
    }

    return super.afterSaveOrUpdate();
  }

  @Override
  protected GroupDao getBaseDao()
  {
    return WicketSupport.get(GroupDao.class);
  }

  @Override
  protected GroupEditForm newEditForm(final AbstractEditPage<?, ?, ?> parentPage, final GroupDO data)
  {
    return new GroupEditForm(this, data);
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }

    @Override
    public void select(String property, Object selectedValue) {

    }

    @Override
    public void unselect(String property) {

    }

    @Override
    public void cancelSelection(String property) {

    }
}
