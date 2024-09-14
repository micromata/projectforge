/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

/**
 * Interface for reporting.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * @see org.projectforge.business.fibu.kost.Kost2ArtDO
 */
public interface Kost2Art
{
  /**
   * Zweistellig (Endziffer 7-8 von Kost2-Kostenträgern).
   * @return
   */
  Long getId();

  /**
   * Name, der auch in Auswahlboxen etc. angezeigt wird.
   * @return
   */
  String getName();

  /**
   * @return Freitext.
   */
  String getDescription();

  /**
   * Sind Buchungssätze zu dieser Kostenart (über Kost2) an den Endkunden fakturiert?
   * @return
   */
  boolean isFakturiert();

  /**
   * @return Zweistelliger id: 00, 01, ..., 99
   */
  String getFormattedId();

  boolean isDeleted();

  /**
   * Used by ProjektListAction.
   * @return
   */
  boolean isSelected();

  /**
   * Used by ProjektListAction: is this kost2art already defined for the project or not?
   */
  boolean isExistsAlready();

  /**
   * Kost2Arten, die als Projektstandard markiert werden, werden in der Projektübersicht und den Projektdetails besonders markiert.
   * @return
   */
  boolean isProjektStandard();
}
