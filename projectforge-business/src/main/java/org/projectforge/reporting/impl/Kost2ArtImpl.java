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

import java.io.Serializable;

import org.projectforge.business.fibu.kost.Kost2ArtDO;
import org.projectforge.common.StringHelper;
import org.projectforge.reporting.Kost2Art;


/**
 * Proxy for Kost2ArtDO;
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * @see Kost2ArtDO
 */
public class Kost2ArtImpl implements Kost2Art, Comparable<Kost2ArtImpl>, Serializable
{
  private static final long serialVersionUID = -1608258416985538412L;

  protected Kost2ArtDO kost2ArtDO;

  protected boolean selected;

  protected boolean existsAlready;

  public Kost2ArtImpl(Kost2ArtDO kost2Art)
  {
    this.kost2ArtDO = kost2Art;
  }

  public Kost2ArtDO getKost2ArtDO()
  {
    return kost2ArtDO;
  }

  public String getFormattedId()
  {
    return StringHelper.format2DigitNumber(kost2ArtDO.getId());
  }

  public boolean isDeleted()
  {
    return kost2ArtDO.isDeleted();
  }
  
  public boolean isSelected()
  {
    return selected;
  }

  public void setSelected(boolean value)
  {
    this.selected = value;
  }

  public boolean isExistsAlready()
  {
    return existsAlready;
  }
  
  public void setExistsAlready(boolean existsAlready)
  {
    this.existsAlready = existsAlready;
  }

  public boolean isProjektStandard()
  {
    return kost2ArtDO.getProjektStandard();
  }

  public String getDescription()
  {
    return kost2ArtDO.getDescription();
  }

  public boolean isFakturiert()
  {
    return kost2ArtDO.getFakturiert();
  }

  public Integer getId()
  {
    return kost2ArtDO.getId();
  }

  public String getName()
  {
    return kost2ArtDO.getName();
  }
  
  public int compareTo(Kost2ArtImpl o)
  {
    return kost2ArtDO.compareTo(o.kost2ArtDO);
  }
}
