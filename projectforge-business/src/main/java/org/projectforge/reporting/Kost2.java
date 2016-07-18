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

import org.projectforge.business.fibu.kost.KostentraegerStatus;

/**
 * Interface for reporting.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * @see org.projectforge.business.fibu.kost.Kost2DO
 */
public interface Kost2
{
  /** Synthetischer, eindeutiger Datenbankschlüssel. */
  public Integer getId();
  
  /** Ist der Kostenträger aktiv? */
  public KostentraegerStatus getKostentraegerStatus();

  /** @return Die erste Ziffer des acht-stelligen Kostenträgers. */
  public int getNummernkreis();

  /** @return Ziffer 2-4 des acht-stelligen Kostenträgers. */
  public int getBereich();

  /** @return Ziffer 5-6 des acht-stelligen Kostenträgers. */
  public int getTeilbereich();

  /** @return Die letzten beiden Ziffern des Kostenträgers als Kost2Art-Objekt. */
  public Kost2Art getKost2Art();

  /** @return Das dem Projekt zugeordnete Projekt, ansonsten null. */
  public Projekt getProjekt();

  public String getDescription();

  public String getComment();

  /** Gibt den Kostenträger als Zeichenkette in der Form 1.234.56.78 zurück. */
  public String getFormattedString();
}
