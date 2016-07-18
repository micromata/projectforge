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
  public Integer getId();

  /**
   * Name, der auch in Auswahlboxen etc. angezeigt wird.
   * @return
   */
  public String getName();

  /**
   * @return Freitext.
   */
  public String getDescription();

  /**
   * Sind Buchungssätze zu dieser Kostenart (über Kost2) an den Endkunden fakturiert?
   * @return
   */
  public boolean isFakturiert();

  /**
   * @return Zweistelliger id: 00, 01, ..., 99
   */
  public String getFormattedId();

  public boolean isDeleted();
  
  /**
   * Used by ProjektListAction.
   * @return
   */
  public boolean isSelected();

  /**
   * Used by ProjektListAction: is this kost2art already defined for the project or not?
   */
  public boolean isExistsAlready();

  /**
   * Kost2Arten, die als Projektstandard markiert werden, werden in der Projektübersicht und den Projektdetails besonders markiert.
   * @return
   */
  public boolean isProjektStandard();
}
