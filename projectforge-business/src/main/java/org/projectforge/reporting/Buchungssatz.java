/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.reporting;

import org.projectforge.business.fibu.kost.SHType;

import java.math.BigDecimal;
import java.time.LocalDate;


/**
 * Interface for reporting.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * @see org.projectforge.business.fibu.kost.BuchungssatzDO
 */
public interface Buchungssatz
{
  /** Synthetischer, eindeutiger Datenbankschlüssel. */
  Integer getId();

  /** @return Jahr des Buchungsmonats. */
  Integer getYear();

  /** @return Zugehöriger Buchungsmonat. 1-January, ..., 12-December. */
  Integer getMonth();

  /** @return Zugehöriger Buchungsmonat zweistellig formatiert 01-12: 01-Januar, 02-Februar bis 12-Dezember. */
  String getFormattedMonth();

  /** @return Satznr des Buchungssatzes innerhalb des Buchungsmonats. */
  Integer getSatznr();

  /** @return Buchungsdatum */
  LocalDate getDatum();

  BigDecimal getBetrag();

  /** @return Soll/Haben */
  SHType getSh();

  /** @return Buchungskonto */
  Konto getKonto();

  /** @return Buchungsgegenkonto. */
  Konto getGegenKonto();

  /** @return Belegtext (Freitext) */
  String getBeleg();

  /** @return Freitext */
  String getText();

  /** @return Freitext */
  String getMenge();

  Kost1 getKost1();

  Kost2 getKost2();

  /**
   * Kommt nicht über die Buchhaltung, sondern kann nachträglich über ProjectForge angelegt werden.
   * @return Freitext.
   */
  String getComment();
}
