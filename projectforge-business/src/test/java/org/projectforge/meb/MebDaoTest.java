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

package org.projectforge.meb;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

import java.io.Serializable;
import java.util.Date;

import org.projectforge.business.meb.MebDao;
import org.projectforge.business.meb.MebEntryDO;
import org.projectforge.business.meb.MebEntryStatus;
import org.projectforge.framework.access.AccessException;
import org.projectforge.test.AbstractTestNGBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

public class MebDaoTest extends AbstractTestNGBase
{
  @Autowired
  private MebDao mebDao;

  @Test
  public void testGetMail()
  {
    assertEquals("598d4c200461b81522a3328565c25f7c", MebDao.createCheckSum("hallo"));
    assertEquals("598d4c200461b81522a3328565c25f7c", MebDao.createCheckSum(" h#äöüß.,al   lo\n.,-"));
  }

  @Test
  public void testMebDaoAccess()
  {
    MebEntryDO entry = new MebEntryDO().setDate(new Date()).setSender("1234567890").setStatus(MebEntryStatus.RECENT);
    logon(AbstractTestNGBase.TEST_USER);
    try {
      mebDao.save(entry);
      fail("Exception expected because only administrators can insert entries without an owner.");
    } catch (final AccessException ex) {
      // OK.
    }
    logon(AbstractTestNGBase.ADMIN);
    mebDao.save(entry); // Allowed for admins
    logon(AbstractTestNGBase.TEST_USER);
    entry = new MebEntryDO().setDate(new Date()).setSender("1234567890").setStatus(MebEntryStatus.RECENT);
    entry.setOwner(getUser(AbstractTestNGBase.TEST_USER));
    Serializable id = mebDao.save(entry);
    mebDao.getById(id);
    logon(AbstractTestNGBase.TEST_USER2);
    try {
      mebDao.getById(id);
      fail("Exception expected because user shouldn't have access to foreign entries.");
    } catch (final AccessException ex) {
      // OK.
    }
    entry = new MebEntryDO().setDate(new Date()).setOwner(getUser(AbstractTestNGBase.TEST_USER)).setSender("1234567890")
        .setStatus(MebEntryStatus.RECENT);
    try {
      mebDao.save(entry);
      fail("Exception expected because only administrators can insert entries without an owner.");
    } catch (final AccessException ex) {
      // OK.
    }
    logon(AbstractTestNGBase.TEST_USER);
    mebDao.save(entry);
    logon(AbstractTestNGBase.ADMIN);
    entry = new MebEntryDO().setDate(new Date()).setSender("1234567890").setStatus(MebEntryStatus.RECENT);
    id = mebDao.save(entry);
    entry = mebDao.getById(id);
    entry.setOwner(getUser(AbstractTestNGBase.TEST_USER));
    mebDao.update(entry);
    try {
      entry = mebDao.getById(id);
      fail("Exception expected because only owners have select access.");
    } catch (final AccessException ex) {
      // OK.
    }
  }

}
