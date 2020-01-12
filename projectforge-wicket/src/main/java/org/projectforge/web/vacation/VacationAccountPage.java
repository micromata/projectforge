/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.web.vacation;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.projectforge.business.fibu.EmployeeDao;
import org.projectforge.business.vacation.service.VacationService;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.AbstractStandardFormPage;

public class VacationAccountPage extends AbstractStandardFormPage implements ISelectCallerPage {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(VacationAccountPage.class);

  @SpringBean
  private EmployeeDao employeeDao;

  @SpringBean
  private VacationService vacationService;


  private final VacationAccountForm form;

  @Override
  protected void onInitialize() {
    super.onInitialize();
    vacationService.hasAccessToVacationService(getUser(), true); // throw runtime exception if not allowed
  }

  @Override
  protected String getTitle() {
    return getString("vacation.leaveaccount.title");
  }

  public VacationAccountPage(final PageParameters parameters) {
    super(parameters);
    form = new VacationAccountForm(this);
    StringValue employeeString = parameters.get("employee");
    if (!employeeString.isEmpty()) {
      Integer employeeId = NumberHelper.parseInteger(employeeString.toString());
      if (employeeId != null) {
        form.setEmployee(employeeDao.internalGetById(employeeId));
      }
    }
    body.add(form);
    form.init();
  }

  @Override
  public void select(String property, Object selectedValue) {
    if ("employee".equals(property) == true) {
      final Integer id;
      if (selectedValue instanceof String) {
        id = NumberHelper.parseInteger((String) selectedValue);
      } else {
        id = (Integer) selectedValue;
      }
      form.setEmployee(employeeDao.internalGetById(id));
    } else {
      log.error("Property '" + property + "' not supported for selection.");
    }
  }

  @Override
  public void unselect(String property) {

  }

  @Override
  public void cancelSelection(String property) {

  }
}
