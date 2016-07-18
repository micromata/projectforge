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

package org.projectforge.business.fibu;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Deprecated
public enum AuftragsArt
{
  /** Festpreisprojekt */
  PROJEKT("projekt"), /** Folgerelease eines Festpreisprojekts */
  PROJEKT_RELEASE("projekt_release"), CHANGE_REQUEST("change_request"), HOT_FIX("hot_fix"), DAUERNDE_LEISTUNG("dauernde_leistung"), WARTUNG(
      "wartung"), TIME_AND_MATERIALS("time_and_materials");

  public static final AuftragsArt[] LIST = new AuftragsArt[] { PROJEKT, PROJEKT_RELEASE, CHANGE_REQUEST, HOT_FIX, DAUERNDE_LEISTUNG,
      WARTUNG, TIME_AND_MATERIALS};

  private String key;

  /**
   * The key will be used e. g. for i18n.
   * @return
   */
  public String getKey()
  {
    return key;
  }

  AuftragsArt(String key)
  {
    this.key = key;
  }
}
