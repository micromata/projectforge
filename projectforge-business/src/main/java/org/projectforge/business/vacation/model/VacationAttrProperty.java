package org.projectforge.business.vacation.model;

public enum VacationAttrProperty
{
  PREVIOUSYEARLEAVE("previousyearleave"), // 
  PREVIOUSYEARLEAVEUSED("previousyearleaveused");

  private String propertyName;

  VacationAttrProperty(String propertyName)
  {
    this.propertyName = propertyName;
  }

  public String getPropertyName()
  {
    return propertyName;
  }
}
