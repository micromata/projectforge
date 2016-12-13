package org.projectforge.business.vacation.model;

import org.projectforge.common.i18n.I18nEnum;

/**
 * Created by blumenstein on 22.11.16.
 */
public enum VacationMode implements I18nEnum
{
  OWN("own"), SUBSTITUTION("substitution"), MANAGER("manager"), OTHER("other");

  private String key;

  /**
   * @return The full i18n key including the i18n prefix "fibu.auftrag.status.".
   */
  public String getI18nKey()
  {
    return "vacation." + key;
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

  VacationMode(String key)
  {
    this.key = key;
  }
}
