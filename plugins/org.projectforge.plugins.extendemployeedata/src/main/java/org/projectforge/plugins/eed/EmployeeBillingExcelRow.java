package org.projectforge.plugins.eed;

import java.math.BigDecimal;

public class EmployeeBillingExcelRow
{
  private Integer id;

  private String staffNumber;

  private BigDecimal eBikeLeasing;

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

  public BigDecimal geteBikeLeasing()
  {
    return eBikeLeasing;
  }

  public void seteBikeLeasing(BigDecimal eBikeLeasing)
  {
    this.eBikeLeasing = eBikeLeasing;
  }
}
