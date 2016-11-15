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
