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

package org.projectforge.plugins.eed.wicket;

import java.util.Date;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.EmployeeTimedDO;
import org.projectforge.framework.persistence.utils.ImportedElement;
import org.projectforge.plugins.eed.ExtendedEmployeeDataEnum;
import org.projectforge.web.core.importstorage.AbstractImportStoragePanel;
import org.projectforge.web.core.importstorage.ImportFilter;

import de.micromata.genome.db.jpa.tabattr.api.TimeableService;

public class EmployeeBillingImportStoragePanel extends AbstractImportStoragePanel<EmployeeBillingImportPage>
{
  //  protected BusinessAssessment businessAssessment;

  @SpringBean
  private TimeableService<Integer, EmployeeTimedDO> timeableService;

  public EmployeeBillingImportStoragePanel(final String id, final EmployeeBillingImportPage parentPage,
      final ImportFilter filter)
  {
    super(id, parentPage, filter);
  }

  //  @Override
  //  public void refresh()
  //  {
  //    super.refresh();
  //  }

  //  @SuppressWarnings("serial")
  //  @Override
  //  protected void appendSheetActionLinks(final ImportedSheet<?> sheet, final RepeatingView actionLinkRepeater)
  //  {
  //        addActionLink(actionLinkRepeater, new SubmitLink("actionLink")
  //        {
  //          @Override
  //          public void onSubmit()
  //          {
  //            parentPage.showBusinessAssessment(sheet.getName());
  //          }
  //        }, "show business assessment");
  //  }

  @Override
  protected void addHeadColumns(final RepeatingView headColRepeater)
  {
    headColRepeater.add(new Label(headColRepeater.newChildId(), getString("id")));
    headColRepeater.add(new Label(headColRepeater.newChildId(), getString("fibu.employee.staffNumber")));

    ExtendedEmployeeDataEnum.getAllAttrColumnDescriptions().forEach(
        desc -> headColRepeater.add(new Label(headColRepeater.newChildId(), getString(desc.getI18nKey()))));
  }

  @Override
  protected void addColumns(final RepeatingView cellRepeater, final ImportedElement<?> element, final String style)
  {
    final EmployeeDO employee = (EmployeeDO) element.getValue();
    addCell(cellRepeater, employee.getPk(), style + " white-space: nowrap; text-align: right;");
    addCell(cellRepeater, employee.getStaffNumber(), style);

    ExtendedEmployeeDataEnum.getAllAttrColumnDescriptions().forEach(
        desc -> addCell(cellRepeater, getAttribute(employee, desc.getGroupName(), desc.getPropertyName()), style));
  }

  private String getAttribute(final EmployeeDO employee, final String groupName, final String propertyName)
  {
    final Date dateToSelectAttrRow = new Date(); // TODO CT
    final EmployeeTimedDO attrRow = timeableService.getAttrRowForSameMonth(employee, groupName, dateToSelectAttrRow);
    return attrRow.getStringAttribute(propertyName);
  }
}
