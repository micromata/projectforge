package org.projectforge.business.fibu;

import org.projectforge.common.i18n.I18nEnum;

public enum Gender implements I18nEnum
{
  NOT_KNOWN(0, "notKnown"),
  MALE(1, "male"),
  FEMALE(2, "female"),
  NOT_APPLICABLE(9, "notApplicable");

  private final int isoCode;
  private final String i18nKey;

  Gender(int isoCode, String i18nKey)
  {
    this.isoCode = isoCode;
    this.i18nKey = i18nKey;
  }

  /**
   * @return The integer representation of the gender according to the ISO/IEC 5218.
   */
  public int getIsoCode()
  {
    return isoCode;
  }

  /**
   * @return The full i18n key including the i18n prefix "gender.".
   */
  public String getI18nKey()
  {
    return "gender." + i18nKey;
  }

}
