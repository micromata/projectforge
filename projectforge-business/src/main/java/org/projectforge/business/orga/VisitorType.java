package org.projectforge.business.orga;

import org.projectforge.common.i18n.I18nEnum;

public enum VisitorType implements I18nEnum
{
  NORMAL("normal"),
  EXTENDED("extended"),
  FAMILY("family");

  private final String i18nKey;

  VisitorType(String i18nKey)
  {
    this.i18nKey = i18nKey;
  }

  /**
   * @return The full i18n key including the i18n prefix "gender.".
   */
  public String getI18nKey()
  {
    return "orga.visitorbook.visitortype." + i18nKey;
  }

}
