package org.projectforge.business.vacation.model;

import org.projectforge.common.i18n.I18nEnum;

public enum VacationStatus implements I18nEnum
{
  APPROVED("approved"), REJECTED("rejected"), IN_PROGRESS("inProgress");

  private String key;

  /**
   * @return The full i18n key including the i18n prefix "fibu.auftrag.status.".
   */
  public String getI18nKey()
  {
    return "vacation.status." + key;
  }

  /**
   * The key will be used e. g. for i18n.
   * 
   * @return
   */
  public String getKey()
  {
    return key;
  }

  VacationStatus(String key)
  {
    this.key = key;
  }
}