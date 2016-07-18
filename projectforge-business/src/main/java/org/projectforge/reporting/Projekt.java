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

import java.util.List;

import org.projectforge.business.fibu.ProjektStatus;


/**
 * Interface for reporting.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * @see org.projectforge.business.fibu.ProjektDO
 */
public interface Projekt
{
  /** Synthetischer, eindeutiger Datenbankschlüssel. */
  public Integer getId();

  /**
   * Ziffer 5-6 von KOST2 (00-99)
   */
  public int getNummer();

  public String getName();

  /**
   * @return Kunde or null if not exists.
   */
  public Kunde getKunde();

  /**
   * Nur bei internen Projekten ohne Kundennummer, stellt diese Nummer die Ziffern 2-4 aus 4.* dar.
   */
  public Integer getInternKost2_4();

  /**
   * Ziffer 2-4 von Kost2: Wenn Kunde gesetzt ist, wird die Kundennummer, ansonsten internKost2_4 zurückgegeben.
   */
  public Integer getBereich();

  /**
   * @see #getNummer
   */
  public Integer getTeilbereich();

  public ProjektStatus getStatus();

  public String getDescription();

  /** @return Kost2 1.-6. Stelle: 5.123.45 */
  public String getKost();

  public boolean isDeleted();

  /** @return Alle für das Projekt verfügbaren Kost2Arten (7.-8. Ziffer von Kost2). */
  public List<Kost2Art> getKost2Arts();
}
