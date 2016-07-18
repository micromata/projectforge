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

import org.projectforge.business.fibu.KundeDO;
import org.projectforge.business.fibu.KundeDao;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

public class KundeDaoTest extends AbstractTestBase
{
  @Autowired
  private KundeDao kundeDao;

  @Test
  public void checkAccess()
  {
    logon(TEST_FINANCE_USER);
    KundeDO kunde = new KundeDO();
    kunde.setName("ACME");
    kunde.setId(42);
    Serializable id = kundeDao.save(kunde);
    kunde = kundeDao.getById(id);
    kunde.setDescription("Test");
    kundeDao.update(kunde);

    logon(TEST_CONTROLLING_USER);
    kundeDao.getById(id);
    checkNoWriteAccess(id, kunde, "Controlling");

    logon(TEST_USER);
    checkNoAccess(id, kunde, "Other");

    logon(TEST_PROJECT_MANAGER_USER);
    checkNoWriteAccess(id, kunde, "Project manager");
    checkNoHistoryAccess(id, kunde, "Project manager");

    logon(TEST_ADMIN_USER);
    checkNoAccess(id, kunde, "Admin ");
  }

  private void checkNoAccess(Serializable id, KundeDO kunde, String who)
  {
    try {
      BaseSearchFilter filter = new BaseSearchFilter();
      kundeDao.getList(filter);
      fail("AccessException expected: " + who + " users should not have select list access to customers.");
    } catch (AccessException ex) {
      // OK
    }
    try {
      kundeDao.getById(id);
      fail("AccessException expected: " + who + " users should not have select access to customers.");
    } catch (AccessException ex) {
      // OK
    }
    checkNoHistoryAccess(id, kunde, who);
    checkNoWriteAccess(id, kunde, who);
  }

  private void checkNoHistoryAccess(Serializable id, KundeDO kunde, String who)
  {
    assertEquals(who + " users should not have select access to history of customers.",
        kundeDao.hasLoggedInUserHistoryAccess(false), false);
    try {
      kundeDao.hasLoggedInUserHistoryAccess(true);
      fail("AccessException expected: " + who + " users should not have select access to history of customers.");
    } catch (AccessException ex) {
      // OK
    }
    assertEquals(who + " users should not have select access to history of customers.",
        kundeDao.hasLoggedInUserHistoryAccess(kunde, false), false);
    try {
      kundeDao.hasLoggedInUserHistoryAccess(kunde, true);
      fail("AccessException expected: " + who + " users should not have select access to history of invoices.");
    } catch (AccessException ex) {
      // OK
    }
  }

  private void checkNoWriteAccess(Serializable id, KundeDO kunde, String who)
  {
    try {
      KundeDO ku = new KundeDO();
      ku.setId(42);
      kunde.setName("ACME 2");
      kundeDao.save(ku);
      fail("AccessException expected: " + who + " users should not have save access to customers.");
    } catch (AccessException ex) {
      // OK
    }
    try {
      kunde.setDescription(who);
      kundeDao.update(kunde);
      fail("AccessException expected: " + who + " users should not have update access to customers.");
    } catch (AccessException ex) {
      // OK
    }
  }

}
