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

import org.projectforge.business.fibu.KundeDO;
import org.projectforge.business.fibu.KundeStatus;
import org.projectforge.reporting.Kunde;


/**
 * Proxy for KundeDO;
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * @see KundeDO
 */
public class KundeImpl implements Kunde
{
  private KundeDO kunde;

  public KundeImpl(KundeDO kunde)
  {
    this.kunde = kunde;
  }

  public String getDescription()
  {
    return kunde != null ? kunde.getDescription() : "";
  }

  public String getDivision()
  {
    return kunde != null ? kunde.getDivision() : "";
  }

  public Integer getId()
  {
    return kunde != null ? kunde.getId() : null;
  }

  public String getName()
  {
    return kunde != null ? kunde.getName() : "";
  }

  public KundeStatus getStatus()
  {
    return kunde != null ? kunde.getStatus() : null;
  }
}
