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
import static org.testng.AssertJUnit.assertTrue;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Set;

import org.projectforge.business.fibu.AuftragDO;
import org.projectforge.business.fibu.AuftragDao;
import org.projectforge.business.fibu.AuftragsPositionDO;
import org.projectforge.business.fibu.RechnungDO;
import org.projectforge.business.fibu.RechnungDao;
import org.projectforge.business.fibu.RechnungsPositionDO;
import org.projectforge.business.fibu.RechnungsPositionVO;
import org.projectforge.framework.time.DayHolder;
import org.projectforge.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

public class RechnungCacheTest extends AbstractTestBase
{
  @Autowired
  private AuftragDao auftragDao;

  @Autowired
  private RechnungDao rechnungDao;

  @Test
  public void baseTest()
  {
    final DayHolder today = new DayHolder();
    logon(getUser(TEST_FINANCE_USER));
    final AuftragDO auftrag = new AuftragDO();
    AuftragsPositionDO auftragsPosition = new AuftragsPositionDO();
    auftragsPosition.setTitel("Pos 1");
    auftrag.addPosition(auftragsPosition);
    auftragsPosition = new AuftragsPositionDO();
    auftragsPosition.setTitel("Pos 2");
    auftrag.addPosition(auftragsPosition);
    auftrag.setNummer(auftragDao.getNextNumber(auftrag));
    auftragDao.save(auftrag);

    final RechnungDO rechnung1 = new RechnungDO();
    RechnungsPositionDO position = new RechnungsPositionDO();
    position.setAuftragsPosition(auftrag.getPosition((short) 1)).setEinzelNetto(new BigDecimal("100")).setText("1.1");
    rechnung1.addPosition(position);
    position = new RechnungsPositionDO();
    position.setAuftragsPosition(auftrag.getPosition((short) 2)).setEinzelNetto(new BigDecimal("200")).setText("1.2");
    rechnung1.addPosition(position);
    rechnung1.setNummer(rechnungDao.getNextNumber(rechnung1)).setDatum(today.getSQLDate());
    rechnungDao.save(rechnung1);

    final RechnungDO rechnung2 = new RechnungDO();
    position = new RechnungsPositionDO();
    position.setAuftragsPosition(auftrag.getPosition((short) 1)).setEinzelNetto(new BigDecimal("400")).setText("2.1");
    rechnung2.addPosition(position);
    rechnung2.setNummer(rechnungDao.getNextNumber(rechnung2)).setDatum(today.getSQLDate());
    rechnungDao.save(rechnung2);

    Set<RechnungsPositionVO> set = rechnungDao.getRechnungCache().getRechnungsPositionVOSetByAuftragId(auftrag.getId());
    assertEquals("3 invoice positions expected.", 3, set.size());
    final Iterator<RechnungsPositionVO> it = set.iterator();
    RechnungsPositionVO posVO = it.next(); // Positions are ordered.
    assertEquals("1.1", posVO.getText());
    posVO = it.next();
    assertEquals("1.2", posVO.getText());
    posVO = it.next();
    assertEquals("2.1", posVO.getText());
    assertTrue(new BigDecimal("700").compareTo(RechnungDao.getNettoSumme(set)) == 0);

    set = rechnungDao.getRechnungCache()
        .getRechnungsPositionVOSetByAuftragsPositionId(auftrag.getPosition((short) 1).getId());
    assertEquals("2 invoice positions expected.", 2, set.size());
    assertTrue(new BigDecimal("500").compareTo(RechnungDao.getNettoSumme(set)) == 0);

    set = rechnungDao.getRechnungCache()
        .getRechnungsPositionVOSetByAuftragsPositionId(auftrag.getPosition((short) 2).getId());
    assertEquals("1 invoice positions expected.", 1, set.size());
    assertTrue(new BigDecimal("200").compareTo(RechnungDao.getNettoSumme(set)) == 0);

    final RechnungDO rechnung = rechnungDao.getById(rechnung2.getId());
    rechnung.getPosition(0).setAuftragsPosition(null);
    rechnungDao.update(rechnung);
    set = rechnungDao.getRechnungCache().getRechnungsPositionVOSetByAuftragId(auftrag.getId());
    assertEquals("2 invoice positions expected.", 2, set.size());
    assertTrue(new BigDecimal("300").compareTo(RechnungDao.getNettoSumme(set)) == 0);
  }

}
