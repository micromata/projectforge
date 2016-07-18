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

import java.util.List;

import org.apache.log4j.Logger;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.business.user.UserPrefAreaRegistry;
import org.projectforge.business.user.UserPrefDao;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.api.UserPrefArea;
import org.projectforge.framework.persistence.user.entities.UserPrefDO;
import org.projectforge.framework.persistence.user.entities.UserPrefEntryDO;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.fibu.Kost2DropDownChoice;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.EditPage;

@EditPage(defaultReturnPage = UserPrefListPage.class)
public class UserPrefEditPage extends AbstractEditPage<UserPrefDO, UserPrefEditForm, UserPrefDao> implements ISelectCallerPage
{
  private static final long serialVersionUID = 3405518532401481456L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(UserPrefEditPage.class);

  public static final String PARAMETER_AREA = "area";

  @SpringBean
  UserPrefDao userPrefDao;

  /**
   * Creates a template of the given object for the given area.
   * @param area
   * @param object Master object to fill the UserPrefDO from.
   * @see UserPrefDao#addUserPrefParameters(UserPrefDO, Object)
   */
  public UserPrefEditPage(final UserPrefArea area, final Object object)
  {
    super(new PageParameters(), "userPref");
    final UserPrefDO userPref = new UserPrefDO();
    initUserPref(userPref, area, object);
    super.init(userPref);
  }

  public UserPrefEditPage(final PageParameters parameters)
  {
    super(parameters, "userPref");
    final String areaId = WicketUtils.getAsString(parameters, PARAMETER_AREA);
    if (areaId != null) {
      final UserPrefArea area = UserPrefAreaRegistry.instance().getEntry(areaId);
      if (area != null) {
        final UserPrefDO userPref = new UserPrefDO();
        initUserPref(userPref, area, null);
        super.init(userPref);
        return;
      }
    }
    super.init();
  }

  private UserPrefDO initUserPref(final UserPrefDO userPref, final UserPrefArea area, final Object object)
  {
    userPref.setArea(area);
    userPref.setUser(ThreadLocalUserContext.getUser());
    if (object != null) {
      userPrefDao.addUserPrefParameters(userPref, object);
    } else {
      userPrefDao.addUserPrefParameters(userPref, area);
    }
    return userPref;
  }

  @Override
  public void clearIds()
  {
    super.clearIds();
    for (final UserPrefEntryDO entry : form.getData().getUserPrefEntries()) {
      entry.setId(null);
    }
  }

  /**
   * @see org.projectforge.web.fibu.ISelectCallerPage#select(java.lang.String, java.lang.Integer)
   */
  public void select(final String property, final Object selectedValue)
  {
    final UserPrefEntryDO param = getData().getUserPrefEntry(property);
    if (param == null) {
      log.error("Property '" + property + "' not supported for selection.");
    } else {
      setValue(param, selectedValue);
    }
  }

  /**
   * @see org.projectforge.web.fibu.ISelectCallerPage#unselect(java.lang.String)
   */
  public void unselect(final String property)
  {
    final UserPrefEntryDO param = getData().getUserPrefEntry(property);
    if (param == null) {
      log.error("Property '" + property + "' not supported for un-selection.");
    } else {
      setValue(param, null);
    }
  }

  private void setValue(final UserPrefEntryDO param, final Object value)
  {
    userPrefDao.setValueObject(param, value);
    final List<UserPrefEntryDO> dependents = getData().getDependentUserPrefEntries(param.getParameter());
    if (dependents != null) {
      for (final UserPrefEntryDO entry : dependents) {
        if (Kost2DO.class.isAssignableFrom(entry.getType()) == true) {
          final Kost2DropDownChoice choice = (Kost2DropDownChoice) form.dependentsMap.get(entry.getParameter());
          choice.setTaskId((Integer) value);
        }
      }
    }
  }

  /**
   * @see org.projectforge.web.fibu.ISelectCallerPage#cancelSelection(java.lang.String)
   */
  public void cancelSelection(final String property)
  {
    // Do nothing.
  }

  @Override
  protected UserPrefDao getBaseDao()
  {
    return userPrefDao;
  }

  @Override
  protected UserPrefEditForm newEditForm(final AbstractEditPage< ? , ? , ? > parentPage, final UserPrefDO data)
  {
    return new UserPrefEditForm(this, data);
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
