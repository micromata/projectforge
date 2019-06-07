/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.business.fibu.kost.Kost1DO;
import org.projectforge.business.fibu.kost.KostentraegerStatus;
import org.projectforge.reporting.Kost1;


/**
 * Proxy for Kost1DO;
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * @see Kost1DO
 */
public class Kost1Impl implements Kost1
{
  private Kost1DO kost1;

  public Kost1Impl(Kost1DO kost1)
  {
    this.kost1 = kost1;
  }
  
  public Integer getId()
  {
    return kost1.getId();
  }

  public int getBereich()
  {
    return kost1.getBereich();
  }

  public String getDescription()
  {
    return kost1.getDescription();
  }

  public int getEndziffer()
  {
    return kost1.getEndziffer();
  }

  public KostentraegerStatus getKostentraegerStatus()
  {
    return kost1.getKostentraegerStatus();
  }

  public int getNummernkreis()
  {
    return kost1.getNummernkreis();
  }

  public int getTeilbereich()
  {
    return kost1.getTeilbereich();
  }
  
  public String getFormattedString()
  {
    return KostFormatter.format(kost1);
  }
}
