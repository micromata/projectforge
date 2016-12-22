package org.projectforge.plugins.eed.wicket;

import java.util.Date;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.EmployeeSalaryDO;
import org.projectforge.framework.persistence.utils.ImportedElement;
import org.projectforge.web.core.importstorage.AbstractImportStoragePanel;
import org.projectforge.web.core.importstorage.ImportFilter;

import de.micromata.genome.db.jpa.tabattr.api.TimeableService;

class EmployeeSalaryImportStoragePanel extends AbstractImportStoragePanel<EmployeeSalaryImportPage>
{
  @SpringBean
  private TimeableService timeableService;

  private Date dateToSelectAttrRow;

  EmployeeSalaryImportStoragePanel(final String id, final EmployeeSalaryImportPage parentPage,
      final ImportFilter filter)
  {
    super(id, parentPage, filter);
  }

  @Override
  protected void addHeadColumns(final RepeatingView headColRepeater)
  {
    headColRepeater.add(new Label(headColRepeater.newChildId(), getString("fibu.employee.staffNumber")));
    headColRepeater.add(new Label(headColRepeater.newChildId(), getString("name")));
    headColRepeater.add(new Label(headColRepeater.newChildId(), getString("calendar.year")));
    headColRepeater.add(new Label(headColRepeater.newChildId(), getString("calendar.month")));
    headColRepeater.add(new Label(headColRepeater.newChildId(), getString("fibu.employee.salary.bruttoMitAgAnteil")));
    headColRepeater.add(new Label(headColRepeater.newChildId(), getString("comment")));
  }

  @Override
  protected void addColumns(final RepeatingView cellRepeater, final ImportedElement<?> element, final String style)
  {
    final String s = "white-space: nowrap; text-align: right;";
    final String styleRightAlign = (style == null) ? s : style + " " + s;
    final EmployeeSalaryDO employeeSalary = (EmployeeSalaryDO) element.getValue();
    final EmployeeDO employee = employeeSalary != null ? employeeSalary.getEmployee() : null;
    addCell(cellRepeater, employee != null ? employee.getStaffNumber() : "", styleRightAlign);
    addCell(cellRepeater, employee != null ? employee.getUser().getFullname() : "", styleRightAlign);
    addCell(cellRepeater, employeeSalary != null ? employeeSalary.getYear() : null, styleRightAlign);
    addCell(cellRepeater, employeeSalary != null ? employeeSalary.getMonth() : null, styleRightAlign);
    addCell(cellRepeater, employeeSalary != null && employeeSalary.getBruttoMitAgAnteil() != null ? employeeSalary.getBruttoMitAgAnteil().toString() : "",
        styleRightAlign);
    addCell(cellRepeater, employeeSalary != null ? employeeSalary.getComment() : "", styleRightAlign);
  }

  void setDateToSelectAttrRow(Date dateToSelectAttrRow)
  {
    this.dateToSelectAttrRow = dateToSelectAttrRow;
  }
}
