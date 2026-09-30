/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

public enum AuftragFakturiertFilterStatus implements I18nEnum
{
  ALL("all"),
  /** Everything is fully invoiced. */
  FAKTURIERT("vollstaendigFakturiert"),
  /**
   * At least one position has to be invoiced now: it is finished or a payment schedule is reached, dated until the end
   * of the current month (or undated).
   */
  ZU_FAKTURIEREN("zuFakturieren"),
  /** Like {@link #ZU_FAKTURIEREN}, but including reached payment schedules dated in the following months. */
  ZU_FAKTURIEREN_INKL_KUENFTIGE("zuFakturierenInklKuenftige"),
  /** The order isn't fully invoiced. */
  NICHT_FAKTURIERT("nochNichtVollstaendigFakturiert");

  private final String i18nKey;

  AuftragFakturiertFilterStatus(final String i18nKey)
  {
    this.i18nKey = i18nKey;
  }

  @Override
  public String getI18nKey()
  {
    return "fibu.auftrag.filter.type." + i18nKey;
  }
}
