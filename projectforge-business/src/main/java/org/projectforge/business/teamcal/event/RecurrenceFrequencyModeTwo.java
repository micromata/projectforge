package org.projectforge.business.teamcal.event;

import org.projectforge.common.i18n.I18nEnum;

/**
 * Created by fdesel on 03.08.17.
 */
public enum RecurrenceFrequencyModeTwo implements I18nEnum
{
  MONDAY("monday"), TUESDAY("tuesday"), WEDNESDAY("wednesday"), THURSDAY("thursday"), FRIDAY("friday"), SATURDAY("saturday"), SUNDAY("sunday"),
  DAY("day"), WEEKDAY("weekday"), WEEKEND("weekend");

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

  RecurrenceFrequencyModeTwo(final String key)
  {
    this.key = key;
  }
}
