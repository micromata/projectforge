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

package org.projectforge.reporting.impl;

import org.projectforge.business.fibu.KostFormatter;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.business.fibu.kost.KostentraegerStatus;
import org.projectforge.reporting.Kost2;
import org.projectforge.reporting.Kost2Art;
import org.projectforge.reporting.Projekt;


/**
 * Proxy for Kost2DO;
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * @see Kost2DO
 */
public class Kost2Impl implements Kost2
{
  private Kost2DO kost2;

  private Kost2Art kost2Art;

  private Projekt projekt;

  public Kost2Impl(Kost2DO kost2)
  {
    this.kost2 = kost2;
    this.kost2Art = new Kost2ArtImpl(kost2.getKost2Art());
    this.projekt = new ProjektImpl(kost2.getProjekt());
  }

  public Integer getId()
  {
    return kost2.getId();
  }

  public int getTeilbereich()
  {
    return kost2.getTeilbereich();
  }

  public int getBereich()
  {
    return kost2.getBereich();
  }

  public String getComment()
  {
    return kost2.getComment();
  }

  public String getDescription()
  {
    return kost2.getDescription();
  }

  public Kost2Art getKost2Art()
  {
    return kost2Art;
  }

  public Projekt getProjekt()
  {
    return projekt;
  }

  public KostentraegerStatus getKostentraegerStatus()
  {
    return kost2.getKostentraegerStatus();
  }

  public int getNummernkreis()
  {
    return kost2.getNummernkreis();
  }

  public String getFormattedString()
  {
    return KostFormatter.format(kost2);
  }
}
