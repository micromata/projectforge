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

package org.projectforge.meb;

import static org.junit.jupiter.api.Assertions.*;
import java.io.Serializable;
import java.util.Date;

import org.projectforge.business.meb.MebDao;
import org.projectforge.business.meb.MebEntryDO;
import org.projectforge.business.meb.MebEntryStatus;
import org.projectforge.framework.access.AccessException;
import org.projectforge.test.AbstractTestBase;
import org.projectforge.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;

public class MebDaoTest extends AbstractTestBase
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
    logon(AbstractTestBase.TEST_USER);
    try {
      mebDao.save(entry);
      fail("Exception expected because only administrators can insert entries without an owner.");
    } catch (final AccessException ex) {
      // OK.
    }
    logon(AbstractTestBase.ADMIN);
    mebDao.save(entry); // Allowed for admins
    logon(AbstractTestBase.TEST_USER);
    entry = new MebEntryDO().setDate(new Date()).setSender("1234567890").setStatus(MebEntryStatus.RECENT);
    entry.setOwner(getUser(AbstractTestBase.TEST_USER));
    Serializable id = mebDao.save(entry);
    mebDao.getById(id);
    logon(AbstractTestBase.TEST_USER2);
    try {
      mebDao.getById(id);
      fail("Exception expected because user shouldn't have access to foreign entries.");
    } catch (final AccessException ex) {
      // OK.
    }
    entry = new MebEntryDO().setDate(new Date()).setOwner(getUser(AbstractTestBase.TEST_USER)).setSender("1234567890")
        .setStatus(MebEntryStatus.RECENT);
    try {
      mebDao.save(entry);
      fail("Exception expected because only administrators can insert entries without an owner.");
    } catch (final AccessException ex) {
      // OK.
    }
    logon(AbstractTestBase.TEST_USER);
    mebDao.save(entry);
    logon(AbstractTestBase.ADMIN);
    entry = new MebEntryDO().setDate(new Date()).setSender("1234567890").setStatus(MebEntryStatus.RECENT);
    id = mebDao.save(entry);
    entry = mebDao.getById(id);
    entry.setOwner(getUser(AbstractTestBase.TEST_USER));
    mebDao.update(entry);
    try {
      entry = mebDao.getById(id);
      fail("Exception expected because only owners have select access.");
    } catch (final AccessException ex) {
      // OK.
    }
  }

}
