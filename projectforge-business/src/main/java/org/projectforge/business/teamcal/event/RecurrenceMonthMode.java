package org.projectforge.business.teamcal.event;

import org.projectforge.common.i18n.I18nEnum;

/**
 * Created by fdesel on 11.08.17.
 */
public enum RecurrenceMonthMode implements I18nEnum
{
  ATTHE("atthe"), EACH("each"), NONE("none");

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

  RecurrenceMonthMode(String key)
  {
    this.key = key;
  }

}
