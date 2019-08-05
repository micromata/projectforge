/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.eed.wicket;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.EmployeeDao;
import org.projectforge.business.user.UserRightId;
import org.projectforge.business.user.UserRightValue;
import org.projectforge.plugins.eed.service.LBExporterService;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.AbstractStandardFormPage;
import org.projectforge.web.wicket.DownloadUtils;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

public class ExportDataPage extends AbstractStandardFormPage implements ISelectCallerPage
{
  private static final long serialVersionUID = -7157440416517271655L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ExportDataPage.class);

  private final ExportDataForm form;

  @SpringBean
  private EmployeeDao employeeDao;

  @SpringBean
  private LBExporterService exporterService;

  public ExportDataPage(final PageParameters parameters)
  {
    super(parameters);
    form = new ExportDataForm(this);
    body.add(form);
    form.init();
  }

  @Override
  protected String getTitle()
  {
    return getString("plugins.eed.export.title");
  }

  @Override
  public void select(String property, Object selectedValue)
  {

  }

  @Override
  public void unselect(String property)
  {

  }

  @Override
  public void cancelSelection(String property)
  {

  }

  public void exportData()
  {
    checkAccess();
    log.info("Export data for LB");
    List<EmployeeDO> employeeList = employeeDao.internalLoadAllNotDeleted();
    final String filename = "Liste-PF-"
        + form.selectedMonth + "-" + form.selectedYear
        + ".xls";
    Calendar cal = new GregorianCalendar(form.selectedYear, form.selectedMonth - 1, 1);
    byte[] xls = exporterService.getExcel(employeeList, cal);
    if (xls == null || xls.length == 0) {
      log.error("Oups, xls has zero size. Filename: " + filename);
      return;
    }
    DownloadUtils.setDownloadTarget(xls, filename);
  }

  private void checkAccess()
  {
    accessChecker.checkLoggedInUserRight(UserRightId.HR_EMPLOYEE_SALARY, UserRightValue.READONLY,
        UserRightValue.READWRITE);
    accessChecker.checkRestrictedOrDemoUser();
  }

}
