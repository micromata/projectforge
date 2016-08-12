package org.projectforge.plugins.eed;

import java.math.BigDecimal;

/**
 * If you change fields here, you have to change ExtendedEmployeeDataEnum accordingly.
 */
public class EmployeeBillingExcelRow
{
  private Integer id;

  private String fullName;

  private BigDecimal costtravelcosttravel;

  private BigDecimal expensesexpenses;

  private BigDecimal overtimeovertime;

  private BigDecimal bonusbonus;

  private BigDecimal specialpaymentspecialpayment;

  private BigDecimal targetagreementstargetagreements;

  private BigDecimal costshopcostshop;

  private String costshopcontents;

  private BigDecimal weekendworkworkinghourssaturday;

  private BigDecimal weekendworkworkinghourssunday;

  private BigDecimal weekendworkworkinghoursholiday;

  private String othersothers;

  public Integer getId()
  {
    return id;
  }

  public void setId(Integer id)
  {
    this.id = id;
  }

  public String getFullName()
  {
    return fullName;
  }

  public void setFullName(String fullName)
  {
    this.fullName = fullName;
  }

  public BigDecimal getCosttravelcosttravel()
  {
    return costtravelcosttravel;
  }

  public void setCosttravelcosttravel(BigDecimal costtravelcosttravel)
  {
    this.costtravelcosttravel = costtravelcosttravel;
  }

  public BigDecimal getExpensesexpenses()
  {
    return expensesexpenses;
  }

  public void setExpensesexpenses(BigDecimal expensesexpenses)
  {
    this.expensesexpenses = expensesexpenses;
  }

  public BigDecimal getOvertimeovertime()
  {
    return overtimeovertime;
  }

  public void setOvertimeovertime(BigDecimal overtimeovertime)
  {
    this.overtimeovertime = overtimeovertime;
  }

  public BigDecimal getBonusbonus()
  {
    return bonusbonus;
  }

  public void setBonusbonus(BigDecimal bonusbonus)
  {
    this.bonusbonus = bonusbonus;
  }

  public BigDecimal getSpecialpaymentspecialpayment()
  {
    return specialpaymentspecialpayment;
  }

  public void setSpecialpaymentspecialpayment(BigDecimal specialpaymentspecialpayment)
  {
    this.specialpaymentspecialpayment = specialpaymentspecialpayment;
  }

  public BigDecimal getTargetagreementstargetagreements()
  {
    return targetagreementstargetagreements;
  }

  public void setTargetagreementstargetagreements(BigDecimal targetagreementstargetagreements)
  {
    this.targetagreementstargetagreements = targetagreementstargetagreements;
  }

  public BigDecimal getCostshopcostshop()
  {
    return costshopcostshop;
  }

  public void setCostshopcostshop(BigDecimal costshopcostshop)
  {
    this.costshopcostshop = costshopcostshop;
  }

  public String getCostshopcontents()
  {
    return costshopcontents;
  }

  public void setCostshopcontents(String costshopcontents)
  {
    this.costshopcontents = costshopcontents;
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

  public String getOthersothers()
  {
    return othersothers;
  }

  public void setOthersothers(String othersothers)
  {
    this.othersothers = othersothers;
  }
}
