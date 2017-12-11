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

import java.util.Calendar;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.EmployeeSalaryDO;
import org.projectforge.business.fibu.EmployeeSalaryDao;
import org.projectforge.business.fibu.EmployeeSalaryType;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.AbstractSecuredBasePage;
import org.projectforge.web.wicket.EditPage;
import org.slf4j.Logger;

@EditPage(defaultReturnPage = EmployeeSalaryListPage.class)
public class EmployeeSalaryEditPage
    extends AbstractEditPage<EmployeeSalaryDO, EmployeeSalaryEditForm, EmployeeSalaryDao> implements
    ISelectCallerPage
{
  private static final long serialVersionUID = -3899191243765232906L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EmployeeSalaryEditPage.class);

  @SpringBean
  private EmployeeSalaryDao employeeSalaryDao;

  public EmployeeSalaryEditPage(final PageParameters parameters)
  {
    super(parameters, "fibu.employee.salary");
    init();
  }

  private EmployeeSalaryEditRecentEntry recent;

  @Override
  protected void onPreEdit()
  {
    super.onPreEdit();
    if (getData().getId() == null) {
      recent = getRecent();
      getData().setYear(recent.getYear());
      getData().setMonth(recent.getMonth());
      getData().setType(recent.getType());
    }
  }

  private EmployeeSalaryEditRecentEntry getRecent()
  {
    if (recent == null) {
      recent = (EmployeeSalaryEditRecentEntry) getUserPrefEntry(EmployeeSalaryEditRecentEntry.class.getName());
    }
    if (recent == null) {
      recent = new EmployeeSalaryEditRecentEntry();
      final Calendar cal = DateHelper.getCalendar();
      recent.setYear(cal.get(Calendar.YEAR));
      recent.setMonth(cal.get(Calendar.MONTH));
      recent.setType(EmployeeSalaryType.GEHALT);
      putUserPrefEntry(EmployeeSalaryEditRecentEntry.class.getName(), recent, true);
    }
    return recent;
  }

  @Override
  public AbstractSecuredBasePage afterSaveOrUpdate()
  {
    recent = getRecent();
    if (getData().getYear() != null) {
      recent.setYear(getData().getYear());
    }
    if (getData().getMonth() != null) {
      recent.setMonth(getData().getMonth());
    }
    if (getData().getType() != null) {
      recent.setType(getData().getType());
    }
    return null;
  }

  /**
   * @see org.projectforge.web.fibu.ISelectCallerPage#select(java.lang.String, java.lang.Integer)
   */
  @Override
  public void select(final String property, final Object selectedValue)
  {
    if ("userId".equals(property) == true) {
      final Integer id;
      if (selectedValue instanceof String) {
        id = NumberHelper.parseInteger((String) selectedValue);
      } else {
        id = (Integer) selectedValue;
      }
      employeeSalaryDao.setEmployee(getData(), id);
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
  protected EmployeeSalaryDao getBaseDao()
  {
    return employeeSalaryDao;
  }

  @Override
  protected EmployeeSalaryEditForm newEditForm(final AbstractEditPage<?, ?, ?> parentPage, final EmployeeSalaryDO data)
  {
    return new EmployeeSalaryEditForm(this, data);
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
