package org.projectforge.export;

public class AttrColumnDescription
{
  private final String groupName;

  private final String propertyName;

  public AttrColumnDescription(String groupName, String propertyName)
  {
    this.groupName = groupName;
    this.propertyName = propertyName;
  }

  public String getGroupName()
  {
    return groupName;
  }

  public String getPropertyName()
  {
    return propertyName;
  }

  public String getCombinedName()
  {
    return groupName + propertyName;
  }

}
