package org.projectforge.plugins.eed.wicket;

import java.util.Date;
import java.util.List;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.EmployeeTimedDO;
import org.projectforge.export.AttrColumnDescription;
import org.projectforge.framework.persistence.utils.ImportedElement;
import org.projectforge.web.core.importstorage.AbstractImportStoragePanel;
import org.projectforge.web.core.importstorage.ImportFilter;

import de.micromata.genome.db.jpa.tabattr.api.TimeableService;

class EmployeeBillingImportStoragePanel extends AbstractImportStoragePanel<EmployeeBillingImportPage>
{
  @SpringBean
  private TimeableService<Integer, EmployeeTimedDO> timeableService;

  private Date dateToSelectAttrRow;

  EmployeeBillingImportStoragePanel(final String id, final EmployeeBillingImportPage parentPage,
      final ImportFilter filter)
  {
    super(id, parentPage, filter);
  }

  @Override
  protected void addHeadColumns(final RepeatingView headColRepeater)
  {
    headColRepeater.add(new Label(headColRepeater.newChildId(), getString("id")));
    headColRepeater.add(new Label(headColRepeater.newChildId(), getString("fibu.employee.user")));

    final List<AttrColumnDescription> attrColumnsInSheet = parentPage.getAttrColumnsInSheet();
    if (attrColumnsInSheet != null) {
      attrColumnsInSheet.forEach(
          desc -> headColRepeater.add(new Label(headColRepeater.newChildId(), getString(desc.getI18nKey())))
      );
    }
  }

  @Override
  protected void addColumns(final RepeatingView cellRepeater, final ImportedElement<?> element, final String style)
  {
    final String s = "white-space: nowrap; text-align: right;";
    final String styleRightAlign = (style == null) ? s : style + " " + s;
    final EmployeeDO employee = (EmployeeDO) element.getValue();
    addCell(cellRepeater, employee.getPk(), styleRightAlign);
    final String fullname = (employee.getUser() == null) ? null : employee.getUser().getFullname();
    addCell(cellRepeater, fullname, style);

    final List<AttrColumnDescription> attrColumnsInSheet = parentPage.getAttrColumnsInSheet();
    if (attrColumnsInSheet != null) {
      attrColumnsInSheet.forEach(
          desc -> addCell(cellRepeater, getAttribute(employee, desc), styleRightAlign)
      );
    }
  }

  void setDateToSelectAttrRow(Date dateToSelectAttrRow)
  {
    this.dateToSelectAttrRow = dateToSelectAttrRow;
  }

  private String getAttribute(final EmployeeDO employee, final AttrColumnDescription desc)
  {
    final EmployeeTimedDO attrRow = timeableService.getAttrRowForSameMonth(employee, desc.getGroupName(), dateToSelectAttrRow);
    return attrRow.getStringAttribute(desc.getPropertyName());
  }
}
