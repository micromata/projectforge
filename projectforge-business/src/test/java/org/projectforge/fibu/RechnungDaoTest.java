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
import static org.testng.AssertJUnit.fail;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Date;

import org.projectforge.business.fibu.RechnungDO;
import org.projectforge.business.fibu.RechnungDao;
import org.projectforge.business.fibu.RechnungFilter;
import org.projectforge.business.fibu.RechnungTyp;
import org.projectforge.business.fibu.RechnungsPositionDO;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

public class RechnungDaoTest extends AbstractTestBase
{
  @Autowired
  private RechnungDao rechnungDao;

  private static int dbNumber = RechnungDao.START_NUMBER;

  @Test
  public void getNextNumber()
  {
    logon(TEST_FINANCE_USER);
    final RechnungDO rechnung1 = new RechnungDO();
    int number = rechnungDao.getNextNumber(rechnung1);
    rechnung1.setDatum(new Date(System.currentTimeMillis()));
    rechnung1.setFaelligkeit(new Date(System.currentTimeMillis()));
    rechnung1.setProjekt(initTestDB.addProjekt(null, 1, "foo"));
    try {
      rechnungDao.save(rechnung1);
      fail("Exception with wrong number should be thrown (no number given).");
    } catch (UserException ex) {
    }
    rechnung1.setNummer(number);
    rechnung1.addPosition(createPosition(1, "50.00", "0", "test"));
    Serializable id = rechnungDao.save(rechnung1);
    final RechnungDO rechnung1FromDb = rechnungDao.getById(id);
    assertEquals(dbNumber++, rechnung1FromDb.getNummer().intValue());

    final RechnungDO rechnung2 = new RechnungDO();
    rechnung2.setDatum(new Date(System.currentTimeMillis()));
    rechnung2.setNummer(number);
    try {
      rechnungDao.save(rechnung2);
      fail("Exception with wrong number should be thrown (does already exists).");
    } catch (UserException ex) {
    }
    number = rechnungDao.getNextNumber(rechnung2);
    rechnung2.setNummer(number + 1);
    rechnung2.setFaelligkeit(new Date(System.currentTimeMillis()));
    rechnung2.setProjekt(initTestDB.addProjekt(null, 1, "foo"));
    try {
      rechnungDao.save(rechnung2);
      fail("Exception with wrong number should be thrown (not continuously).");
    } catch (UserException ex) {
    }
    rechnung2.setNummer(number);
    rechnung2.addPosition(createPosition(1, "50.00", "0", "test"));
    id = rechnungDao.save(rechnung2);
    final RechnungDO rechnung2FromDb = rechnungDao.getById(id);
    assertEquals(dbNumber++, rechnung2FromDb.getNummer().intValue());

    final RechnungDO rechnung3 = new RechnungDO();
    rechnung3.setDatum(new Date(System.currentTimeMillis()));
    rechnung3.setTyp(RechnungTyp.GUTSCHRIFTSANZEIGE_DURCH_KUNDEN);
    rechnung3.addPosition(createPosition(1, "50.00", "0", "test"));
    rechnung3.setFaelligkeit(new Date(System.currentTimeMillis()));
    rechnung3.setProjekt(initTestDB.addProjekt(null, 1, "foo"));
    id = rechnungDao.save(rechnung3);
    final RechnungDO rechnung3FromDb = rechnungDao.getById(id);
    assertEquals(null, rechnung3FromDb.getNummer());
    dbNumber++; // Needed for getNextNumber test;
  }

  @Test
  public void checkAccess()
  {
    logon(TEST_FINANCE_USER);
    RechnungDO rechnung = new RechnungDO();
    int number = rechnungDao.getNextNumber(rechnung);
    rechnung.setDatum(new Date(System.currentTimeMillis()));
    rechnung.setFaelligkeit(new Date(System.currentTimeMillis()));
    rechnung.setProjekt(initTestDB.addProjekt(null, 1, "foo"));
    rechnung.setNummer(number);

    rechnung.addPosition(createPosition(2, "100.50", "0.19", "test"));
    rechnung.addPosition(createPosition(1, "50.00", "0", "test"));
    assertEquals("289.19", String.valueOf(rechnung.getGrossSum().setScale(2)));
    Serializable id = rechnungDao.save(rechnung);
    dbNumber++;
    rechnung = rechnungDao.getById(id);

    logon(TEST_CONTROLLING_USER);
    rechnungDao.getById(id);
    checkNoWriteAccess(id, rechnung, "Controlling");

    logon(TEST_USER);
    checkNoAccess(id, rechnung, "Other");

    logon(TEST_PROJECT_MANAGER_USER);
    checkNoAccess(id, rechnung, "Project manager");

    logon(TEST_ADMIN_USER);
    checkNoAccess(id, rechnung, "Admin ");
  }

  private void checkNoAccess(Serializable id, RechnungDO rechnung, String who)
  {
    try {
      RechnungFilter filter = new RechnungFilter();
      rechnungDao.getList(filter);
      fail("AccessException expected: " + who + " users should not have select list access to invoices.");
    } catch (AccessException ex) {
      // OK
    }
    try {
      rechnungDao.getById(id);
      fail("AccessException expected: " + who + " users should not have select access to invoices.");
    } catch (AccessException ex) {
      // OK
    }
    checkNoHistoryAccess(id, rechnung, who);
    checkNoWriteAccess(id, rechnung, who);
  }

  private void checkNoHistoryAccess(Serializable id, RechnungDO rechnung, String who)
  {
    assertEquals(who + " users should not have select access to history of invoices.",
        rechnungDao.hasLoggedInUserHistoryAccess(false), false);
    try {
      rechnungDao.hasLoggedInUserHistoryAccess(true);
      fail("AccessException expected: " + who + " users should not have select access to history of invoices.");
    } catch (AccessException ex) {
      // OK
    }
    assertEquals(who + " users should not have select access to history of invoices.",
        rechnungDao.hasLoggedInUserHistoryAccess(rechnung, false), false);
    try {
      rechnungDao.hasLoggedInUserHistoryAccess(rechnung, true);
      fail("AccessException expected: " + who + " users should not have select access to history of invoices.");
    } catch (AccessException ex) {
      // OK
    }
  }

  private void checkNoWriteAccess(Serializable id, RechnungDO rechnung, String who)
  {
    try {
      RechnungDO re = new RechnungDO();
      int number = rechnungDao.getNextNumber(re);
      re.setDatum(new Date(System.currentTimeMillis()));
      re.setNummer(number);
      rechnungDao.save(re);
      fail("AccessException expected: " + who + " users should not have save access to invoices.");
    } catch (AccessException ex) {
      // OK
    }
    try {
      rechnung.setBemerkung(who);
      rechnungDao.update(rechnung);
      fail("AccessException expected: " + who + " users should not have update access to invoices.");
    } catch (AccessException ex) {
      // OK
    }
  }

  private RechnungsPositionDO createPosition(final int menge, final String einzelNetto, final String vat,
      final String text)
  {
    final RechnungsPositionDO pos = new RechnungsPositionDO();
    pos.setMenge(new BigDecimal(menge));
    pos.setEinzelNetto(new BigDecimal(einzelNetto));
    pos.setVat(new BigDecimal(vat));
    pos.setText(text);
    return pos;
  }

}
