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

package org.projectforge.web.access;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.projectforge.framework.access.AccessDao;
import org.projectforge.framework.access.GroupTaskAccessDO;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.EditPage;
import org.slf4j.Logger;

@EditPage(defaultReturnPage = AccessListPage.class)
public class AccessEditPage extends AbstractEditPage<GroupTaskAccessDO, AccessEditForm, AccessDao>
    implements ISelectCallerPage
{
  private static final long serialVersionUID = 4636922408954211544L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AccessEditPage.class);

  public AccessEditPage(final PageParameters parameters)
  {
    super(parameters, "access");
    super.init();
  }

  @Override
  public void cancelSelection(final String property)
  {
    // Do nothing.
  }

  //Create Or Update
  @Override
  protected void create()
  {
    GroupTaskAccessDO accessDaoEntry = WicketSupport.getAccessDao().getEntry(getData().getTask(), getData().getGroup());
    if(accessDaoEntry != null && accessDaoEntry.getDeleted()) {
      getData().setId(accessDaoEntry.getId());
      super.undelete();
    }
    else if(accessDaoEntry != null && accessDaoEntry.getDeleted() == false) {
      error(getLocalizedMessage("access.exception.standard", accessDaoEntry.getTask().getTitle(), accessDaoEntry
        .getGroup().getName()));
    }
    else {
      super.create();
    }
  }

  @Override
  public void select(final String property, final Object selectedValue)
  {
    var accessDao = WicketSupport.getAccessDao();
    if ("taskId".equals(property) == true) {
      accessDao.setTask(getData(), (Long) selectedValue);
    } else if ("groupId".equals(property) == true) {
      accessDao.setGroup(getData(), (Long) selectedValue);
      form.groupSelectPanel.getTextField().modelChanged();
    } else {
      log.error("Property '" + property + "' not supported for selection.");
    }
  }

  @Override
  public void unselect(final String property)
  {
    if ("taskId".equals(property) == true) {
      getData().setTask(null);
    } else if ("groupId".equals(property) == true) {
      getData().setGroup(null);
      form.groupSelectPanel.getTextField().modelChanged();
    } else {
      log.error("Property '" + property + "' not supported for unselection.");
    }
  }

  @Override
  protected AccessDao getBaseDao()
  {
    return WicketSupport.getAccessDao();
  }

  @Override
  protected AccessEditForm newEditForm(final AbstractEditPage<?, ?, ?> parentPage, final GroupTaskAccessDO data)
  {
    return new AccessEditForm(this, data);
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
