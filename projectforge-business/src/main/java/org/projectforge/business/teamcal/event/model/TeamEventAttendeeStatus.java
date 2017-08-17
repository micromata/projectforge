/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.business.teamcal.event.model;

import org.projectforge.common.i18n.I18nEnum;
import org.projectforge.framework.i18n.I18nHelper;

import net.fortuna.ical4j.model.parameter.PartStat;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public enum TeamEventAttendeeStatus implements I18nEnum
{
  ACCEPTED("accepted", PartStat.ACCEPTED),
  COMPLETED("completed", PartStat.COMPLETED),
  DECLINED("declined", PartStat.DECLINED),
  DELEGATED("delegated", PartStat.DELEGATED),
  IN_PROCESS("in_process", PartStat.IN_PROCESS),
  NEEDS_ACTION("needs_action", PartStat.NEEDS_ACTION),
  TENTATIVE("tentative", PartStat.TENTATIVE);

  private String key;
  private PartStat partStat;

  /**
   * @return The key suffix will be used e. g. for i18n.
   */
  public String getKey()
  {
    return key;
  }

  /**
   * @return The full i18n key including the i18n prefix "fibu.auftrag.status.".
   */
  @Override
  public String getI18nKey()
  {
    return "plugins.teamcal.attendee.status." + key;
  }

  /**
   * @return The i18n localized value.
   */
  public String getI18nValue()
  {
    return I18nHelper.getLocalizedMessage(getI18nKey());
  }

  TeamEventAttendeeStatus(final String key, final PartStat partStat)
  {
    this.key = key;
    this.partStat = partStat;
  }

  public boolean isIn(final TeamEventAttendeeStatus... status)
  {
    for (final TeamEventAttendeeStatus st : status) {
      if (this == st) {
        return true;
      }
    }
    return false;
  }

  public PartStat getPartStat()
  {
    return this.partStat;
  }

  public static TeamEventAttendeeStatus getStatusForPartStat(final String partStat)
  {
    if (PartStat.ACCEPTED.getValue().equals(partStat)) {
      return TeamEventAttendeeStatus.ACCEPTED;
    }
    if (PartStat.COMPLETED.getValue().equals(partStat)) {
      return TeamEventAttendeeStatus.COMPLETED;
    }
    if (PartStat.DECLINED.getValue().equals(partStat)) {
      return TeamEventAttendeeStatus.DECLINED;
    }
    if (PartStat.DELEGATED.getValue().equals(partStat)) {
      return TeamEventAttendeeStatus.DELEGATED;
    }
    if (PartStat.IN_PROCESS.getValue().equals(partStat)) {
      return TeamEventAttendeeStatus.IN_PROCESS;
    }
    if (PartStat.NEEDS_ACTION.getValue().equals(partStat)) {
      return TeamEventAttendeeStatus.NEEDS_ACTION;
    }
    if (PartStat.TENTATIVE.getValue().equals(partStat)) {
      return TeamEventAttendeeStatus.TENTATIVE;
    }

    return null;
  }
}
