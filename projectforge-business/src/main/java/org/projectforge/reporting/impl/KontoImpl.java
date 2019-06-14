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

import org.projectforge.business.fibu.KontoDO;
import org.projectforge.reporting.Konto;


/**
 * Proxy for KontoDO;
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * @see KontoDO
 */
public class KontoImpl implements Konto
{
  private KontoDO konto;

  public KontoImpl(KontoDO konto)
  {
    this.konto = konto;
  }
  
  public Integer getId()
  {
    return konto.getId();
  }

  public String getBezeichnung()
  {
    return konto.getBezeichnung();
  }

  public String getDescription()
  {
    return konto.getDescription();
  }

  public Integer getNummer()
  {
    return konto.getNummer();
  }
}
