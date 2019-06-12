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

package org.projectforge.fibu;

import org.junit.jupiter.api.Test;
import org.projectforge.business.fibu.RechnungDO;
import org.projectforge.business.fibu.RechnungsPositionDO;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RechnungDOTest
{
  @Test
  public void nettoBetrag()
  {
    final RechnungDO rechnung = new RechnungDO();
    RechnungsPositionDO pos = new RechnungsPositionDO();
    pos.setMenge(new BigDecimal(61));
    pos.setEinzelNetto(new BigDecimal(101.25));
    pos.setVat(new BigDecimal("0.19").stripTrailingZeros());
    rechnung.addPosition(pos);
    pos = new RechnungsPositionDO();
    pos.setMenge(new BigDecimal(13));
    pos.setEinzelNetto(new BigDecimal(112.5));
    pos.setVat(new BigDecimal("0.19").stripTrailingZeros());
    rechnung.addPosition(pos);
    pos = new RechnungsPositionDO();
    pos.setMenge(new BigDecimal(79));
    pos.setEinzelNetto(new BigDecimal(112.5));
    pos.setVat(new BigDecimal("0.19").stripTrailingZeros());
    rechnung.addPosition(pos);
    pos = new RechnungsPositionDO();
    pos.setMenge(new BigDecimal(61));
    pos.setEinzelNetto(new BigDecimal(112.5));
    pos.setVat(new BigDecimal("0.19").stripTrailingZeros());
    rechnung.addPosition(pos);
    pos = new RechnungsPositionDO();
    pos.setMenge(new BigDecimal(44));
    pos.setEinzelNetto(new BigDecimal(90));
    pos.setVat(new BigDecimal("0.19").stripTrailingZeros());
    rechnung.addPosition(pos);
    pos = new RechnungsPositionDO();
    pos.setMenge(new BigDecimal(76));
    pos.setEinzelNetto(new BigDecimal(112.5));
    pos.setVat(new BigDecimal("0.19").stripTrailingZeros());
    rechnung.addPosition(pos);
    pos = new RechnungsPositionDO();
    pos.setMenge(new BigDecimal(86));
    pos.setEinzelNetto(new BigDecimal(101.25));
    pos.setVat(new BigDecimal("0.19").stripTrailingZeros());
    rechnung.addPosition(pos);
    pos = new RechnungsPositionDO();
    pos.setMenge(new BigDecimal(88));
    pos.setEinzelNetto(new BigDecimal(112.5));
    pos.setVat(new BigDecimal("0.19").stripTrailingZeros());
    rechnung.addPosition(pos);
    pos = new RechnungsPositionDO();
    pos.setMenge(new BigDecimal(68));
    pos.setEinzelNetto(new BigDecimal(123.75));
    pos.setVat(new BigDecimal("0.19").stripTrailingZeros());
    rechnung.addPosition(pos);
    pos = new RechnungsPositionDO();
    pos.setMenge(new BigDecimal(59));
    pos.setEinzelNetto(new BigDecimal(112.5));
    pos.setVat(new BigDecimal("0.19").stripTrailingZeros());
    rechnung.addPosition(pos);
    pos = new RechnungsPositionDO();
    pos.setMenge(new BigDecimal(8));
    pos.setEinzelNetto(new BigDecimal(101.25));
    pos.setVat(new BigDecimal("0.19").stripTrailingZeros());
    rechnung.addPosition(pos);
    pos = new RechnungsPositionDO();
    pos.setMenge(new BigDecimal(66));
    pos.setEinzelNetto(new BigDecimal(101.25));
    pos.setVat(new BigDecimal("0.19").stripTrailingZeros());
    rechnung.addPosition(pos);
    pos = new RechnungsPositionDO();
    pos.setMenge(new BigDecimal(70));
    pos.setEinzelNetto(new BigDecimal(90));
    pos.setVat(new BigDecimal("0.19").stripTrailingZeros());
    rechnung.addPosition(pos);
    pos = new RechnungsPositionDO();
    pos.setMenge(new BigDecimal(60));
    pos.setEinzelNetto(new BigDecimal(101.25));
    pos.setVat(new BigDecimal("0.19").stripTrailingZeros());
    rechnung.addPosition(pos);

    assertEquals(new BigDecimal("89426.25"), rechnung.getNetSum().setScale(2, BigDecimal.ROUND_HALF_DOWN));
    assertEquals(new BigDecimal("106417.24"), rechnung.getGrossSum().setScale(2, BigDecimal.ROUND_HALF_DOWN));
    assertEquals(new BigDecimal("16990.99"), rechnung.getVatAmountSum().setScale(2, BigDecimal.ROUND_HALF_DOWN));
  }
}
