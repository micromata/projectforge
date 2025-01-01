/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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
 * Interface for reporting. Repräsentiert den Kostenträger Kost1.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * @see org.projectforge.business.fibu.kost.Kost1DO
 */
public interface Kost1
{
  /** Synthetischer, eindeutiger Datenbankschlüssel. */
  Long getId();

  /** Ist der Kostenträger aktiv? */
  KostentraegerStatus getKostentraegerStatus();

  /** @return Die erste Ziffer des acht-stelligen Kostenträgers. */
  int getNummernkreis();

  /** @return Ziffer 2-4 des acht-stelligen Kostenträgers. */
  int getBereich();

  /** @return Ziffer 5-6 des acht-stelligen Kostenträgers. */
  int getTeilbereich();

  /** @return Die letzten beiden Ziffern des Kostenträgers. */
  int getEndziffer();

  /** @return Freitext. */
  String getDescription();

  /** Gibt den Kostenträger als Zeichenkette in der Form 1.234.56.78 zurück. */
  String getFormattedString();
}
