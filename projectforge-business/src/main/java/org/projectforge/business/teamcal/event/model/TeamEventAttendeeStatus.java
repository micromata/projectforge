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

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public enum TeamEventAttendeeStatus implements I18nEnum
{
  ACCEPTED("accepted"), COMPLETED("completed"), DECLINED("declined"), DELEGATED("delegated"), //
  IN_PROCESS("in_process"), NEEDS_ACTION("needs_action"), TENTATIVE("tentative"), NEW("new");

  private String key;

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

  TeamEventAttendeeStatus(final String key)
  {
    this.key = key;
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
}
