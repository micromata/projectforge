/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.fibu;

import org.projectforge.common.i18n.I18nEnum;

public enum ProjektStatus implements I18nEnum
{

  /** Projektstatus nicht gesetzt. */
  NONE("none"),
  /** Projekt befindet sich noch in der Akquisephase. */
  ACQUISISTION("acquisition"),
  /** Projekt ist vorübergehend (noch) nicht aktiv. */
  ON_HOLD("onhold"), /** Projekt wird aktuell entwickelt. */
  BUILD("build"), /** Projekt ist produktiv und wird NICHT gewartet. */
  PRODUCTIVE("productive"), /** Projekt ist produktiv und wird (hoffentlich) gewartet. */
  MAINTENANCE("maintenance"), /** Projekt ist beendet. */
  ENDED("ended");

   private String key;

  /**
   * The key will be used e. g. for i18n.
   * @return
   */
  public String getKey()
  {
    return key;
  }
  
  public String getI18nKey() {
    return "fibu.projekt.status." + key;
  };

  ProjektStatus(String key)
  {
    this.key = key;
  }

  public boolean isIn(ProjektStatus... status) {
    for (ProjektStatus st : status) {
      if (this == st) {
        return true;
      }
    }
    return false;
  }
}
