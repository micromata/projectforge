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

package org.projectforge.web.fibu;

import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.EditPage;

@EditPage(defaultReturnPage = EmployeeListPage.class)
public class EmployeeEditPage extends AbstractEditPage<EmployeeDO, EmployeeEditForm, EmployeeService>
    implements ISelectCallerPage
{
  private static final long serialVersionUID = -3899191243765232906L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(EmployeeEditPage.class);

  @SpringBean
  private EmployeeService employeeService;

  public EmployeeEditPage(final PageParameters parameters)
  {
    super(parameters, "fibu.employee");
    init();
    body.remove("tabTitle");
    body.add(new Label("tabTitle", getString(getTitleKey(i18nPrefix, isNew()))).setRenderBodyOnly(true));
  }

  /**
   * @see org.projectforge.web.fibu.ISelectCallerPage#select(java.lang.String, java.lang.Object)
   */
  @Override
  public void select(final String property, final Object selectedValue)
  {
    // TODO PF6 never been called?!
    if ("userId".equals(property) == true) {
      final Integer id;
      if (selectedValue instanceof String) {
        id = NumberHelper.parseInteger((String) selectedValue);
      } else {
        id = (Integer) selectedValue;
      }
      employeeService.setPfUser(getData(), id);
    } else if ("kost1Id".equals(property) == true) {
      final Integer id;
      if (selectedValue instanceof String) {
        id = NumberHelper.parseInteger((String) selectedValue);
      } else {
        id = (Integer) selectedValue;
      }
      getBaseDao().setKost1(getData(), id);
    } else {
      log.error("Property '" + property + "' not supported for selection.");
    }
  }

  /**
   * @see org.projectforge.web.fibu.ISelectCallerPage#unselect(java.lang.String)
   */
  @Override
  public void unselect(final String property)
  {
    log.error("Property '" + property + "' not supported for selection.");
  }

  /**
   * @see org.projectforge.web.fibu.ISelectCallerPage#cancelSelection(java.lang.String)
   */
  @Override
  public void cancelSelection(final String property)
  {
    // Do nothing.
  }

  @Override
  protected EmployeeService getBaseDao()
  {
    return employeeService;
  }

  @Override
  protected EmployeeEditForm newEditForm(final AbstractEditPage<?, ?, ?> parentPage, final EmployeeDO data)
  {
    return new EmployeeEditForm(this, data);
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }

  @Override
  protected String getTitle()
  {
    return getString(getTitleKey(i18nPrefix, isNew())) + " → " + getData().getUser().getFullname();
  }
}
