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

import org.projectforge.business.fibu.KundeStatus;

/**
 * Interface for reporting.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * @see org.projectforge.business.fibu.KundeDO
 */
public interface Kunde
{
  /**
   * @return Kundennummer (Ziffer 2-4 von KOST2: 000-999).
   */
  public Integer getId();

  public String getName();

  /**
   * Auch Abteilungen von Kunden haben eine eigene Kundennummer.
   */
  public String getDivision();

  public KundeStatus getStatus();

  /** Freitext. */
  public String getDescription();
}
