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

package org.projectforge.fibu;

import static org.testng.AssertJUnit.assertEquals;

import java.math.BigDecimal;

import org.projectforge.business.fibu.RechnungDO;
import org.projectforge.business.fibu.RechnungsPositionDO;
import org.testng.annotations.Test;

public class RechnungDOTest
{
  @Test
  public void nettoBetrag()
  {
    final RechnungDO rechnung = new RechnungDO();
    RechnungsPositionDO pos = new RechnungsPositionDO();
    pos.setEinzelNetto(new BigDecimal(900));
    rechnung.addPosition(pos);
    pos = new RechnungsPositionDO();
    pos.setMenge(new BigDecimal(7)).setEinzelNetto(new BigDecimal(900)).setVat(new BigDecimal("0.19"));
    rechnung.addPosition(pos);
    assertEquals(new BigDecimal("7200.00"), rechnung.getNetSum());
    assertEquals(new BigDecimal("8397.00"), rechnung.getGrossSum());
  }
}
