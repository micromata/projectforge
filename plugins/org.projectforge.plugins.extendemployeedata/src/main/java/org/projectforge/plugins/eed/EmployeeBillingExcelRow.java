package org.projectforge.plugins.eed;

import java.math.BigDecimal;

/**
 * If you change fields here, you have to change ExtendedEmployeeDataEnum accordingly.
 */
public class EmployeeBillingExcelRow
{
  private Integer id;

  private String staffNumber;

  private BigDecimal mobilecheckmobilecheck;

  private BigDecimal daycarecenterdaycarecenter;

  private BigDecimal ebikeleasingebikeleasing;

  private BigDecimal weekendworkworkinghourssaturday;

  private BigDecimal weekendworkworkinghourssunday;

  private BigDecimal weekendworkworkinghoursholiday;

  public Integer getId()
  {
    return id;
  }

  public void setId(Integer id)
  {
    this.id = id;
  }

  public String getStaffNumber()
  {
    return staffNumber;
  }

  public void setStaffNumber(String staffNumber)
  {
    this.staffNumber = staffNumber;
  }

  public BigDecimal getMobilecheckmobilecheck()
  {
    return mobilecheckmobilecheck;
  }

  public void setMobilecheckmobilecheck(BigDecimal mobilecheckmobilecheck)
  {
    this.mobilecheckmobilecheck = mobilecheckmobilecheck;
  }

  public BigDecimal getDaycarecenterdaycarecenter()
  {
    return daycarecenterdaycarecenter;
  }

  public void setDaycarecenterdaycarecenter(BigDecimal daycarecenterdaycarecenter)
  {
    this.daycarecenterdaycarecenter = daycarecenterdaycarecenter;
  }

  public BigDecimal getEbikeleasingebikeleasing()
  {
    return ebikeleasingebikeleasing;
  }

  public void setEbikeleasingebikeleasing(BigDecimal ebikeleasingebikeleasing)
  {
    this.ebikeleasingebikeleasing = ebikeleasingebikeleasing;
  }

  public BigDecimal getWeekendworkworkinghourssaturday()
  {
    return weekendworkworkinghourssaturday;
  }

  public void setWeekendworkworkinghourssaturday(BigDecimal weekendworkworkinghourssaturday)
  {
    this.weekendworkworkinghourssaturday = weekendworkworkinghourssaturday;
  }

  public BigDecimal getWeekendworkworkinghourssunday()
  {
    return weekendworkworkinghourssunday;
  }

  public void setWeekendworkworkinghourssunday(BigDecimal weekendworkworkinghourssunday)
  {
    this.weekendworkworkinghourssunday = weekendworkworkinghourssunday;
  }

  public BigDecimal getWeekendworkworkinghoursholiday()
  {
    return weekendworkworkinghoursholiday;
  }

  public void setWeekendworkworkinghoursholiday(BigDecimal weekendworkworkinghoursholiday)
  {
    this.weekendworkworkinghoursholiday = weekendworkworkinghoursholiday;
  }

}
