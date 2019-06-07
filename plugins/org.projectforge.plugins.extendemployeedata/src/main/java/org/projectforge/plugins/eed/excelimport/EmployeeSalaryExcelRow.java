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

package org.projectforge.plugins.eed.excelimport;

import java.math.BigDecimal;

/**
 * If you change fields here, you have to change ExtendEmployeeDataEnum accordingly.
 */
public class EmployeeSalaryExcelRow
{
  private String staffnumber;

  private BigDecimal salary;

  private String remark;

  public String getStaffnumber()
  {
    return staffnumber;
  }

  public void setStaffnumber(String staffnumber)
  {
    this.staffnumber = staffnumber;
  }

  public BigDecimal getSalary()
  {
    return salary;
  }

  public void setSalary(BigDecimal salary)
  {
    this.salary = salary;
  }

  public String getRemark()
  {
    return remark;
  }

  public void setRemark(String remark)
  {
    this.remark = remark;
  }
}
