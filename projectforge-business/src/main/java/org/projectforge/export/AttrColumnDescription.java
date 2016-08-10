package org.projectforge.export;

import org.projectforge.excel.I18nExportColumn;

public class AttrColumnDescription
{
  private final String groupName;

  private final String propertyName;

  private final String i18nKey;

  public AttrColumnDescription(String groupName, String propertyName, String i18nKey)
  {
    this.groupName = groupName;
    this.propertyName = propertyName;
    this.i18nKey = i18nKey;
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

  public I18nExportColumn toI18nExportColumn()
  {
    return new I18nExportColumn(getCombinedName(), i18nKey, 20);  // TODO CT: width
  }

}
