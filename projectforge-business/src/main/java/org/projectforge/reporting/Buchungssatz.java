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

package org.projectforge.reporting;

import java.math.BigDecimal;
import java.util.Date;

import org.projectforge.business.fibu.kost.SHType;


/**
 * Interface for reporting.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * @see org.projectforge.business.fibu.kost.BuchungssatzDO
 */
public interface Buchungssatz
{
  /** Synthetischer, eindeutiger Datenbankschlüssel. */
  public Integer getId();
  
  /** @return Jahr des Buchungsmonats. */
  public Integer getYear();

  /** @return Zugehöriger Buchungsmonat 0-11: 0-Januar, 1-Februar bis 11-Dezember. */
  public Integer getMonth();
  
  /** @return Zugehöriger Buchungsmonat zweistellig formatiert 01-12: 01-Januar, 02-Februar bis 12-Dezember. */
  public String getFormattedMonth();

  /** @return Satznr des Buchungssatzes innerhalb des Buchungsmonats. */
  public Integer getSatznr();

  /** @return Buchungsdatum */
  public Date getDatum();

  public BigDecimal getBetrag();

  /** @return Soll/Haben */
  public SHType getSh();

  /** @return Buchungskonto */
  public Konto getKonto();

  /** @return Buchungsgegenkonto. */
  public Konto getGegenKonto();

  /** @return Belegtext (Freitext) */
  public String getBeleg();

  /** @return Freitext */
  public String getText();

  /** @return Freitext */
  public String getMenge();

  public Kost1 getKost1();

  public Kost2 getKost2();

  /**
   * Kommt nicht über die Buchhaltung, sondern kann nachträglich über ProjectForge angelegt werden.
   * @return Freitext.
   */
  public String getComment();
}
