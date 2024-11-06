/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

import de.micromata.merlin.excel.importer.ImportedElement;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.projectforge.business.fibu.EmployeeCache;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.EmployeeSalaryDO;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.core.importstorage.AbstractImportStoragePanel;
import org.projectforge.web.core.importstorage.ImportFilter;

class EmployeeSalaryImportStoragePanel extends AbstractImportStoragePanel<EmployeeSalaryImportPage> {
    EmployeeSalaryImportStoragePanel(final String id, final EmployeeSalaryImportPage parentPage,
                                     final ImportFilter filter) {
        super(id, parentPage, filter);
    }

    @Override
    protected void addHeadColumns(final RepeatingView headColRepeater) {
        headColRepeater.add(new Label(headColRepeater.newChildId(), getString("fibu.employee.staffNumber")));
        headColRepeater.add(new Label(headColRepeater.newChildId(), getString("name")));
        headColRepeater.add(new Label(headColRepeater.newChildId(), getString("calendar.year")));
        headColRepeater.add(new Label(headColRepeater.newChildId(), getString("calendar.month")));
        headColRepeater.add(new Label(headColRepeater.newChildId(), getString("fibu.employee.salary.bruttoMitAgAnteil")));
        headColRepeater.add(new Label(headColRepeater.newChildId(), getString("comment")));
    }

    @Override
    protected void addColumns(final RepeatingView cellRepeater, final ImportedElement<?> element, final String style) {
        EmployeeCache employeeCache = WicketSupport.get(EmployeeCache.class);
        UserGroupCache userGroupCache = WicketSupport.get(UserGroupCache.class);
        final String s = "white-space: nowrap; text-align: right;";
        final String styleRightAlign = (style == null) ? s : style + " " + s;
        final EmployeeSalaryDO employeeSalary = (EmployeeSalaryDO) element.getValue();
        final EmployeeDO employee = employeeSalary != null ? employeeCache.getEmployee(employeeSalary.getEmployeeId()) : null;
        final PFUserDO user = employee != null ? userGroupCache.getUser(employee.getUserId()) : null;
        addCell(cellRepeater, employee != null ? employee.getStaffNumber() : "", styleRightAlign);
        addCell(cellRepeater, employee != null ? user.getFullname() : "", styleRightAlign);
        addCell(cellRepeater, employeeSalary != null ? employeeSalary.getYear() : null, styleRightAlign);
        addCell(cellRepeater, employeeSalary != null ? employeeSalary.getMonth() : null, styleRightAlign);
        addCell(cellRepeater, employeeSalary != null && employeeSalary.getBruttoMitAgAnteil() != null ? employeeSalary.getBruttoMitAgAnteil().toString() : "",
                styleRightAlign);
        addCell(cellRepeater, employeeSalary != null ? employeeSalary.getComment() : "", styleRightAlign);
    }
}
