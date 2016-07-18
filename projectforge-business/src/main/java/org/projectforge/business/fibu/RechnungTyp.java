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

import org.projectforge.common.i18n.I18nEnum;

/**
 * Es gibt zwei Arten: Die normale Rechnung mit Rechnungsnummer und die Gutschriftsanzeige durch den Kunden ohne Rechnungsnummer.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public enum RechnungTyp implements I18nEnum
{
  RECHNUNG("rechnung"), GUTSCHRIFTSANZEIGE_DURCH_KUNDEN("gutschriftsAnzeigeDurchKunden");

  private String key;

  /**
   * The key will be used e. g. for i18n.
   * @return
   */
  public String getKey()
  {
    return key;
  }

  /**
   * @return The full i18n key including the i18n prefix "fibu.auftrag.status.".
   */
  public String getI18nKey()
  {
    return "fibu.rechnung.typ." + key;
  }

  RechnungTyp(String key)
  {
    this.key = key;
  }
}
