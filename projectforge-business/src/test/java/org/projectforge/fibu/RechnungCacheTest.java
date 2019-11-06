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
import org.projectforge.business.fibu.*;
import org.projectforge.framework.time.DayHolder;
import org.projectforge.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.Iterator;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RechnungCacheTest extends AbstractTestBase {
  @Autowired
  private AuftragDao auftragDao;

  @Autowired
  private RechnungDao rechnungDao;

  @Test
  public void baseTest() {
    final DayHolder today = new DayHolder();
    logon(getUser(AbstractTestBase.TEST_FINANCE_USER));
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
    position.setAuftragsPosition(auftrag.getPosition((short) 1));
    position.setEinzelNetto(new BigDecimal("100"));
    position.setText("1.1");
    rechnung1.addPosition(position);
    position = new RechnungsPositionDO();
    position.setAuftragsPosition(auftrag.getPosition((short) 2));
    position.setEinzelNetto(new BigDecimal("200"));
    position.setText("1.2");
    rechnung1.addPosition(position);
    rechnung1.setNummer(rechnungDao.getNextNumber(rechnung1));
    rechnung1.setDatum(today.getSQLDate());
    rechnung1.setFaelligkeit(new Date(System.currentTimeMillis()));
    rechnung1.setProjekt(initTestDB.addProjekt(null, 1, "foo"));
    rechnungDao.save(rechnung1);

    final RechnungDO rechnung2 = new RechnungDO();
    position = new RechnungsPositionDO();
    position.setAuftragsPosition(auftrag.getPosition((short) 1));
    position.setEinzelNetto(new BigDecimal("400"));
    position.setText("2.1");
    rechnung2.addPosition(position);
    rechnung2.setNummer(rechnungDao.getNextNumber(rechnung2));
    rechnung2.setDatum(today.getSQLDate());
    rechnung2.setFaelligkeit(new Date(System.currentTimeMillis()));
    rechnung2.setProjekt(initTestDB.addProjekt(null, 1, "foo"));
    rechnungDao.save(rechnung2);

    Set<RechnungsPositionVO> set = rechnungDao.getRechnungCache().getRechnungsPositionVOSetByAuftragId(auftrag.getId());
    assertEquals(3, set.size(), "3 invoice positions expected.");
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
    assertEquals( 2, set.size(),"2 invoice positions expected.");
    assertTrue(new BigDecimal("500").compareTo(RechnungDao.getNettoSumme(set)) == 0);

    set = rechnungDao.getRechnungCache()
            .getRechnungsPositionVOSetByAuftragsPositionId(auftrag.getPosition((short) 2).getId());
    assertEquals( 1, set.size(),"1 invoice positions expected.");
    assertTrue(new BigDecimal("200").compareTo(RechnungDao.getNettoSumme(set)) == 0);

    final RechnungDO rechnung = rechnungDao.getById(rechnung2.getId());
    rechnung.getPositionen().get(0).setAuftragsPosition(null);
    rechnungDao.update(rechnung);
    set = rechnungDao.getRechnungCache().getRechnungsPositionVOSetByAuftragId(auftrag.getId());
    assertEquals( 2, set.size(),"2 invoice positions expected.");
    assertTrue(new BigDecimal("300").compareTo(RechnungDao.getNettoSumme(set)) == 0);
  }

}
