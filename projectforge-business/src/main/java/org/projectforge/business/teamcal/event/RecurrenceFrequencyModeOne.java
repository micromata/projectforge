package org.projectforge.business.teamcal.event;

import org.projectforge.common.i18n.I18nEnum;

/**
 * Created by fdesel on 03.08.17.
 */
public enum RecurrenceFrequencyModeOne implements I18nEnum
{
  FIRST("first"), SECOND("second"), THIRD("third"), FOURTH("fourth"), FIFTH("fifth");

  private String key;

  @Override
  public String getI18nKey()
  {
    return "plugins.teamcal.event.recurrence." + key;
  }

  public String getKey()
  {
    return key;
  }

  RecurrenceFrequencyModeOne(String key)
  {
    this.key = key;
  }
}
